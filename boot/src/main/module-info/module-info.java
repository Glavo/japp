module org.glavo.japp.boot {
    provides java.net.spi.URLStreamHandlerProvider
            with org.glavo.japp.boot.url.JAppURLStreamHandlerProvider;
    provides java.nio.file.spi.FileSystemProvider
            with org.glavo.japp.boot.jappfs.JAppFileSystemProvider;
}