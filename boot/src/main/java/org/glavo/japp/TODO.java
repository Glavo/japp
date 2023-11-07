package org.glavo.japp;

public final class TODO extends Error {
    public TODO() {
        super("TODO");
    }

    public TODO(String message) {
        super("TODO: " + message);
    }

    public TODO(String message, Throwable cause) {
        super("TODO: " + message, cause);
    }

    public TODO(Throwable cause) {
        super("TODO", cause);
    }
}
