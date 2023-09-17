package org.glavo.japp.launcher;


public final class BootLauncher {
    private static final String PROPERTY_PREFIX = "org.glavo.japp.";

    public static void main(String[] args) {
        String jappFile = System.getProperty(PROPERTY_PREFIX + "jappFile");
        String mainClass = System.getProperty(PROPERTY_PREFIX + "mainClass");

        // TODO
    }
}
