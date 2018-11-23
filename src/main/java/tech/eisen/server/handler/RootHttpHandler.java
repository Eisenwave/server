package tech.eisen.server.handler;

import org.jetbrains.annotations.NotNull;
import tech.eisen.server.EisenServer;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import tech.eisen.server.ResourceCache;
import tech.eisen.server.content.FileAttributes;
import tech.eisen.server.content.HtmlPreProcessorPipe;
import tech.eisen.server.http.*;
import tech.eisen.server.http.HttpHeaders;

public class RootHttpHandler extends EisenHttpHandler {
    
    public RootHttpHandler(@NotNull EisenServer server) {
        super(server);
    }
    
    @Override
    public void handleEvent(HttpEvent event) throws IOException {
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
                event.setStatus(HttpStatus.METHOD_NOT_ALLOWED);
                event.writeHeaders();
                return;
        }
        
        URL resourceURL = getClass().getClassLoader().getResource("html" + uri.getPath());
        if (resourceURL != null) {
            handleGetOrHeadResource(event, get, resourceURL, uri);
        }
        else {
            Path path = new File(server.getDirectory(), uri.getPath().substring(1)).toPath().toAbsolutePath();
            handleGetOrHeadFile(event, get, path);
            //System.out.println(path);
            //Path path = file.toPath();
        }
    }
    
    private void handleGetOrHeadResource(HttpEvent event, boolean get, URL resourceURL, URI uri) throws IOException {
        if (uri.getPath().equals("/")) {
            resourceURL = getClass().getResource("/html/index.html");
        }
    
        Map<String, String> env = new HashMap<>();
        env.put("port", Integer.toString(server.getPort()));
        
        HttpHeaders reqheaders = event.getRequestHeaders();
        env.put("user.ip", event.getPeer().getHostName());
        env.put("user.port", Integer.toString(event.getPeer().getPort()));
        if (reqheaders.hasHeader("authorization")) {
            env.put("user.name", reqheaders.getAuthorization().getUser());
        }
    
        HtmlPreProcessorPipe htmlPreprocessor = new HtmlPreProcessorPipe(server, env);
    
        ResourceCache cache = server.getResourceCache();
        FileAttributes attributes = cache.getAttributes(resourceURL);
        
        String contentType = attributes.getMediaType();
        if (contentType == null)
            contentType = "application/octet-stream";
        long lastModified = attributes.lastModifiedTime().toMillis();
        
        InputStream urlStream = server.getResource(resourceURL);
        byte[] bytes;
        
        if (contentType.equals("text/html"))
            bytes = htmlPreprocessor.pipeToBytes(new InputStreamReader(urlStream));
        else
            bytes = IOUtils.toByteArray(urlStream);
    
        HttpHeaders responseHeaders = event.getResponseHeaders();
        
        if (get) {
            EncodingStream<?> stream = getEncodingStream(contentType, event);
            
            event.setStatus(HttpStatus.OK);
            //responseHeaders.set("www-authenticate", "Basic realm=\"\"");
            responseHeaders.setContentEncoding(stream.getEncoding());
            responseHeaders.setContentLength(bytes.length);
            responseHeaders.setContentType(contentType, null);
            responseHeaders.setLastModified(lastModified);
            event.writeHeaders();
            
            OutputStream resBodyStream = stream.openStream();
            resBodyStream.write(bytes);
            stream.finish(resBodyStream);
        }
        else {
            event.setStatus(HttpStatus.OK);
            event.writeHeaders();
        }
        
    }
    
    private void handleGetOrHeadFile(HttpEvent event, boolean get, Path path) throws IOException {
        if (Files.isHidden(path)) {
            event.setStatus(HttpStatus.NOT_FOUND);
            return;
        }
        
        if (!path.startsWith(server.getDirectory().toPath())) {
            event.setStatus(HttpStatus.NOT_FOUND);
            return;
        }
        
        if (!Files.exists(path)) {
            event.setStatus(HttpStatus.NOT_FOUND);
            return;
        }
        
        HttpHeaders responseHeaders = event.getResponseHeaders();
        String contentType = Files.probeContentType(path);
        //responseHeaders.add("Content-Length", Integer.toString(response.length));
        
        if (get) {
            EncodingStream<?> stream = getEncodingStream(contentType, event);
    
            event.setStatus(HttpStatus.OK);
            responseHeaders.setContentLength(Files.size(path));
            responseHeaders.setContentEncoding(stream.getEncoding());
            responseHeaders.setContentType(contentType, null);
            event.writeHeaders();
            
            OutputStream resBodyStream = stream.openStream();
            Files.copy(path, resBodyStream);
            stream.finish(resBodyStream);
        }
        else {
            event.setStatus(HttpStatus.OK);
            event.writeHeaders();
        }
    }
    
    private EncodingStream<?> getEncodingStream(String contentType, HttpEvent event) {
        if (isCompressionException(contentType))
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
