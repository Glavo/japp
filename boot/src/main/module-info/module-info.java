module org.glavo.japp.boot {
    provides java.net.spi.URLStreamHandlerProvider
            with org.glavo.japp.boot.url.JAppURLStreamHandlerProvider;
}