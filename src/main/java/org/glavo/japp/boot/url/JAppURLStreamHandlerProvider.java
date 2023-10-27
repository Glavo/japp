package org.glavo.japp.boot.url;

import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

// For Java 9+
public class JAppURLStreamHandlerProvider extends URLStreamHandlerProvider {
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("japp".equals(protocol)) {
            return new JAppURLHandler();
        }
        return null;
    }
}
