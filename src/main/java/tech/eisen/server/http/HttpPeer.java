package tech.eisen.server.http;

import org.jetbrains.annotations.NotNull;

public class HttpPeer {
    
    private final String hostName;
    private final int port;
    
    public HttpPeer(@NotNull String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }
    
    @NotNull
    public String getHostName() {
        return hostName;
    }
    
    public int getPort() {
        return port;
    }
    
}
