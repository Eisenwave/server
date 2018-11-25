package tech.eisen.server;

import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.eisen.server.content.FileAttributes;
import tech.eisen.util.IOUtil;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.attribute.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceCache {
    
    // STATIC
    
    private static final Tika TIKA = new Tika();
    private static final int BLOCK_SIZE = 4096;
    
    private static FileAttributes getAttributes(@NotNull URLConnection connection) throws IOException {
        long lastModified = connection.getLastModified();
        long length = connection.getContentLengthLong();
        
        String name = connection.getURL().getFile();
        String type = TIKA.detect(name);
        if (type.equals("application/octet-stream")) {
            try (InputStream stream = connection.getInputStream()) {
                type = TIKA.detect(stream);
            }
        }
        
        return new CachedBasicFileAttributes(length, lastModified, type);
    }
    
    private static String getFileName(URL url) {
        String[] split = url.getFile().split("/");
        return split[split.length - 1];
    }
    
    // INSTANCE
    
    private final Map<URL, Entry> cache = new ConcurrentHashMap<>();
    
    public boolean has(URL url) {
        return cache.containsKey(url);
    }
    
    public byte[] getAllBytes(@NotNull URL url) throws IOException {
        Entry entry = cache.get(url);
        if (entry != null && entry.data != null)
            return entry.data;
        
        FileAttributes attributes = entry != null? entry.attributes : getAttributes(url.openConnection());
        byte[] data = IOUtils.toByteArray(url.openStream());
        cache.put(url, new Entry(attributes, data));
        
        return data;
    }
    
    public String getAsString(@NotNull URL url, @Nullable Charset charset) throws IOException {
        byte[] bytes = getAllBytes(url);
        return charset == null? new String(bytes) : new String(bytes, charset);
    }
    
    public String getAsString(@NotNull URL url) throws IOException {
        return getAsString(url, null);
    }
    
    public InputStream openStream(@NotNull URL url) throws IOException {
        Entry entry = cache.get(url);
        if (entry != null && entry.data != null)
            return new ByteArrayInputStream(entry.data);
        
        URLConnection connection = url.openConnection();
        
        FileAttributes attributes = entry != null? entry.attributes : getAttributes(connection);
        
        InputStream urlIn = connection.getInputStream();
        PipedOutputStream pipeOut = new PipedOutputStream();
        PipedInputStream pipeIn = new PipedInputStream(pipeOut);
        
        Thread ioThread = new Thread(() -> {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            
            try {
                IOUtil.pipeTwice(urlIn, pipeOut, byteOut, BLOCK_SIZE);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    urlIn.close();
                    pipeOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            cache.put(url, new Entry(attributes, byteOut.toByteArray()));
        });
        
        ioThread.setName("IO-Worker");
        ioThread.setDaemon(true);
        ioThread.start();
        
        return pipeIn;
    }
    
    public Reader openReader(@NotNull URL url) throws IOException {
        return new InputStreamReader(openStream(url));
    }
    
    public FileAttributes getAttributes(@NotNull URL url) throws IOException {
        Entry entry = cache.get(url);
        if (entry != null)
            return entry.attributes;
        
        FileAttributes attributes = getAttributes(url.openConnection());
        cache.put(url, new Entry(attributes, null));
        
        return attributes;
    }
    
    // ACTIONS
    
    public void store(@NotNull URL url, @NotNull String type, long lastModified, byte[] data) {
        FileAttributes attributes = new CachedBasicFileAttributes(data.length, lastModified, type);
        cache.put(url, new Entry(attributes, data));
    }
    
    /**
     * <p>
     * Checks whether a given URL which has been cached has changed since the time of caching.
     * If so, the attributes of the URL will be updated and {@code true} is returned, else {@code false}.
     * </p>
     * <p>
     * Also, if the attributes of the URL have not been cached before, they will be cached.
     * In that case {@code true} is returned.
     * </p>
     *
     * @param url the URL
     * @return whether the entry in the cache has been updated
     * @throws IOException if an I/O error occurs
     */
    public boolean updateAttributes(@NotNull URL url) throws IOException {
        Entry entry = cache.get(url);
        if (entry == null) {
            getAttributes(url);
            return true;
        }
        
        URLConnection connection = url.openConnection();
        
        long oldLastModified = entry.attributes.lastModifiedTime().toMillis();
        long newLastModified = connection.getLastModified();
        
        if (oldLastModified != newLastModified) {
            entry.attributes = getAttributes(connection);
            entry.data = null;
            return true;
        }
        
        return false;
    }
    
    // SUBCLASSES
    
    private static class Entry {
        
        private FileAttributes attributes;
        private byte[] data;
        
        public Entry(@NotNull FileAttributes attributes, @Nullable byte[] data) {
            this.attributes = attributes;
            this.data = data;
        }
        
    }
    
    private static class CachedBasicFileAttributes implements FileAttributes {
        
        private final FileTime lastModified;
        private final long size;
        private final String type;
        
        public CachedBasicFileAttributes(long size, long lastModified, @Nullable String type) {
            this.lastModified = FileTime.fromMillis(lastModified);
            this.size = size;
            this.type = type;
        }
        
        @Nullable
        @Override
        public String getMediaType() {
            return type;
        }
        
        @Override
        public FileTime lastModifiedTime() {
            return lastModified;
        }
        
        @Override
        public FileTime lastAccessTime() {
            return lastModified;
        }
        
        @Override
        public FileTime creationTime() {
            return lastModified;
        }
        
        @Override
        public boolean isRegularFile() {
            return true;
        }
        
        @Override
        public boolean isDirectory() {
            return false;
        }
        
        @Override
        public boolean isSymbolicLink() {
            return false;
        }
        
        @Override
        public boolean isOther() {
            return false;
        }
        
        @Override
        public long size() {
            return size;
        }
        
        @Override
        public Object fileKey() {
            return null;
        }
        
    }
    
}
