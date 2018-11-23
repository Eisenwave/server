package tech.eisen.server;

import org.apache.commons.io.IOUtils;
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
    
    private static final int BLOCK_SIZE = 4096;
    
    private final Map<URL, Entry> cache = new ConcurrentHashMap<>();
    
    public byte[] getAllBytes(URL url) throws IOException {
        Entry entry = cache.get(url);
        if (entry != null && entry.data != null)
            return entry.data;
        
        FileAttributes attributes = entry != null? entry.attributes : getAttributes(url.openConnection());
        byte[] data = IOUtils.toByteArray(url.openStream());
        cache.put(url, new Entry(attributes, data));
        
        return data;
    }
    
    public String getAsString(URL url, @Nullable Charset charset) throws IOException {
        byte[] bytes = getAllBytes(url);
        return charset == null? new String(bytes) : new String(bytes, charset);
    }
    
    public String getAsString(URL url) throws IOException {
        return getAsString(url, null);
    }
    
    public InputStream openStream(URL url) throws IOException {
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
    
    public Reader openReader(URL url) throws IOException {
        return new InputStreamReader(openStream(url));
    }
    
    public FileAttributes getAttributes(URL url) throws IOException {
        Entry entry = cache.get(url);
        if (entry != null)
            return entry.attributes;
    
        FileAttributes attributes = getAttributes(url.openConnection());
        cache.put(url, new Entry(attributes, null));
        
        return attributes;
    }
    
    private static FileAttributes getAttributes(URLConnection connection) {
        long lastModified = connection.getLastModified();
        long length = connection.getContentLengthLong();
        String type = connection.getContentType();
        
        return new CachedBasicFileAttributes(length, lastModified, type);
    }
    
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
