package tech.eisen.server.handler.match;

import org.jetbrains.annotations.NotNull;
import tech.eisen.server.http.HttpRequest;
import tech.eisen.server.http.HttpRequestMethod;

import java.util.function.Predicate;

public class RequestMethodMatcher implements Predicate<HttpRequest> {
    
    private final HttpRequestMethod method;
    
    public RequestMethodMatcher(@NotNull HttpRequestMethod method) {
        this.method = method;
    }
    
    @Override
    public boolean test(HttpRequest request) {
        return request.getMethod() == method;
    }
    
}
