package tech.eisen.server.http;

import org.jetbrains.annotations.NotNull;

public class HttpException extends Exception {
    
    private HttpStatus status;
    
    public HttpException(@NotNull HttpStatus status) {
        this.status = status;
    }
    
    public HttpException(@NotNull HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    
    public HttpException(@NotNull HttpStatus status, Throwable cause) {
        super(cause);
        this.status = status;
    }
    
    public HttpException(@NotNull HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
    
    @NotNull
    public HttpStatus getStatus() {
        return status;
    }
    
}
