package tech.eisen.server.security;

import eisenwave.torrens.io.TextDeserializer;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import tech.eisen.server.Main;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeserializerPasswords implements TextDeserializer<Map<String, char[]>> {
    
    @NotNull
    @Override
    public Map<String, char[]> fromReader(Reader reader) throws IOException {
        Map<String, char[]> result = new HashMap<>();
        CSVParser parser = CSVParser.parse(reader, Main.CSV_FORMAT);
        
        List<CSVRecord> records = parser.getRecords();
        for (int i = 1; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            result.put(record.get(0), record.get(1).toCharArray());
            //System.out.println(record.get(0) + " " + record.get(1));
        }
        
        return result;
    }
    
}
