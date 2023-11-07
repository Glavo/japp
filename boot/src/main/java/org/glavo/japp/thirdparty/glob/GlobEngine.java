package org.glavo.japp.thirdparty.glob;

import java.util.Arrays;

/**
 * This is the engine of last resort if no shortcuts can be used.  Care has been taken to not use recursion,
 * inner loops and generics ... all of which cause small performance hits and discourage JIT to optimize
 * the bytecode.  External libraries have also been avoided for maximum compatibility.
 * <p>
 * This uses a non-greedy algorithm to pattern match.  This means that every time we hit a wildcard we are
 * going to push a branch option onto our stack where the current character matched the pattern.  However,
 * this is not the path we take at that moment.  Instead we take the other option where the pattern matched
 * nothing and the character index is walked forward.  Thereby having a non-greedy algorithm.
 *
 *  @author Joshua Gerth
 */
class GlobEngine implements MatchingEngine {

    static final int STARTING_STACK_SIZE = 16;

    final char[] lowerCase;
    final char[] upperCase;
    final boolean[] matchOne;
    final boolean[] wildcard;
    final int length;

    /**
     * Constructor for a Contains Engine Matcher.
     *
     * @param lowerCase The version of our pattern which should be used for lower case checking.  (The
     *                  actual contents may not be lower case depending on the case sensitivity flag.)
     * @param upperCase The version of our pattern which should be used for upper case checking.  (The
     *                  actual contents may not be upper case depending on the case sensitivity flag.)
     * @param wildcard  Boolean array which identifies the location of wildcard symbols in our pattern.
     * @param matchOne  Boolean array which identifies the location of match-one symbols in our pattern.
     * @param length    The length of the lowerCase, upperCase and matchOne arrays.  The built-in
     *                  array lengths can not be used directly as the actual lengths may have
     *                  reduced during compilation.  (Due to escape handling, or wildcard folding.)
     */
    protected GlobEngine(final char[] lowerCase, final char[] upperCase,
                         final boolean[] wildcard, final boolean[] matchOne, final int length) {

        this.lowerCase = lowerCase;
        this.upperCase = upperCase;
        this.matchOne = matchOne;
        this.length = length;
        this.wildcard = wildcard;
    }


    @Override
    public boolean matches(final String string) {

        // The purest point of view where nothing can match null
        if (string == null) {
            return false;
        }

        // We use a stack instead of doing recursion.
        //  We could move this stack declaration to a class field and doing so gains us about a 5% speed
        //  improvement.  However, it would also remove the thread safety of this class so for the time
        //  being I'm going to leave it here.
        int[] stack = new int[STARTING_STACK_SIZE];
        int stackIndex = 0;

        // Our two indexes, one to walk down the input string and the other to walk down the pattern
        int charsIndex = 0;
        int patternIndex = 0;

        while (true) {

            // If we are not at the end of our pattern and we have hit a wildcard
            if (patternIndex != length && wildcard[patternIndex]) {

                // If we are not at the end of our input string
                if (charsIndex != string.length()) {
                    // Check our stack is big enough.
                    if (stackIndex == stack.length) {
                        int[] tmp = new int[stack.length * 2];
                        System.arraycopy(stack, 0, tmp, 0, stackIndex);
                        stack = tmp;
                    }
                    // Add a path branch where we try walking the char index forward
                    stack[stackIndex++] = patternIndex;
                    stack[stackIndex++] = charsIndex + 1;
                }

                // Walk the wildcard index forward (non-greedy)
                patternIndex += 1;
            }

            // If we reach the end of our character array and
            //  we are at the end of the wildcard then we are are successful
            if (patternIndex == length && charsIndex == string.length()) {
                // Boom!
                return true;
            }

            // This path fails if we are either at the end of the character string, or the pattern string as we
            //  can not be at the end of both of them at the same time since that would have been caught by the
            //  above conditional.
            // If we are not at the end of either string then we fail if the characters match or if we are on a
            //  matchOne (meaning we match any single character.)
            if (charsIndex == string.length() || patternIndex == length ||
                    (!matchOne[patternIndex] && string.charAt(charsIndex) != lowerCase[patternIndex] && string.charAt(charsIndex) != upperCase[patternIndex])) {

                // This branch has failed to match, so check if we have a different branch we can try
                if (stackIndex != 0) {
                    // Yes, so reset our indexes and loop.
                    charsIndex = stack[--stackIndex];
                    patternIndex = stack[--stackIndex];
                } else {
                    // No, we have exhausted all available branches so this pattern fails to branch.
                    return false;
                }
            } else {
                // We get here if we did not fail in any of those above conditions and we should continue
                //  walking down both strings.
                patternIndex++;
                charsIndex++;
            }
        }
    }


    @Override
    public String toString() {
        return "GlobEngine{\n" +
                "lowerCase = '" + new String(lowerCase) + "',\n" +
                "upperCase = '" + new String(upperCase) + "',\n" +
                "matchOne     = " + Arrays.toString(matchOne) + ",\n" +
                "wildcard      = " + Arrays.toString(wildcard) + ",\n" +
                "length    = " + length + "\n}";
    }


    @Override
    public int matchingSizeInBytes() {
        // A running matcher will require 3 integers for indexes and a stack of integers.  This stack can grow
        // without bound so we will just use the STARTING_STACK_SIZE
        return Integer.BYTES * 3 + Integer.BYTES * STARTING_STACK_SIZE + staticSizeInBytes();
    }


    @Override
    public int staticSizeInBytes() {
        // Lower and upper cases are the same size.  Boolean is assumed to be 1 byte for storage.
        return lowerCase.length * (Character.BYTES * 2 + 2) + Integer.BYTES;
    }
}
