package tech.eisen.server.http;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public enum HttpStatus {
    CONTINUE(100),
    SWITCHING_PROTOCOLS(101),
    PROCESSING(102),
    EARLY_HINTS(103),
    
    OK(200),
    CREATED(201),
    ACCEPTED(202),
    NON_AUTHORITATIVE_INFORMATION(203),
    NO_CONTENT(204),
    RESET_CONTENT(205),
    PARTIAL_CONTENT(206),
    MULTI_STATUS(207),
    ALREADY_REPORTED(208),
    
    MULTIPLE_CHOICES(300),
    MOVED_PERMANENTLY(301),
    FOUND(302),
    SEE_OTHER(303),
    NOT_MODIFIED(304),
    
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    PAYMENT_REQUIRED(402),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    
    SERVER_ERROR(500),
    NOT_IMPLEMENTED(501),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503)
    ;
    
    private static final Map<Integer, HttpStatus> byStatusCode = new HashMap<>();
    
    static {
        for (HttpStatus status : values())
            byStatusCode.put(status.getCode(), status);
    }
    
    @Nullable
    public static HttpStatus getByStatusCode(int code) {
        return byStatusCode.get(code);
    }
    
    private final int value;
    
    private HttpStatus(int value) {
        this.value = value;
    }
    
    public int getCode() {
        return value;
    }
    
}
