package tech.eisen.server.http;

import org.jetbrains.annotations.*;

import java.io.*;

public class HttpEvent {
    
    private final static byte[] CRLF = {'\r', '\n'};
    
    private final HttpPeer peer;
    private final HttpRequest request;
    private final OutputStream responseStream;
    private final HttpHeaders headers = new HttpHeaders();
    
    private HttpStatus status;
    
    public HttpEvent(@NotNull HttpPeer peer, @NotNull HttpRequest request, @NotNull OutputStream responseStream) {
        this.peer = peer;
        this.request = request;
        this.responseStream = responseStream;
    }
    
    public void writeHeaders() throws IOException {
        responseStream.write("HTTP/1.1 ".getBytes());
        responseStream.write(Integer.toString(status.getCode()).getBytes());
        responseStream.write(' ');
        responseStream.write(status.toString().getBytes());
        responseStream.write(CRLF);
    
        for (String header : headers.getNames()) {
            String line = header + ": " + headers.getRawValue(header);
            responseStream.write(line.getBytes());
            responseStream.write(CRLF);
        }
    
        responseStream.write(CRLF);
    }
    
    @NotNull
    public HttpPeer getPeer() {
        return peer;
    }
    
    @NotNull
    public HttpRequest getRequest() {
        return request;
    }
    
    @NotNull
    public InputStream getRequestStream() {
        return request.getStream();
    }
    
    @NotNull
    public OutputStream getResponseStream() {
        return responseStream;
    }
    
    public HttpHeaders getRequestHeaders() {
        return request.getHeaders();
    }
    
    public HttpHeaders getResponseHeaders() {
        return headers;
    }
    
    @NotNull
    public HttpStatus getStatus() {
        return status;
    }
    
    public void setStatus(@NotNull HttpStatus status) {
        this.status = status;
    }
    
}
