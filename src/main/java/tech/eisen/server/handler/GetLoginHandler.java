package tech.eisen.server.handler;

import org.jetbrains.annotations.NotNull;
import tech.eisen.server.EisenServer;
import tech.eisen.server.http.HttpEvent;
import tech.eisen.server.http.HttpException;
import tech.eisen.server.http.HttpHeaders;
import tech.eisen.server.http.HttpStatus;
import tech.eisen.server.security.PasswordStore;

import java.io.*;
import java.net.URL;

public class GetLoginHandler implements HttpEventHandler {
    
    private final EisenServer server;
    
    public GetLoginHandler(@NotNull EisenServer server) {
        this.server = server;
    }
    
    /**
     * GET /login
     *
     * @param event the http event
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void handle(HttpEvent event) throws IOException, HttpException {
        HttpHeaders reqHeaders = event.getRequestHeaders();
        HttpHeaders resHeaders = event.getResponseHeaders();
        
        boolean fail = false;
        if (!reqHeaders.hasHeader("authorization"))
            fail = true;
        
        if (!fail) {
            PasswordStore passwords = server.getPasswordStore();
            HttpHeaders.Authorization auth = reqHeaders.getAuthorization();
            String user = auth.getUser().toLowerCase();
            char[] pw = auth.getPassword();
            System.out.println(user + "; " + new String(pw));
            
            if (!passwords.isRegistered(user.toLowerCase()))
                fail = true;
            if (!fail)
                fail = !passwords.matchPassword(user, pw);
        }
        
        if (fail)
            throw new HttpException(HttpStatus.UNAUTHORIZED, "Wrong username or password");
        
        else {
            event.setStatus(HttpStatus.MOVED_PERMANENTLY);
            resHeaders.setLocation("/");
            //resHeaders.setContentLength(1);
            //resHeaders.setContentType("text/plain", null);
            event.writeHeaders();
            //event.getResponseStream().write('\n');
        }
        
        //URL loginURL = getClass().getClassLoader().getResource("html" + uri.getPath());
    }
    
}
