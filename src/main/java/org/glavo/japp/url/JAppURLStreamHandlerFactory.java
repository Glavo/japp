package org.glavo.japp.url;

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

                    // TODO: Lookup in JAppClassLoader

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
