package org.glavo.japp.thirdparty.glob;

/**
 * This shortcut is for patterns which have the wildcard character only at the very end.  Like
 * 'foobar%' or 'foobar*'
 * <p>
 * In this case we can start at the front and just compare the two strings until one before the end of the
 * pattern (as the last character is the wildcard)
 *
 *  @author Joshua Gerth
 */
class StartsWithEngine implements MatchingEngine {

    final char[] lowerCase;
    final char[] upperCase;
    final boolean[] matchOne;
    final int length;

    /**
     * Constructor for a StartsWith Engine Matcher.
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
    protected StartsWithEngine(final char[] lowerCase, final char[] upperCase,
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

        // The input string must be at least as long as the glob pattern -1 (for the final wildcard)
        if (string.length() < length - 1) {
            return false;
        }

        // Run down through the pattern and compare them.  If we get to the end of our pattern then have matched.
        for (int index = 0; index < length -1; ++index) {
            if (!matchOne[index] && string.charAt(index) != lowerCase[index] && string.charAt(index) != upperCase[index]) {
                return false;
            }
        }
        return true;
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
