package org.glavo.japp.thirdparty.glob;

/**
 * This is for when the pattern being matched against is the empty string ''.  In this case the only thing
 * we can match against is another empty string.
 *
 *  @author Joshua Gerth
 */
class EmptyOnlyEngine implements MatchingEngine {

    // Empty engines hold no state so we can create it once and reuse it.
    static final EmptyOnlyEngine EMPTY_ONLY_ENGINE = new EmptyOnlyEngine();

    /**
     * Empty constructor for an EmptyOnly Engine Matcher.  As there is no internal state for this
     * matcher the static {@link #EMPTY_ONLY_ENGINE} should be used instead.
     */
    private EmptyOnlyEngine() {
    }

    @Override
    public boolean matches(final String string) {

        return string != null && string.isEmpty();
    }

    @Override
    public int matchingSizeInBytes() {
        // Zero running size
        return 0;
    }

    @Override
    public int staticSizeInBytes() {
        // Zero static size
        return 0;
    }
}
