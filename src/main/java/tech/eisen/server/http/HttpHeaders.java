package tech.eisen.server.http;

import org.jetbrains.annotations.*;

import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

public class HttpHeaders {
    
    private final static Pattern
        LIST_SEPARATOR = Pattern.compile("[ ]*,[ ]*"),
        VALUE_SEPARATOR = Pattern.compile("[ ]*;[ ]*");
    
    private static HeaderValue parse(String name, String rawValue) {
        switch (name) {
            case "accept-encoding":
                return new AcceptEncoding(rawValue);
            case "authorization":
                return new Authorization(rawValue);
            case "user-agent":
                return new RawHeader(rawValue);
            case "content-encoding":
                return new ContentEncoding(rawValue);
            case "content-length":
                return new NumericHeader(rawValue);
            case "content-type":
                return new RawHeader(rawValue);
            case "last-modified":
                return new LastModified(rawValue);
            case "max-redirects":
                return new NumericHeader(rawValue);
            default:
                return new RawHeader(rawValue);
        }
    }
    
    private final Map<String, HeaderValue> entryMap = new HashMap<>();
    
    // MISC METHODS
    
    public void clear() {
        entryMap.clear();
    }
    
    public Set<String> getNames() {
        return Collections.unmodifiableSet(entryMap.keySet());
    }
    
    public boolean hasHeader(String name) {
        return entryMap.containsKey(name.toLowerCase());
    }
    
    // GENERIC GETTERS
    
    public HeaderValue get(@NotNull String field) {
        return entryMap.get(field.toLowerCase());
    }
    
    public String getRawValue(@NotNull String field) {
        HeaderValue value = entryMap.get(field.toLowerCase());
        return value == null? null : value.getRawValue();
    }
    
    // SPECIFIC GETTERS
    
    public AcceptEncoding getAcceptEncoding() {
        return (AcceptEncoding) entryMap.get("accept-encoding");
    }
    
    public Authorization getAuthorization() {
        return (Authorization) entryMap.get("authorization");
    }
    
    public List<String> getContentEncoding() {
        return ((ContentEncoding) entryMap.get("content-encoding")).getEncoding();
    }
    
    public long getContentLength() {
        return ((NumericHeader) entryMap.get("content-length")).longValue();
    }
    
    public String getContentType() {
        return getRawValue("content-type");
    }
    
    public LastModified getLastModified() {
        return (LastModified) entryMap.get("last-modified");
    }
    
    public int getMaxRedirects() {
        return ((NumericHeader) entryMap.get("max-redirects")).intValue();
    }
    
    public String getUserAgent() {
        return getRawValue("user-agent");
    }
    
    // GENERIC SETTERS
    
    public void set(@NotNull String name, HeaderValue value) {
        entryMap.put(name.toLowerCase(), value);
    }
    
    public void set(@NotNull String name, String rawValue) {
        name = name.toLowerCase();
        entryMap.put(name, parse(name, rawValue));
    }
    
    // SPECIFIC SETTERS
    
    public void setAcceptEncoding(AcceptEncoding acceptEncoding) {
        set("accept-encoding", acceptEncoding);
    }
    
    public void setAuthorization(Authorization authorization) {
        set("authorization", authorization);
    }
    
    public void setContentEncoding(String... encoding) {
        set("content-encoding", new ContentEncoding(encoding));
    }
    
    public void setContentLength(long contentLength) {
        set("content-length", Long.toString(contentLength));
    }
    
    public void setContentType(String type, @Nullable Charset charset) {
        set("content-type", charset == null? type : type + "; " + charset.name());
    }
    
    public void setLastModified(long millis) {
        set("last-modified", new LastModified(millis));
    }
    
    public void setMaxRedirects(int redirects) {
        set("max-redirects", new NumericHeader(redirects));
    }
    
    public void setUserAgent(String agentString) {
        set("max-redirects", agentString);
    }
    
    // SUBCLASSES
    
    public static interface HeaderValue {
        
        abstract String getRawValue();
        
    }
    
    public static class AcceptEncoding implements HeaderValue {
        
        private boolean any;
        private final String raw;
        
        private LinkedHashMap<String, Float> map = new LinkedHashMap<>();
        
        @SuppressWarnings("SimplifyStreamApiCallChains")
        public AcceptEncoding(String raw) {
            String[] list = LIST_SEPARATOR.split(raw);
            List<Object[]> entries = new ArrayList<>(list.length);
            
            for (String e : list) {
                String[] encodingAndWeight = VALUE_SEPARATOR.split(e, 2);
                if (encodingAndWeight.length < 2) {
                    entries.add(new Object[] {encodingAndWeight[0], 1f});
                }
                else {
                    String weightStr = encodingAndWeight[1];
                    if (!weightStr.startsWith("q="))
                        throw new IllegalArgumentException("weight must start with \"q=\"");
                    float weight = Float.parseFloat(weightStr.substring(2));
                    
                    if (weight > 0)
                        entries.add(new Object[] {encodingAndWeight[0], weight});
                }
            }
            
            entries.sort((a, b) -> compareWeights((float) a[1], (float) b[1]));
            entries.stream().forEachOrdered(e -> map.put((String) e[0], (Float) e[1]));
            
            this.raw = raw.trim();
            this.any = raw.isEmpty() || entries.size() == 1 && entries.get(0)[0].equals("*");
        }
        
