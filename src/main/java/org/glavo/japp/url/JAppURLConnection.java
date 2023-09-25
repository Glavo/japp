package org.glavo.japp.url;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class JAppURLConnection extends URLConnection {

    private static final String MODULES = "/modules";
    private static final String CLASSPATH = "/classpath";

    JAppURLConnection(URL url) throws MalformedURLException {
        super(url);

        String path = url.getPath();
        if (path.isEmpty() || path.charAt(0) != '/') {
            throw new MalformedURLException(url + " missing path or /");
        }

        if (path.length() == 1) {

        } else if (path.startsWith(MODULES)) {


        } else if (path.startsWith(CLASSPATH)) {

        }

    }

    @Override
    public void connect() throws IOException {
        // TODO
    }
}
