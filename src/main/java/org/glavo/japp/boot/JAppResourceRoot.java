package org.glavo.japp.boot;

import java.net.URI;
import java.net.URISyntaxException;

public enum JAppResourceRoot {
    MODULES("/modules"),
    CLASSPATH("/classpath"),
    RESOURCE("/resources");

    private final String pathPrefix;

    JAppResourceRoot(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public URI toURI(JAppResourceGroup group) {
        try {
            return new URI("japp", null, pathPrefix + '/' + group.getName() + '/', null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public URI toURI(JAppResourceGroup group, JAppResource resource) {
        try {
            return new URI("japp", null, pathPrefix + '/' + group.getName() + '/' + resource.getName(), null);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
