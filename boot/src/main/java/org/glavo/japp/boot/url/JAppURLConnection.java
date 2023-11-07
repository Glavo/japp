package org.glavo.japp.boot.url;

import org.glavo.japp.boot.JAppReader;
import org.glavo.japp.boot.JAppResource;
import org.glavo.japp.boot.JAppResourceRoot;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class JAppURLConnection extends URLConnection {

    private JAppResource resource;

    private final JAppResourceRoot root;
    private final String group;
    private final String path;

    JAppURLConnection(URL url) throws MalformedURLException {
        super(url);

        String fullPath = url.getPath();
        if (!fullPath.startsWith("/")) {
            throw invalidURL();
        }

        JAppResourceRoot root = null;
        String group = null;
        String path = null;
        for (JAppResourceRoot r : JAppResourceRoot.values()) {
            String prefix = r.getPathPrefix();
            if (fullPath.startsWith(prefix) && fullPath.length() > prefix.length() && fullPath.charAt(prefix.length()) == '/') {
                int idx = fullPath.indexOf('/', prefix.length() + 1);
                if (idx < 0) {
                    throw invalidURL();
                }

                root = r;
                group = fullPath.substring(prefix.length() + 1, idx);
                path = fullPath.substring(idx + 1);
                break;
            }
        }

        if (root == null) {
            throw invalidURL();
        }

        this.root = root;
        this.group = group;
        this.path = path;
    }

    private MalformedURLException invalidURL() {

        return new MalformedURLException("Invalid URL: " + this.url);
    }

    @Override
    public void connect() throws IOException {
        if (connected) {
            return;
        }

        resource = JAppReader.getSystemReader().findResource(root, group, path);
        if (resource == null) {
            throw new IOException("Resource not found");
        }

        connected = true;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        return JAppReader.getSystemReader().getResourceAsInputStream(resource);
    }

    @Override
    public long getContentLengthLong() {
        try {
            connect();
            return resource.getSize();
        } catch (IOException ignored) {
            return -1L;
        }
    }
}
