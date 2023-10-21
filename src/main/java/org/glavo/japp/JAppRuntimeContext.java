package org.glavo.japp;

public final class JAppRuntimeContext {
    
    public static JAppRuntimeContext fromCurrentEnvironment() {
        @SuppressWarnings("deprecation")
        int release = Runtime.version().major();
        return new JAppRuntimeContext(release);
    }
    
    private final int release;

    public JAppRuntimeContext(int release) {
        this.release = release;
    }

    public int getRelease() {
        return release;
    }
}
