package tech.eisen.server.http;

import eisenwave.torrens.error.FileSyntaxException;
import eisenwave.torrens.error.FileVersionException;
import eisenwave.torrens.io.Deserializer;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class DeserializerHttpRequest implements Deserializer<HttpRequest> {
    
    @NotNull
    @Override
    public HttpRequest fromStream(InputStream stream) throws IOException {
        String firstLine = readLine(stream);
        //if (firstLine.isEmpty())
        //    throw new FileSyntaxException("HTTP request starts with empty line");
        while (firstLine.isEmpty()) {
            System.err.println("empty line: '" + firstLine + "'");
            firstLine = readLine(stream);
        }
        
        String[] methodURIVersion = firstLine.split("[ ]+", 3);
        HttpRequestMethod method = HttpRequestMethod.valueOf(methodURIVersion[0].toUpperCase());
        URI uri;
        try {
            uri = new URI(methodURIVersion[1]);
        } catch (URISyntaxException e) {
            throw new FileSyntaxException(e);
        }
        String version = methodURIVersion[2].split("/", 2)[1];
        
        if (!version.equals("1.1") && !version.equals("1.0")) {
            throw new FileVersionException("unknown HTTP version: '" + version + "'");
        }
        
        HttpRequest result = new HttpRequest(version, method, uri, stream);
        HttpHeaders headers = result.getHeaders();
        
        for (String line = readLine(stream).trim(); !line.isEmpty(); line = readLine(stream).trim()) {
            String[] nameValue = line.split("[ ]*:[ ]*", 2);
            headers.set(nameValue[0], nameValue[1]);
        }
        
        return result;
    }
    
    public static String readLine(InputStream stream) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        
        boolean carriage = false;
        for (int c = stream.read(); ; c = stream.read()) {
            if (c < 0)
                throw new EOFException("Couldn't read line due to unexpected EOF");
            else if (c == '\n')
                return byteStream.toString("ASCII");
            else if (carriage)
                throw new IOException("Carriage return now followed by line break");
            else if (c == '\r')
                carriage = true;
            else
                byteStream.write(c);
        }
    }
    
}
