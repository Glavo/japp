package org.glavo.japp.url;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class JAppURLConnection extends URLConnection {

    JAppURLConnection(URL url) throws MalformedURLException {
        super(url);

        String path = url.getPath();
        if (path.isEmpty() || path.charAt(0) != '/') {
            throw new MalformedURLException(url + " missing path or /");
        }
    }

    @Override
    public void connect() throws IOException {
        // TODO
    }
}
