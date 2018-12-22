package tech.eisen.server.content;

import com.github.rjeschke.txtmark.*;
import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.eisen.server.EisenServer;
import tech.eisen.util.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class HtmlPreProcessorPipe implements TextPipe {
    
    public static final char INITIATOR = '$';
    
    /* private final static Configuration MARKDOWN_CONFIGURATION = Configuration.builder()
        .enablePanicMode()
        .enableSafeMode()
        .build(); */
    
    private final static Configuration MD_CONFIGURATION = Configuration.builder()
        .enableSafeMode()
        .forceExtentedProfile()
        .setDecorator(new EisenMarkdownDecorator())
        .setAllowSpacesInFencedCodeBlockDelimiters(false)
        .build();
    
    private final EisenServer server;
    private final Map<String, String> env = new HashMap<>();
    private final boolean constantMode;
    
    public HtmlPreProcessorPipe(@NotNull EisenServer server, @NotNull Map<String, String> environment) {
        this.server = server;
        environment.forEach((key, val) -> env.put(key.toLowerCase(), val));
        this.constantMode = false;
    }
    
    public HtmlPreProcessorPipe(@NotNull EisenServer server,
                                @NotNull Map<String, String> environment,
                                boolean constantMode) {
        this.server = server;
        environment.forEach((key, val) -> env.put(key.toLowerCase(), val));
        this.constantMode = constantMode;
    }
    
    /**
     * Returns if the processor is in constant mode. Only variables and function calls preceded with the {@code const:}
     * modifier will accessed or invoked.
     *
     * @return whether the processor is in constant mode
     */
    public boolean isConstantMode() {
        return constantMode;
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
    
                boolean constant = function.startsWith("const:");
                if (constant)
                    function = function.substring(6);
    
                if (!constantMode || constant) {
                    JsonObject jsonObj = (JsonObject) new JsonParser().parse(json);
                    output.write(processFunction(function, jsonObj));
                }
                else {
                    output.write(INITIATOR);
                    output.write(function);
                    output.write(json);
                }
    
                started = false;
            }
            
            else if (!isIdentifier((char) c)) {
                String variable = buffer.toString();
    
                boolean constant = variable.startsWith("const:");
                if (constant)
                    variable = variable.substring(6);
    
                if (!constantMode || constant) {
                    String result = env.get(variable.toLowerCase());
                    output.write(result == null? INITIATOR + variable : result);
                }
                else {
                    output.write(INITIATOR);
                    output.write(variable);
                }
    
                output.write(c);
                started = false;
            }
            
            else buffer.write(c);
        }
    }
    
    private static boolean isIdentifier(char c) {
        return c == '_' || c == '.' || c == ':'
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
            
            // $def{"<var1>": "<value1>", "<var2>": "<value2>", ..., "<varN>", "<valueN>"}
            case "def": {
                json.entrySet().forEach((entry) -> env.put(entry.getKey(), entry.getValue().getAsString()));
                return "";
            }
            
            // $embed{"src": "<resource_path>", "[type]": "<media_type_or_extension>"}
            case "embed":
                result = embed(json);
                return pipeBetweenStrings(result);
                
            // $if{
            //     "<condition>": "<parameter>",
            //     "then": "<value_if_all_conditions_met>",
            //     "else": "<value_if_not_all_conditions_met>"
            // }
            case "if":
                result = _if(json);
                return pipeBetweenStrings(result);
            
            // $literal{"value": "<value_exempted_from_further_pre-processing>"}
            case "literal":
                result = literal(json);
                return result;
            
            default: return "UNKNOWN_FUNCTION";
        }
    }
    
    private String embed(JsonObject json) throws PreProcessException {
        String src = json.get("src").getAsString();
        
        final String result;
        try (Reader reader = new InputStreamReader(server.getResource(src))) {
            result = IOUtils.toString(reader);
        } catch (IOException e) {
            throw new PreProcessException(e);
        }
        
        if (json.has("type")) {
            switch (json.get("type").getAsString()) {
                case "md":
                case "text/markdown": {
                    return Processor.process(result, MD_CONFIGURATION);
                }
            }
        }
        
        return result;
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
    
}
