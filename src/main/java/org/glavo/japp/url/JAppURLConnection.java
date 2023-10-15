package org.glavo.japp.url;

import org.glavo.japp.JAppReader;
import org.glavo.japp.JAppResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class JAppURLConnection extends URLConnection {

    private JAppResource resource;

    private final boolean isModulePath;
    private final String itemName;
    private final String path;

    JAppURLConnection(URL url) throws MalformedURLException {
        super(url);

        String fullPath = url.getPath();

        String prefix;
        boolean isModulePath;
        if (fullPath.startsWith(JAppResource.MODULES)) {
            prefix = JAppResource.MODULES;
            isModulePath = true;
        } else if (fullPath.startsWith(JAppResource.CLASSPATH)) {
            prefix = JAppResource.CLASSPATH;
            isModulePath = false;
        } else {
            throw invalidURL();
        }

        int idx = fullPath.indexOf('/', prefix.length() + 1);
        if (idx < 0) {
            throw invalidURL();
        }

        String itemName = fullPath.substring(prefix.length(), idx);
        String path = fullPath.substring(idx + 1);

        if (itemName.isEmpty() || path.isEmpty()) {
            throw invalidURL();
        }

        this.isModulePath = isModulePath;
        this.itemName = itemName;
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

        resource = JAppReader.getSystemReader().findResource(isModulePath, itemName, path);
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
