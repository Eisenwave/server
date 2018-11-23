package tech.eisen.server.handler;

import org.jetbrains.annotations.NotNull;
import tech.eisen.server.EisenServer;
import tech.eisen.server.content.HtmlPreProcessorPipe;
import tech.eisen.server.http.*;
import tech.eisen.server.http.HttpHeaders;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public abstract class EisenHttpHandler {
    
    protected final EisenServer server;
    
    public EisenHttpHandler(@NotNull EisenServer server) {
        this.server = server;
    }
    
    public EisenServer getServer() {
        return server;
    }
    
    protected abstract void handleEvent(HttpEvent event) throws Exception;
    
    public final void handle(HttpEvent event) throws IOException {
        HttpRequest request = event.getRequest();
        final String path = request.getURI().getPath(),
            remoteHost = event.getPeer().getHostName(),
            method = request.getMethod().toString();
        
        try {
            handleEvent(event);
            String msg = String.format("%s %s \"%s\" %d %s", remoteHost, method, path,
                event.getStatus().getCode(), event.getStatus());
            System.out.println(msg);
        } catch (Exception ex) {
            ex.printStackTrace();
            
            HtmlPreProcessorPipe preProcessor = initPreProcessor(ex);
            byte[] responseBytes = preProcessor.pipeToBytes(server.getResourceText("html/500.html"));
    
            HttpHeaders responseHeaders = event.getResponseHeaders();
            responseHeaders.clear();
            //httpExchange.setAttribute("Content-Type", "");
            responseHeaders.setContentType("text/html", Charset.defaultCharset());
            responseHeaders.setContentLength(responseBytes.length);
            event.setStatus(HttpStatus.SERVER_ERROR);
            event.writeHeaders();
            
            event.getResponseStream().write(responseBytes);
            System.out.println(remoteHost + ' ' + method + ' ' + path + " 500 SERVER ERROR");
        }
    }
    
    private HtmlPreProcessorPipe initPreProcessor(Exception ex) {
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        try {
            writer.close();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        
        Map<String, String> env = new HashMap<>();
        env.put("stacktrace", writer.toString());
    
        return new HtmlPreProcessorPipe(server, env);
    }
    
}
