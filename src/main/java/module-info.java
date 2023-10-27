import org.glavo.japp.boot.url.JAppURLStreamHandlerProvider;

module org.glavo.japp {
    provides java.net.spi.URLStreamHandlerProvider with JAppURLStreamHandlerProvider;
}