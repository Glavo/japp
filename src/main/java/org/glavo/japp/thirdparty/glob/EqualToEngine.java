package org.glavo.japp.thirdparty.glob;

/**
 * Short cut when the pattern does not contain any wildcard characters.  In this case we can walk the string quickly
 * and not worry about any save points.
 *
 *  @author Joshua Gerth
 */
class EqualToEngine implements MatchingEngine {

    final char[] lowerCase;
    final char[] upperCase;
    final boolean[] matchOne;
    final int length;

    /**
     * Constructor for an EqualsTo Engine Matcher.
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
    protected EqualToEngine(final char[] lowerCase, final char[] upperCase,
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

        // Test if the lengths are the same and shortcut if not
        if (length != string.length()) {
            return false;
        }

        // Walk down the two character arrays and verify the characters are equal.
        int index = 0;
        while (true) {
            // If we reach the end the they must be equal
            if (index == length) {
                return true;
            }

            if (!matchOne[index] && string.charAt(index) != lowerCase[index] && string.charAt(index) != upperCase[index]) {
                return false;
            }
            index++;
        }
    }

    @Override
    public int matchingSizeInBytes() {
        // One integer needed for a running matcher
        return Integer.BYTES + staticSizeInBytes();
    }

    @Override
    public int staticSizeInBytes() {
        // Lower and upper cases are the same size.  Boolean is assumed to be 1 byte for storage.
        return lowerCase.length * (Character.BYTES * 2 + 1) + Integer.BYTES;
    }
}
