package org.glavo.japp.thirdparty.glob;

/**
 * This class is responsible for compiling the given pattern into one of the Matching Engines.
 * <p>
 * As we are expecting to compile a pattern once and execute matches multiple times care has been
 * taken to move as much of the pattern processing to the compile phase.
 * <p>
 * When a pattern is compiled all the case sensitivity is handled, wildcard and match one characters are
 * tagged in separate boolean lookup arrays and multiple wildcards are folded into one.  Also all escape
 * handling is processed so the matching engine has to do as little work as possible.
 * <p>
 * Some compiled pattern examples
 *
 * Example 1:
 *   pattern = "foo%bar"
 * will compile to
 *   lowerCase = "foo%bar"
 *   upperCase = "FOO%BAR"
 *   wildcard  = "0001000"
 *   matchOne  = "0000000"
 *
 * Example 2:
 *   pattern = "foo%%bar"
 * will compile to
 *   lowerCase = "foo%bar"
 *   upperCase = "FOO%BAR"
 *   wildcard  = "0001000"
 *   matchOne  = "0000000"
 *
 * Example 3:
 *   pattern = "foo%%\%%%_\_bar"
 * will compile to
 *   lowerCase = "foo%%%__bar"
 *   upperCase = "FOO%%%__BAR"
 *   wildcard  = "00010100000"
 *   matchOne  = "00000010000"
 *
 *  @author Joshua Gerth
 */
public class GlobPattern {

    /**
     * If the null character is specified as either the wildcard or match-one character then that feature will
     * be disabled.  Turning off both the wildcard and match-one characters is essentially doing a string equality
     * check.
     */
    public static final char NULL_CHARACTER = '\0';

    /**
     * Instructs the matcher to do case-insensitive comparisons.
     */
    public static final int CASE_INSENSITIVE = 0b000001;

    /**
     * Instructs the compiler to handle escape sequences like \% \n and \t.  Without this turned on a \% sequence
     * will be matched as '\' followed by a wildcard.
     */
    public static final int HANDLE_ESCAPES = 0b000010;


    /**
     * Package private constructor as this class contains only static methods.
     */
    GlobPattern() {
    }

    /**
     * Compile the given glob string using '*' for the wildcard and '?' for the match one character.  Case
     * in-sensitivity and escape handling is also turned on.
     * <p>
     * The returned matching engine is thread safe.
     *
     * @param globPattern   The glob pattern to compile
     * @return A compiled matching engine.
     */
    public static MatchingEngine compile(final String globPattern) {
        return compile(globPattern, '*', '?', HANDLE_ESCAPES);
    }

