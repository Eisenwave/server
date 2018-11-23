package tech.eisen.server.http;

import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOError;
import java.io.IOException;
import java.net.URLConnection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

public final class HttpUtil {
    
    private HttpUtil() {}
    
    public static String toHttpTime(Instant instant) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT")).format(instant);
    }
    
    public static String toHttpTime(long millis) {
        return toHttpTime(Instant.ofEpochMilli(millis));
    }
    
    public static String guessContentType(String fname, byte[] bytes) {
        System.out.println(fname);
        String[] ext = fname.split("\\.");
        String type = URLConnection.guessContentTypeFromName(ext[ext.length - 1]);
        if (type != null)
            return type;
        try {
            return URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
    
    public static String guessContentType(byte[] bytes) {
        try {
            return URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
    
    @Nullable
    public static String statusName(int status) {
        switch (status) {
            case 100: return "Continue";
            case 101: return "Switching Protocols";
            case 102: return "Processing";
            case 103: return "Early Hints";
            
            case 200: return "OK";
            case 201: return "Created";
            case 202: return "Accepted";
            case 203: return "Non-Authoritative Information";
            case 204: return "No Content";
            case 205: return "Reset Content";
            case 206: return "Partial Content";
            case 207: return "Multi-Status";
            case 208: return "Already Reported";
            case 226: return "IM Used";
            
            case 300: return "Multiple Choices";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 303: return "See Other";
            case 304: return "Not Modified";
            
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 402: return "Payment Required";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            
            default: return null;
        }
    }
    
}
