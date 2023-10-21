package org.glavo.japp;

public final class JAppRuntimeContext {
    
    public static JAppRuntimeContext fromCurrentEnvironment() {
        return new JAppRuntimeContext(Integer.getInteger("java.version"));
    }
    
    private final int release;

    public JAppRuntimeContext(int release) {
        this.release = release;
    }

    public int getRelease() {
        return release;
    }
}
