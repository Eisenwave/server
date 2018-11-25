package tech.eisen.server.http;

import org.jetbrains.annotations.*;

import java.io.*;

public class HttpEvent {
    
    private final static byte[] HTTP_1_1_BYTES = "HTTP/1.1 ".getBytes();
    
    private final static byte[] CRLF = {'\r', '\n'};
    
    private final HttpPeer peer;
    private final HttpRequest request;
    private final OutputStream responseStream;
    private final HttpHeaders headers = new HttpHeaders();
    
    private boolean writtenHeaders = false;
    private HttpStatus status;
    
    public HttpEvent(@NotNull HttpPeer peer, @NotNull HttpRequest request, @NotNull OutputStream responseStream) {
        this.peer = peer;
        this.request = request;
        this.responseStream = responseStream;
    }
    
    /**
     * Writes the HTTP version, status code and headers to the response stream.
     *
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if the status has not been set yet
     *
     * @see #setStatus(HttpStatus)
     * @see #hasWrittenHeaders()
     */
    public void writeHeaders() throws IOException, IllegalStateException {
        if (status == null)
            throw new IllegalStateException("Can't write headers before status has been set");
        
        responseStream.write(HTTP_1_1_BYTES);
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
        this.writtenHeaders = true;
    }
    
    /**
     * Returns whether the headers have already been written once during this event. This method can be used for error
     * handling through the same response stream as the event uses.
     *
     * @return whether {@link #writeHeaders()} has been called before
     */
    public boolean hasWrittenHeaders() {
        return writtenHeaders;
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
