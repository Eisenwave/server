package tech.eisen.server.handler;

import tech.eisen.server.EisenServer;
import tech.eisen.server.content.HtmlPreProcessorPipe;
import tech.eisen.server.http.HttpEvent;
import tech.eisen.server.http.HttpException;
import tech.eisen.server.http.HttpHeaders;
import tech.eisen.server.http.HttpStatus;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.*;

public class ErrorHttpHandler implements HttpEventHandler {
    
    private final EisenServer server;
    private final HttpException error;
    
    public ErrorHttpHandler(EisenServer server, HttpException error) {
        this.server = server;
        this.error = error;
    }
    
    @Override
    public void handle(HttpEvent event) throws IOException {
        if (event.hasWrittenHeaders())
            return;
        
        HttpStatus status = error.getStatus();
        
        URL errorPage = getClass().getClassLoader().getResource("html/" + status.getCode() + ".html");
        
        byte[] bytes;
        String type;
        try {
            if (errorPage == null)
                throw new FileNotFoundException();
            // try to load a "fancy" error page if one is available as a resource
            String pageStr = server.getResourceCache().getAsString(errorPage, Charset.defaultCharset());
            
            pageStr = initPreProcessor(error).pipeBetweenStrings(pageStr);
            bytes = pageStr.getBytes();
            type = "text/html";
        } catch (IOException ex) {
            // otherwise display a plaintext error page
            StringWriter writer = new StringWriter();
            writer.write(Integer.toString(status.getCode()));
            writer.append(' ');
            writer.write(status.name());
            writer.write("\r\n");
            
            String message = error.getMessage();
            if (message != null) {
                writer.write("\r\n");
                writer.write(error.getMessage());
                writer.write("\r\n");
            }
            
            if (status == HttpStatus.SERVER_ERROR) {
                writer.write("\r\n");
                Throwable cause = error.getCause();
                writer.write(getStackTraceAsString(cause != null? cause : error));
                writer.write("\r\n");
            }
            
            bytes = writer.toString().getBytes(Charset.defaultCharset());
            type = "text/plain";
        }
        
        event.setStatus(status);
        HttpHeaders resHeaders = event.getResponseHeaders();
        resHeaders.clear();
        resHeaders.setContentType(type, null);
        resHeaders.setContentLength(bytes.length);
        
        if (status == HttpStatus.UNAUTHORIZED)
            resHeaders.set("www-authenticate", "Basic realm=\"Login\"");
        
        if (bytes.length < 1) {
            resHeaders.setContentEncoding("identity");
            event.writeHeaders();
            event.getResponseStream().write(bytes);
        }
        
        else {
            resHeaders.setContentEncoding("gzip");
            event.writeHeaders();
            
            GZIPOutputStream gzipStream = new GZIPOutputStream(event.getResponseStream());
            gzipStream.write(bytes, 0, bytes.length);
            gzipStream.finish();
        }
    }
    
    private static String getStackTraceAsString(Throwable ex) {
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
    
    private HtmlPreProcessorPipe initPreProcessor(HttpException ex) {
        String stackTrace = getStackTraceAsString(ex);
        Map<String, String> env = new HashMap<>();
        env.put("server.port", Integer.toString(server.getPort()));
        env.put("error.status", ex.getStatus().name());
        env.put("error.code", Integer.toString(ex.getStatus().getCode()));
        env.put("error.class", ex.getClass().getSimpleName());
        env.put("error.message", ex.getMessage());
        env.put("error.stacktrace", stackTrace);
        
        return new HtmlPreProcessorPipe(server, env);
    }
    
}
