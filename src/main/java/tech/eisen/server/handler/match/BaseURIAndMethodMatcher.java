package tech.eisen.server.handler.match;

import org.jetbrains.annotations.NotNull;
import tech.eisen.server.http.HttpRequest;
import tech.eisen.server.http.HttpRequestMethod;

import java.net.URI;
import java.util.function.Predicate;

public class BaseURIAndMethodMatcher implements Predicate<HttpRequest> {
    
    private final String basePath;
    private final HttpRequestMethod method;
    
    public BaseURIAndMethodMatcher(@NotNull URI baseURI, @NotNull HttpRequestMethod method) {
        this.basePath = baseURI.getPath();
        this.method = method;
    }
    
    @Override
    public boolean test(HttpRequest request) {
        return request.getMethod() == method
            && request.getURI().getPath().startsWith(basePath);
    }
    
}
