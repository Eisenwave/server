package tech.eisen.server.handler;

import tech.eisen.server.EisenServer;
import tech.eisen.server.http.HttpEvent;
import tech.eisen.server.http.HttpHeaders;
import tech.eisen.server.http.HttpStatus;
import tech.eisen.server.security.PasswordStore;

import java.io.*;

public class LoginHandler extends EisenHttpHandler {
    
    public LoginHandler(EisenServer server) {
        super(server);
    }
    
    /**
     * GET /login
     *
     * @param event the http event
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void handleEvent(HttpEvent event) throws IOException {
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
        
        if (fail) {
            event.setStatus(HttpStatus.UNAUTHORIZED);
            resHeaders.set("www-authenticate", "Basic realm=\"Login\"");
            resHeaders.setContentLength(1);
            resHeaders.setContentType("text/plain", null);
            event.writeHeaders();
            event.getResponseStream().write('\n');
        }
        
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
    
    /*
    private void handleGetResource(HttpEvent event, boolean get, URL resourceURL, URI uri) throws IOException {
        if (uri.getPath().equals("/")) {
            resourceURL = getClass().getResource("/html/index.html");
        }
        
        ResourceCache cache = server.getResourceCache();
        FileAttributes attributes = cache.getAttributes(resourceURL);
        
        String contentType = attributes.getMediaType();
        if (contentType == null)
            contentType = "application/octet-stream";
        long lastModified = attributes.lastModifiedTime().toMillis();
        
        InputStream urlStream = server.getResource(resourceURL);
        byte[] bytes;
        
        if (contentType.equals("text/html"))
            bytes = htmlPreprocessor.pipeToBytes(new InputStreamReader(urlStream));
        else
            bytes = IOUtils.toByteArray(urlStream);
        
        HttpHeaders responseHeaders = event.getResponseHeaders();
        RootHttpHandler.EncodingStream<?> stream = getEncodingStream(contentType, event);
    
        event.setStatus(HttpStatus.OK);
        //responseHeaders.set("www-authenticate", "Basic realm=\"\"");
        responseHeaders.setContentEncoding(stream.getEncoding());
        responseHeaders.setContentLength(bytes.length);
        responseHeaders.setContentType(contentType, null);
        responseHeaders.setLastModified(lastModified);
        event.writeHeaders();
    
        OutputStream resBodyStream = stream.openStream();
        resBodyStream.write(bytes);
        stream.finish(resBodyStream);
    }
    */
    
}
