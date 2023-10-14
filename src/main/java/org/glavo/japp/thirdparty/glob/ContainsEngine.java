package org.glavo.japp.thirdparty.glob;

/**
 * This class is used when the wildcard symbols only appear at the front and back of the pattern.
 * This just simplifies our branch save points and allows us to shortcut the search early once we
 * have run to the end (minus one for the wildcard symbol) of the pattern.
 *
 *  @author Joshua Gerth
 */
class ContainsEngine implements MatchingEngine {

    // Used to identify when there is no save point to jump back to
    static final int NO_SAVE_POINT = -1;

    final char[] lowerCase;
    final char[] upperCase;
    final boolean[] matchOne;
    final int length;

    /**
     * Constructor for a Contains Engine Matcher.
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
    protected ContainsEngine(final char[] lowerCase, final char[] upperCase,
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

        // The input string must be at least as long as the glob pattern - 2 (for the first and last wildcard)
        if (string.length() < length - 2) {
            return false;
        }

        int charsIndex = 0;
        int patternIndex = 1;   // Skip the starting wildcard
        int charsSavePoint = NO_SAVE_POINT;

        // The only counter we really care about is the one walking the pattern.  Once the pattern index gets
        //  to the end of its pattern (-1) we are done.  The chars index is just along for the ride.

        // The difficulty in using a for loop here is that the patternIndex and charsIndex can both be reset
        //  which means we would have to reset them at value-1 to offset the for loop incrementing them.

        while (true) {
            if (patternIndex == length - 1) {
                // We got to the end of our pattern (-1 for the final wildcard char)
                return true;
            }

            // Check if we are not at the end of our string and if the character is a matchOne or matches
            if (charsIndex != string.length() &&
                    (matchOne[patternIndex] ||
                            string.charAt(charsIndex) == lowerCase[patternIndex] ||
                            string.charAt(charsIndex) == upperCase[patternIndex])) {

                // At this point the input string and the pattern are matching, but this may be a false
                // match in the string (meaning it starts with the pattern but is not a full pattern)
                // Consider string = "my dog fred my dog spot" and the pattern "%my dog spot%"
                // This pattern is going to *start* matching at charIndex 0, but its a false match since
                // the two will diverge before we get to the end of the pattern.
                // Therefore, if we are not already waking down a branch we need to save a branch point
                // that assumes its a false match.

                if (charsSavePoint == NO_SAVE_POINT) {
                    charsSavePoint = charsIndex + 1;
                }
                // Keep walking forward
                patternIndex++;
                charsIndex++;
            } else if (charsIndex != string.length() && charsSavePoint == NO_SAVE_POINT) {

                // charsSavePoint == NO_SAVE_POINT means we are still pointing at the wildcard and
                //  have not yet matched, so as long as we are not at the end of our string, keep walking
                charsIndex++;
            } else if (charsSavePoint != NO_SAVE_POINT) {

                // Our chars index has reached the end of the input string before our pattern index reached
                //  the end of the pattern. Normally we would be dead here, but we have a save point so
                //  reset the charsIndex to the save point (which has already been moved forward), reset
                //  the pattern index to the start of the pattern and remove our save point.
                charsIndex = charsSavePoint;
                patternIndex = 1;
                charsSavePoint = NO_SAVE_POINT;
            } else {

                // We failed and we have no alternative branch to try
                return false;
            }
        }
    }


    @Override
    public int matchingSizeInBytes() {
        // A running ContainsEngine requires three integers
        return Integer.BYTES * 3 + staticSizeInBytes();
    }


    @Override
    public int staticSizeInBytes() {
        // Lower and upper cases are the same size.  Boolean is assumed to be 1 byte for storage.
        return lowerCase.length * (Character.BYTES * 2 + 1) + Integer.BYTES;
    }
}