    /**
     * Compile the given glob pattern and return a GlobPattern Matching.
     * <p>
     * The returned matching engine is thread safe.
     *
     * @param globPattern   The glob pattern to compile
     * @param wildcardChar  The character to use for the wildcard, matching zero or more.
     *                      Typically Unix uses '*' while SQL uses '%'.
     *                      To turn this feature off pass in the null '\0' character.
     * @param matchOneChar  The character to use for matching one.  Typically Unix uses '?' while SQL uses '_'.
     *                      To turn this feature off pass in the null '\0' character.
     * @param flags         Match flags, a bit mask that may include {@link #CASE_INSENSITIVE} and {@link #HANDLE_ESCAPES}
     * @return A newly compiled pattern for matching the wildcard pattern.
     * @throws IllegalArgumentException If the globPattern is {@code null}.
     */
    public static MatchingEngine compile(final String globPattern,
                                         final char wildcardChar,
                                         final char matchOneChar,
                                         final int flags) {

        boolean caseInsensitive = has(flags, CASE_INSENSITIVE);
        boolean handleEscapes = has(flags, HANDLE_ESCAPES);

        // Reject null patterns
        if (globPattern == null) {
            throw new IllegalArgumentException("Glob Pattern can not be null");
        }

        // The 'raw' variables hold the pattern before we have processed it.  We just copy it here so we can directly
        //  access the characters and deal with the case sensitivity in one place.  After this we don't have to
        //  worry about case sensitivity again
        int lengthRaw = globPattern.length();
        char[] lowerCaseRaw;
        char[] upperCaseRaw;
        if (caseInsensitive) {
            // Case in-sensitive match so convert the pattern to upper and lower and assign it accordingly
            lowerCaseRaw = globPattern.toLowerCase().toCharArray();
            upperCaseRaw = globPattern.toUpperCase().toCharArray();
        } else {
            // Case sensitive match so just get the backing character array and assign it to the upper and lower
            lowerCaseRaw = upperCaseRaw = globPattern.toCharArray();
        }

        // These will hold our strings after we process the escapes, multiple wildcards and anything else
        char[] lowerCase = new char[lengthRaw];
        char[] upperCase = new char[lengthRaw];

        // Identifies the positions in the character array with wildcard characters
        boolean[] wildcard = new boolean[lengthRaw];

        // Identifies the positions in the character array match one characters
        boolean[] matchOne = new boolean[lengthRaw];

        // Load the boolean arrays with false values ... this may not be necessary in Java
        for (int i = 0; i < lengthRaw; ++i) {
            wildcard[i] = false;
            matchOne[i] = false;
        }

        // Flag to identify if we have processed an escape character.  If handleEscape is false then this
        // will never be set to true
        boolean inEscape = false;

        // Walk through the raw character arrays and copy them into the upperCase and lowerCase arrays.
        //  While we walk through we will collapse multiple globs and handle escaped characters.

        // Count the wildcards for shortcut handling later
        int wildcardCount = 0;

        // Index points to where we are in the lowerCase and upperCase arrays.
        int index = 0;
        for (int i = 0; i < lengthRaw; ++i) {

            // Pull the characters out
            char lowerC = lowerCaseRaw[i];
            char upperC = upperCaseRaw[i];

            // Handle escaping
            if (handleEscapes && lowerC == '\\' && !inEscape) {
                // Mark the flag and eat the escape
                inEscape = true;
                continue;
            }

            // Check if we have hit a wildcard character
            if (wildcardChar != NULL_CHARACTER && lowerC == wildcardChar) {

                // If we are in an escape then ignore the wildcard and let it fall through as a simple character
                if (!inEscape) {
                    // Check if this is a duplicate
                    if (index != 0 && wildcard[index - 1]) {
                        // Yes, they have entered two wildcard patterns in a row so ignore the second wildcard
                        continue;
                    }

                    // We need to mark this location as being a wildcard
                    wildcard[index] = true;
                    ++wildcardCount;
                }
            } else if (matchOneChar != NULL_CHARACTER && lowerC == matchOneChar) {
                // As before, if we are in an escape then let the exactly one fall through as a regular character.
                if (!inEscape) {
                    // Mark the location as being a matchOne
                    matchOne[index] = true;
                }
            } else if (inEscape) {
                switch (lowerC) {
                    case 'r':
                        // Return
                        lowerC = upperC = '\r';
                        break;
                    case 'n':
                        // New Line
                        lowerC = upperC = '\n';
                        break;
                    case 't':
                        // Tab
                        lowerC = upperC = '\t';
                        break;
                    case '\\':
                        // Backslash
                        lowerC = upperC = '\\';
                        break;
                    case 'u':
                        // Unicode
                        if (i + 4 < lengthRaw) {
                            String s = new String(lowerCaseRaw, i+1, 4);

                            // Try to convert it
                            try {
                                char u = (char) Integer.parseInt(s, 16);
                                upperC = Character.toUpperCase(u);
                                lowerC = Character.toLowerCase(u);
                            } catch (NumberFormatException e) {
                                throw new RuntimeException("Bad Unicode character: " + s);
                            }
                            // Move i ahead 4 for the characters we ate
                            i += 4;
                            break;
                        } else {
                            throw new RuntimeException("Bad Unicode char at end of input");
                        }
                    default:
                        throw new RuntimeException("Unknown escape sequence : \\" + lowerC);
                }
            }

            // Copy the character over
            lowerCase[index] = lowerC;
            upperCase[index] = upperC;
            index++;

            // Turn off escaping
            inEscape = false;
        }

        // Index holds our length
        int length = index;

        // At this point we are done compiling the wildcard pattern into the lowerCase and upperCase arrays.
        //  But we can inspect the resulting patterns and make some simple optimizations.

        // If the pattern is empty ... they gave us an empty string so short cut it.
        if (length == 0) {
            // specifically: ''
            return EmptyOnlyEngine.EMPTY_ONLY_ENGINE;
        }

        // If our pattern is just one character long, and its a wildcard then this will match everything
        //  so return the Match Everything Engine.
        if (length == 1 && wildcard[0]) {
            // specifically: '%'
            return EverythingEngine.EVERYTHING_ENGINE;
        }

        // If there are no wildcards then this is a simple equal to engine
        if (wildcardCount == 0) {
            // ex: 'foo'
            return new EqualToEngine(lowerCase, upperCase, matchOne, length);
        }

        // If there is only one wildcard and it is at either the start or end then return
        //  an EndsWith or StartsWith engine accordingly
        if (wildcardCount == 1) {
            if (wildcard[0]) {
                // ex: '%foo'
                return new EndsWithEngine(lowerCase, upperCase, matchOne, length);
            }
            if (wildcard[length-1]) {
                // ex: 'foo%'
                return new StartsWithEngine(lowerCase, upperCase, matchOne, length);
            }
        }

        // If there are two wildcards and they are at the start AND end then this is a contains
        if (wildcardCount == 2 && wildcard[0] && wildcard[length-1]) {
            // ex: '%foo%'
            return new ContainsEngine(lowerCase, upperCase, matchOne, length);
        }

        // No other shortcuts so fall back to the glob engine
        return new GlobEngine(lowerCase, upperCase, wildcard, matchOne, length);
    }

    private static boolean has(int flags, int feature) {
        return (flags & feature) != 0;
    }
}
