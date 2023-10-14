package org.glavo.japp.thirdparty.glob;

/**
 * Similar to the {@link org.glavo.japp.thirdparty.glob.StartsWithEngine} this one is for the strings where the wildcard only exists at the
 * start of the pattern like '%foobar'.  For this we can start at the end of the pattern and character strings
 * and walk backwards.  If we reach the front of the pattern (minus one for the wildcard) they match.
 *
 *  @author Joshua Gerth
 */
class EndsWithEngine implements MatchingEngine {

    final char[] lowerCase;
    final char[] upperCase;
    final boolean[] matchOne;
    final int length;

    /**
     * Constructor for an EndsWith Engine Matcher.
     *
     * @param lowerCase The version of our pattern which should be used for lower case checking.  (The
     *                  actual contents may not be lower case depending on the case sensitivity flag.)
     * @param upperCase The version of our pattern which should be used for upper case checking.  (The
     *                  actual contents may not be upper case depending on the case sensitivity flag.)
     * @param matchOne  Boolean array which identifies the location of match-one symbols in our pattern.
     * @param length    The length of the lowerCase, upperCase and matchOne arrays.  The built-in
     *                  array lengths can not be used directly as the actual lengths may have
     *                  reduced during compilation.  (Due to escape handling, or wildcard folding.)
     */
    protected EndsWithEngine(final char[] lowerCase, final char[] upperCase,
                             final boolean[] matchOne, final int length) {
        this.lowerCase = lowerCase;
        this.upperCase = upperCase;
        this.matchOne = matchOne;
        this.length = length;
    }


    @Override
    public boolean matches(final String string) {

        // The purest point of view where nothing can match null
        if (string == null) {
            return false;
        }

        // The input string must be at least as long as the glob pattern -1 (for the first wildcard)
        if (string.length() < length - 1) {
            return false;
        }

        int charsIndex = string.length() - 1;

        for (int patternIndex = length - 1; patternIndex > 0; --patternIndex) {
            if (!matchOne[patternIndex] && string.charAt(charsIndex) != lowerCase[patternIndex] &&
                    string.charAt(charsIndex) != upperCase[patternIndex]) {
                return false;
            }
            charsIndex--;
        }
        return true;
    }


    @Override
    public int matchingSizeInBytes() {
        // Two integers are needed for a running matcher
        return Integer.BYTES * 2 + staticSizeInBytes();
    }

    @Override
    public int staticSizeInBytes() {
        // Lower and upper cases are the same size.  Boolean is assumed to be 1 byte for storage.
        return lowerCase.length * (Character.BYTES * 2 + 1) + Integer.BYTES;
    }
}
