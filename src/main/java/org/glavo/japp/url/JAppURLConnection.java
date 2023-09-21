package org.glavo.japp.url;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class JAppURLConnection extends URLConnection {

    protected JAppURLConnection(URL url) {
        super(url);
    }

    @Override
    public void connect() throws IOException {
        // TODO
    }
}
