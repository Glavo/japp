package org.glavo.japp.boot;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public enum JAppResourceRoot {
    MODULES, CLASSPATH, RESOURCE;

    private final String rootName;
    private final String pathPrefix;

    JAppResourceRoot() {
        this.rootName = name().toLowerCase(Locale.ROOT);
        this.pathPrefix = '/' + rootName;
    }

    public String getRootName() {
        return rootName;
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
