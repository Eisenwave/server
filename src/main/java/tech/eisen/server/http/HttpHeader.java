package tech.eisen.server.http;

@Deprecated
public class HttpHeader {
    
    private final String value;
    
    public HttpHeader(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return getValue();
    }
    
}
