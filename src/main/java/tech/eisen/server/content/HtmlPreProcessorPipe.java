package tech.eisen.server.content;

import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import tech.eisen.server.EisenServer;
import tech.eisen.util.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class HtmlPreProcessorPipe implements TextPipe {
    
    public static final char INITIATOR = '$';
    
    private final EisenServer server;
    private final Map<String, String> env = new HashMap<>();
    
    public HtmlPreProcessorPipe(@NotNull EisenServer server, @NotNull Map<String, String> environment) {
        this.server = server;
        environment.forEach((key, val) -> env.put(key.toLowerCase(), val));
    }
    
    @Override
    public void pipeToWriter(Reader input, Writer output) throws IOException {
        boolean started = false;
        StringWriter buffer = null;
        
        for (int c = input.read(); c > 0; c = input.read()) {
            if (!started) {
                if (c == INITIATOR) {
                    started = true;
                    
                    c = input.read();
                    if (c == INITIATOR) {
                        output.write(c);
                        started = false;
                    }
                    else {
                        buffer = new StringWriter();
                        buffer.write(c);
                    }
                }
                
                else output.write(c);
                continue;
            }
            
            if (c == '{') {
                String function = buffer.toString();
                String json = readJSON((char) c, input);
                JsonObject jsonObj = (JsonObject) new JsonParser().parse(json);
                output.write(processFunction(function, jsonObj));
                started = false;
            }
            
            else if (!isIdentifier((char) c)) {
                String variable = buffer.toString();
                String result = processVariable(variable);
                output.write(result);
                started = false;
                buffer = null;
                output.write(c);
            }
            
            else buffer.write(c);
        }
    }
    
    private static boolean isIdentifier(char c) {
        return c == '_' || c == '.'
            || Character.isAlphabetic(c)
            || Character.isDigit(c);
    }
    
    private static String readJSON(char first, Reader input) throws PreProcessException, IOException {
        StringWriter jsonBuffer = new StringWriter();
        jsonBuffer.write(first);
        
        for (int c = input.read(), depth = 0; ; c = input.read()) {
            switch (c) {
                case -1:
                    throw new EOFException("unexpected EOF in JSON portion");
                
                case '{':
                    depth++;
                    jsonBuffer.write(c);
                    break;
                
                case '}':
                    jsonBuffer.write(c);
                    if (depth-- == 0)
                        return jsonBuffer.toString();
                
                default:
                    jsonBuffer.write(c);
            }
        }
    }
    
    private String processFunction(String name, JsonObject json) throws PreProcessException, IOException {
        String result;
        switch (name.toLowerCase()) {
            
            case "embed":
                result = embed(json);
                return pipeBetweenStrings(result);
            
            case "if":
                result = _if(json);
                return pipeBetweenStrings(result);
            
            case "literal":
                result = literal(json);
                return result;
            
            default: return "UNKNOWN_FUNCTION";
        }
    }
    
    private String embed(JsonObject json) throws PreProcessException {
        String src = json.get("src").getAsString();
        
        try (Reader reader = new InputStreamReader(server.getResource(src))) {
            return IOUtils.toString(reader);
        } catch (IOException e) {
            throw new PreProcessException(e);
        }
    }
    
    private String _if(JsonObject json) throws PreProcessException {
        boolean isTrue = true;
        
        if (json.has("defined")) {
            if (!env.containsKey(json.get("defined").getAsString()))
                isTrue = false;
        }
        
        if (isTrue && json.has("true")) {
            if (!json.get("true").getAsBoolean())
                isTrue = false;
        }
        
        if (isTrue && json.has("false")) {
            if (json.get("false").getAsBoolean())
                isTrue = false;
        }
        
        // TODO add some more conditions
        
        if (isTrue)
            return json.get("then").getAsString();
        else if (json.has("else"))
            return json.get("else").getAsString();
        else
            return "";
    }
    
    private String literal(JsonObject json) {
        return json.get("value").toString();
    }
    
    @NotNull
    private String processVariable(String name) {
        String result = env.get(name.toLowerCase());
        return result == null? "NULL" : result;
    }
    
}
