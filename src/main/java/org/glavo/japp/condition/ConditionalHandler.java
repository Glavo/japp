package org.glavo.japp.condition;

public final class ConditionalHandler {
    
    public static ConditionalHandler fromCurrentEnvironment() {
        @SuppressWarnings("deprecation")
        int release = Runtime.version().major();
        return new ConditionalHandler(release);
    }
    
    private final int release;

    public ConditionalHandler(int release) {
        this.release = release;
    }

    public int getRelease() {
        return release;
    }
}
