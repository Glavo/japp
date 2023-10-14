package org.glavo.japp.thirdparty.glob;

/**
 * Interface to a matcher which is responsible for matching the compiled glob pattern against the given input string.
 *
 * @author Joshua Gerth
 */
public interface MatchingEngine {


    /**
     * Ask if the compiled glob pattern matches the given string.
     *
     * @param string The input string we are checking.
     * @return {@code true} if the string patches the glob pattern, {@code false} otherwise.
     */
    boolean matches(String string);


    /**
     * Estimate the size of running the matches method in bytes.  This size will include the static size of
     * the matcher from {@link #staticSizeInBytes()}.
     *
     * @return Estimated memory usage in bytes.
     */
    int matchingSizeInBytes();


    /**
     * Get the static size of the matcher in bytes.
     *
     * @return Static size of the matcher in bytes.
     */
    int staticSizeInBytes();
}
