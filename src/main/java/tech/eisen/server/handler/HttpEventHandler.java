package tech.eisen.server.handler;

import tech.eisen.server.http.HttpEvent;
import tech.eisen.server.http.HttpException;

import java.io.IOException;

public interface HttpEventHandler {
    
    public void handle(HttpEvent event) throws IOException, HttpException;
    
}
