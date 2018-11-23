package tech.eisen.server;

import org.jetbrains.annotations.NotNull;
import tech.eisen.server.http.DeserializerHttpRequest;
import tech.eisen.server.http.HttpEvent;
import tech.eisen.server.http.HttpPeer;
import tech.eisen.server.http.HttpRequest;

import javax.net.ssl.*;
import java.io.*;

// Thread handling the socket from client
public class ServerThread extends Thread {
    
    private final EisenServer server;
    private final SSLSocket sslSocket;
    
    public ServerThread(@NotNull EisenServer server, @NotNull SSLSocket sslSocket) {
        this.server = server;
        this.sslSocket = sslSocket;
    }
    
    
    
    @Override
    public void run() {
        sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
        
        try {
            SSLSession sslSession = sslSocket.getSession();
            
            // Start handling application content
            InputStream requestStream = sslSocket.getInputStream();
            OutputStream responseStream = sslSocket.getOutputStream();
            
            //requestStream.read();
    
            HttpRequest request;
            try {
                request = new DeserializerHttpRequest().fromStream(requestStream);
            } catch (EOFException ex) {
                return;
            }
            
            HttpPeer peer = new HttpPeer(sslSession.getPeerHost(), sslSession.getPeerPort());
    
            //System.err.println("inb4 handling http on " + Thread.currentThread().getId());
            
            HttpEvent event = new HttpEvent(peer, request, responseStream);
            server.handleEvent(event);
            
            /* Write data
            printWriter.println("HTTPS/1.1 200");
            printWriter.println("content-type: text/plain");
            printWriter.println("content-length: ");
            printWriter.println();
            printWriter.flush();
            */
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                sslSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
}
