package org.glavo.japp.thirdparty.glob;

/**
 * This is a shortcut for when the user enters '%' (where % is the wildcard symbol) as the pattern. This
 * matches everything except a null input string.
 *
 *  @author Joshua Gerth
 */
class EverythingEngine implements MatchingEngine {

    // Everything engines hold no state so we can create it once and reuse it.
    static final EverythingEngine EVERYTHING_ENGINE = new EverythingEngine();

    /**
     * Empty constructor for an Everything Engine Matcher.  As there is no internal state for this
     * matcher the static {@link #EVERYTHING_ENGINE} should be used instead.
     */
    private EverythingEngine() {
    }

    @Override
    public boolean matches(final String string) {
        return string != null;
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
