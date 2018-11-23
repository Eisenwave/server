package tech.eisen.server.http;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URI;

public class HttpRequest {
    
    private final String version;
    private final HttpRequestMethod method;
    private final URI uri;
    private final HttpHeaders headers = new HttpHeaders();
    private final InputStream stream;
    
    public HttpRequest(String version, HttpRequestMethod method, URI uri, InputStream stream) {
        this.version = version;
        this.method = method;
        this.uri = uri;
        this.stream = stream;
    }
    
    @NotNull
    public HttpHeaders getHeaders() {
        return headers;
    }
    
    @NotNull
    public HttpRequestMethod getMethod() {
        return method;
    }
    
    @NotNull
    public String getVersion() {
        return version;
    }
    
    @NotNull
    public URI getURI() {
        return uri;
    }
    
    @NotNull
    public InputStream getStream() {
        return stream;
    }
    
}
