/*
 * Copyright 2023 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        return JAppReader.getSystemReader().openResource(resource);
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