        /**
         * <p>
         * Returns the preferred encoding of the client.
         * This will be the encoding with the highest weight if the client provided weights.
         * If the client did not provide any weights, this will simply be the first element in the list of accepted
         * encodings of the client.
         * </p>
         * <p>
         * <p>
         * If no encoding was provided or any encoding is accepted, the client accepts any encoding provided by the
         * server.
         * In this case the method will return {@code *}
         * </p>
         *
         * @return the preferred encoding or {@code *} if the client accepts any
         */
        public String getPreferredEncoding() {
            return any? "*" : map.keySet().iterator().next();
        }
        
        /**
         * <p>
         * Returns an ordered collection containing the encoding preferences of the client sorted from most preferred
         * (first element) to least preferred (last element).
         * </p>
         * <p>
         * If no encoding was provided or any encoding is accepted, the client accepts any encoding provided by the
         * server.
         * In this case the method will return a collection containing only {@code *}
         * </p>
         *
         * @return the encoding preferences
         */
        public Collection<String> getEncodingPreferences() {
            return any? Collections.singletonList("*") : Collections.unmodifiableCollection(map.keySet());
        }
        
        /**
         * <p>
         * Returns whether the client accepts a given encoding at all or not. This method is case sensitive.
         * </p>
         *
         * @param encoding the encoding
         * @return whether the encoding is accepted by the client
         */
        public boolean acceptsEncoding(String encoding) {
            return any || map.containsKey(encoding);
        }
        
        /**
         * <p>
         * Returns whether the client has specified that they accept any encoding given by the server.
         * </p>
         *
         * @return whether any encoding is accepted
         */
        public boolean acceptsAny() {
            return any;
        }
        
        @Override
        public String getRawValue() {
            return raw;
        }
        
    }
    
    public static class Authorization implements HeaderValue {
        
        private final String raw, method, user;
        private final char[] password;
        
        public Authorization(@NotNull String raw) {
            String[] methodAndCredentials = raw.split("[ ]+", 2);
            this.method = methodAndCredentials[0];
            if (!method.equals("Basic"))
                throw new IllegalArgumentException("Unsupported authorization method: \"" + method + "\"");
            
            String credentials = methodAndCredentials[1];
            credentials = new String(Base64.getDecoder().decode(credentials));
            
            String[] userAndPassword = credentials.split(":", 2);
            this.user = userAndPassword[0];
            this.password = userAndPassword[1].toCharArray();
            this.raw = raw;
        }
        
        public Authorization(String user, char[] password) {
            this.method = "Basic";
            this.user = user;
            this.password = password;
            
            String passwordStr = new String(password);
            String credentials = user + ":" + passwordStr;
            Base64.getEncoder().encode(credentials.getBytes());
            
            this.raw = method + " " + user + " " + passwordStr;
        }
    
        public String getMethod() {
            return method;
        }
    
        public String getUser() {
            return user;
        }
    
        public char[] getPassword() {
            return password;
        }
    
        @Override
        public String getRawValue() {
            return raw;
        }
        
    }
    
    public static class ContentEncoding implements HeaderValue {
        
        private final String raw;
        private final List<String> encoding;
        
        public ContentEncoding(@NotNull String... encodings) {
            this.encoding = Arrays.asList(encodings);
            this.raw = encoding.stream().reduce("", (a, b) -> a + ", " + b);
            
            if (encoding.isEmpty())
                throw new IllegalArgumentException("content-encoding must not be empty");
        }
        
        public ContentEncoding(@NotNull String raw) {
            this.encoding = Arrays.asList(LIST_SEPARATOR.split(raw));
            this.raw = raw;
            
            if (encoding.isEmpty())
                throw new IllegalArgumentException("content-encoding must not be empty");
        }
        
        @NotNull
        public List<String> getEncoding() {
            return encoding;
        }
        
        @Override
        public String getRawValue() {
            return raw;
        }
        
    }
    
    public static class LastModified implements HeaderValue {
    
        private final String value;
        
        public LastModified(String value) {
            this.value = value;
        }
    
        public LastModified(long millis) {
            this.value = HttpUtil.toHttpTime(millis);
        }
        
        @Override
        public String getRawValue() {
            return value;
        }
        
    }
    
    /**
     * Numeric header such as {@code Max-Forwards} or {@code Content-Length}.
     */
    public static class NumericHeader extends Number implements HeaderValue {
        
        private final long value;
        
        public NumericHeader(@NotNull String raw) {
            this.value = Long.parseLong(raw);
        }
        
        public NumericHeader(long value) {
            this.value = value;
        }
        
        @Override
        public int intValue() {
            return (int) value;
        }
        
        @Override
        public long longValue() {
            return value;
        }
        
        @Override
        public float floatValue() {
            return value;
        }
        
        @Override
        public double doubleValue() {
            return value;
        }
        
        @Override
        public String getRawValue() {
            return Long.toString(value);
        }
        
    }
    
    /**
     * A raw header such as {@code User-Agent}
     */
    public static class RawHeader implements HeaderValue {
        
        private final String value;
        
        public RawHeader(@NotNull String value) {
            this.value = value;
        }
        
        @Override
        public String getRawValue() {
            return value;
        }
        
    }
    
    private static int compareWeights(float a, float b) {
        float weightDiff = b - a;
        return weightDiff == 0? 0 : weightDiff > 0? 1 : -1;
    }
    
}
