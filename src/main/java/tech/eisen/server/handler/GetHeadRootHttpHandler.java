package tech.eisen.server.handler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.eisen.server.EisenServer;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import tech.eisen.server.ResourceCache;
import tech.eisen.server.content.*;
import tech.eisen.server.http.*;
import tech.eisen.server.http.HttpHeaders;

public class GetHeadRootHttpHandler implements HttpEventHandler {
    
    private final static ClassLoader CLASS_LOADER = GetHeadRootHttpHandler.class.getClassLoader();
    
    private final EisenServer server;
    
    public GetHeadRootHttpHandler(@NotNull EisenServer server) {
        this.server = server;
    }
    
    @Override
    public void handle(HttpEvent event) throws IOException, HttpException {
        final URI uri = event.getRequest().getURI();
        final HttpRequestMethod method = event.getRequest().getMethod();
        
        boolean get;
        switch (method) {
            case GET:
                get = true;
                break;
            
            case HEAD:
                get = false;
                break;
            
            default:
                throw new HttpException(HttpStatus.METHOD_NOT_ALLOWED);
        }
        
        URL url = findURL(uri);
        if (url != null) {
            handleGetOrHeadURL(event, get, url, uri);
        }
        else throw new HttpException(HttpStatus.NOT_FOUND, "Resource could not be found");
    }
    
    private void handleGetOrHeadURL(HttpEvent event, boolean get, URL url, URI uri) throws IOException,
        HttpException {
        
        if (uri.getPath().equals("/")) {
            url = getClass().getResource("/html/index.html");
        }
        
        ResourceCache cache = server.getResourceCache();
        //System.out.println("is " + url + " cached = " + cache.has(url));
        
        FileAttributes attributes;
        final boolean newAttributes;
        try {
            newAttributes = cache.updateAttributes(url);
            attributes = cache.getAttributes(url);
        } catch (IOException ex) {
            throw new HttpException(HttpStatus.SERVER_ERROR, "Error while loading attributes", ex);
        }
        
        String contentType = attributes.getMediaType();
        if (contentType == null)
            contentType = "application/octet-stream";
        long lastModified = attributes.lastModifiedTime().toMillis();
        
        event.setStatus(HttpStatus.OK);
        
        HttpHeaders resHeaders = event.getResponseHeaders();
        resHeaders.setContentType(contentType, null);
        resHeaders.setLastModified(lastModified);
        
        if (!get) {
            event.writeHeaders();
            return;
        }
        
        byte[] bytes;
        
        try (InputStream urlStream = cache.openStream(url)) {
            bytes = contentType.equals("text/html")?
                preProcess(urlStream, event, newAttributes? cache : null, url, contentType, lastModified) :
                IOUtils.toByteArray(urlStream);
        } catch (IOException ex) {
            throw new HttpException(HttpStatus.SERVER_ERROR, "Error while loading resource", ex);
        }
        
        EncodingStream<?> stream = getEncodingStream(bytes.length, contentType, event);
        resHeaders.setContentEncoding(stream.getEncoding());
        resHeaders.setContentLength(bytes.length);
        event.writeHeaders();
        
        OutputStream resBodyStream = stream.openStream();
        resBodyStream.write(bytes);
        stream.finish(resBodyStream);
    }
    
    /**
     * Pre-processes data from an input-stream.
     *
     * @param stream the stream which's bytes are to be pre-processed
     * @param event the event
     * @param cache the cache or null if nothing is to be cached
     * @param url the url
     * @param type the media type
     * @param lastModified the last modified-date of the url
     * @return the pre-processed bytes
     * @throws IOException if an I/O error occurs
     */
    private byte[] preProcess(InputStream stream, HttpEvent event,
                              @Nullable ResourceCache cache, URL url, String type, long lastModified)
        throws IOException {
        final Map<String, String> env = new HashMap<>();
        env.put("server.port", Integer.toString(server.getPort()));
        
        byte[] preBytes = null;
        if (cache != null) {
            //System.out.println("initial pp: " + url);
            HtmlPreProcessorPipe htmlPP = new HtmlPreProcessorPipe(server, env, true);
            preBytes = htmlPP.pipeToBytes(new InputStreamReader(stream));
            cache.store(url, type, lastModified, preBytes);
        }
        
        HttpHeaders reqHeaders = event.getRequestHeaders();
        env.put("user.ip", event.getPeer().getHostName());
        env.put("user.port", Integer.toString(event.getPeer().getPort()));
        if (reqHeaders.hasHeader("authorization")) {
            env.put("user.name", reqHeaders.getAuthorization().getUser());
        }
        
        HtmlPreProcessorPipe htmlPP = new HtmlPreProcessorPipe(server, env);
        InputStream stream2 = preBytes != null? new ByteArrayInputStream(preBytes) : stream;
        return htmlPP.pipeToBytes(new InputStreamReader(stream2));
    }
    
