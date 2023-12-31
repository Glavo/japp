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

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.*;

// For Java 8
public class JAppURLStreamHandlerFactory implements URLStreamHandlerFactory {

    private static final String DEFAULT_PREFIX = "sun.net.www.protocol";

    public static void setup() {
        URL.setURLStreamHandlerFactory(new JAppURLStreamHandlerFactory(System.getProperty("java.protocol.handler.pkgs")));
    }

    private final List<String> packagePrefixes;
    private final Map<String, URLStreamHandler> handles = new HashMap<>();

    public JAppURLStreamHandlerFactory(String packagePrefixList) {
        handles.put("japp", new JAppURLHandler());

        if (packagePrefixList == null) {
            this.packagePrefixes = Collections.singletonList(DEFAULT_PREFIX);
        } else {
            Set<String> prefixes = new LinkedHashSet<>();

            StringTokenizer packagePrefixIter =
                    new StringTokenizer(packagePrefixList, "|");

            while (packagePrefixIter.hasMoreTokens()) {
                prefixes.add(packagePrefixIter.nextToken().trim());
            }

            prefixes.add(DEFAULT_PREFIX);

            this.packagePrefixes = Arrays.asList(prefixes.toArray(new String[0]));
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        // Avoid using reflection during bootstrap

        URLStreamHandler handler = handles.get(protocol);
        if (handler != null) {
            return handler;
        }

        synchronized (this) {
            handler = handles.get(protocol);
            if (handler != null) {
                return handler;
            }


            for (String prefix : packagePrefixes) {
                String name = prefix + protocol + ".Handler";
                try {
                    Class<?> cls = Class.forName(name);
                    handler = (URLStreamHandler) cls.newInstance();
                    break;
                } catch (Exception ignored) {
                }
            }

            if (handler != null) {
                handles.put(protocol, handler);
            }
        }

        return handler;
    }
}
