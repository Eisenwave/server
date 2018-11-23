package tech.eisen.server;

import java.util.HashMap;

public class QueryMap extends HashMap<String, String> {
    
    public QueryMap(String query) {
        if (query == null) return;
        
        for (String field : query.trim().split("&")) {
            String[] keyVal = field.trim().split("=", 2);
            if (keyVal.length < 2)
                put(keyVal[0], keyVal[0]);
            else
                put(keyVal[0], keyVal[1]);
        }
    }
    
}