    /* @Deprecated
    private void handleGetOrHeadFile(HttpEvent event, boolean get, Path path) throws HttpException, IOException {
        if (!path.startsWith(server.getDirectory().toPath()) || !Files.exists(path))
            throw new HttpException(HttpStatus.NOT_FOUND, "File is hidden or could not be found");
        
        final long size, lastModified;
        final String contentType;
        try {
            if (Files.isHidden(path))
                throw new HttpException(HttpStatus.NOT_FOUND, "File is hidden or could not be found");
            
            size = Files.size(path);
            lastModified = Files.getLastModifiedTime(path).toMillis();
            contentType = Files.probeContentType(path);
        } catch (IOException ex) {
            throw new HttpException(HttpStatus.SERVER_ERROR, "Error while verifying file attributes", ex);
        }
    
        event.setStatus(HttpStatus.OK);
        
        HttpHeaders resHeaders = event.getResponseHeaders();
        
        resHeaders.setContentType(contentType, null);
        resHeaders.setLastModified(lastModified);
        
        if (get) {
            EncodingStream<?> stream = getEncodingStream(size, contentType, event);
            resHeaders.setContentLength(size);
            resHeaders.setContentEncoding(stream.getEncoding());
            event.writeHeaders();
            
            OutputStream resBodyStream = stream.openStream();
            Files.copy(path, resBodyStream);
            stream.finish(resBodyStream);
        }
        else event.writeHeaders();
    } */
    
    private EncodingStream<?> getEncodingStream(long size, String contentType, HttpEvent event) {
        if (size <= 256 || isCompressionException(contentType))
            return new IdentityEncodingStream(event.getResponseStream());
        
        HttpHeaders.AcceptEncoding acceptEncoding = event.getRequestHeaders().getAcceptEncoding();
        if (acceptEncoding == null || acceptEncoding.acceptsEncoding("gzip")) {
            
            return new EncodingStream<GZIPOutputStream>() {
                @Override
                public GZIPOutputStream openStream() throws IOException {
                    return new GZIPOutputStream(event.getResponseStream());
                }
                
                @Override
                public String getEncoding() {
                    return "gzip";
                }
                
                @Override
                public void finish(OutputStream stream) throws IOException {
                    ((GZIPOutputStream) stream).finish();
                }
            };
        }
        
        else return new IdentityEncodingStream(event.getResponseStream());
    }
    
    @Nullable
    private URL findURL(URI uri) {
        String path = uri.getPath();
        if (path.equals("/"))
            return CLASS_LOADER.getResource("html/index.html");
        
        String htmlURI = "html" + path;
        URL url = CLASS_LOADER.getResource(htmlURI);
        if (url == null) {
            url = CLASS_LOADER.getResource(htmlURI + ".html");
        }
        if (url == null) {
            try {
                File file = new File(server.getDirectory(), path.substring(1));
                if (file.isFile())
                    return file.toURI().toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        }
        return url;
    }
    
    private static boolean isCompressionException(String contentType) {
        switch (contentType) {
            case "application/x-compressed":
            case "application/x-bzip2":
            case "application/x-gzip":
            case "multipart/x-gzip":
            case "application/x-tar":
            case "application/x-gtar":
                return true;
            
            case "image/bmp":
            case "audio/wav":
            case "audio/x-wav":
                return false;
        }
        
        return contentType.startsWith("video") || contentType.startsWith("audio") || contentType.startsWith("image");
    }
    
    private static interface EncodingStream<T extends OutputStream> {
        
        abstract T openStream() throws IOException;
        
        abstract String getEncoding();
        
        abstract void finish(OutputStream stream) throws IOException;
        
    }
    
    private static class IdentityEncodingStream implements EncodingStream<OutputStream> {
        
        private final OutputStream stream;
        
        public IdentityEncodingStream(OutputStream stream) {
            this.stream = stream;
        }
        
        @Override
        public OutputStream openStream() {
            return stream;
        }
        
        @Override
        public String getEncoding() {
            return "identity";
        }
        
        @Override
        public void finish(OutputStream stream) {}
        
    }
    
}
