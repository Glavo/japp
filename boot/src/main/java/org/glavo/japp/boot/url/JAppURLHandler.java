package org.glavo.japp.boot.url;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class JAppURLHandler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new JAppURLConnection(u);
    }
}
