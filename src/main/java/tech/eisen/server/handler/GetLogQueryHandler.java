package tech.eisen.server.handler;

import org.jetbrains.annotations.NotNull;
import tech.eisen.server.EisenServer;
import tech.eisen.server.Main;
import tech.eisen.server.QueryMap;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import tech.eisen.server.http.HttpEvent;
import tech.eisen.server.http.HttpStatus;
import tech.eisen.server.http.HttpHeaders;

import java.io.*;
import java.nio.charset.Charset;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class GetLogQueryHandler extends EisenHttpHandler {
    
    public GetLogQueryHandler(@NotNull EisenServer server) {
        super(server);
    }
    
    @Override
    public void handleEvent(HttpEvent event) throws IOException {
        StringWriter writer = new StringWriter();
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        QueryMap query = new QueryMap(event.getRequest().getURI().getQuery());
        
        String topic = query.get("topic");
        LocalDate minDate = query.containsKey("minDate")? LocalDate.parse(query.get("minDate")) : null;
        LocalDate maxDate = query.containsKey("maxDate")? LocalDate.parse(query.get("maxDate")) : null;
        //LocalTime minTime = query.containsKey("minTime")? LocalTime.parse(query.get("minTime")) : null;
        //LocalTime maxTime = query.containsKey("maxTime")? LocalTime.parse(query.get("maxTime")) : null;
        DayOfWeek weekDay = query.containsKey("weekDay")? parseDayOfWeek(query.get("weekDay")) : null;
        
        try (CSVParser parser = CSVParser.parse(server.getTrackerLogFile(), Charset.forName("UTF-8"), Main.CSV_FORMAT);
             CSVPrinter printer = new CSVPrinter(writer, Main.CSV_FORMAT)) {
            for (CSVRecord record : parser) {
                LocalDate date = LocalDate.parse(record.get(0));
                //LocalTime time = LocalTime.parse(record.get(1));
                
                if (topic != null && !record.get(3).equals(topic)
                    || minDate != null && date.isBefore(minDate)
                    || maxDate != null && date.isAfter(maxDate)
                    //|| minTime != null && time.isBefore(minTime)
                    || weekDay != null && date.getDayOfWeek() != weekDay)
                    continue;
    
                for (String value : record)
                    printer.print(value);
                printer.println();
            }
        }
        
        String responseString = writer.toString();
        byte[] response = responseString.getBytes(Charset.forName("UTF-8"));
    
        HttpHeaders responseHeaders = event.getResponseHeaders();
        responseHeaders.setContentType("text/plain", Charset.defaultCharset());
        responseHeaders.setContentLength(response.length);
        event.setStatus(HttpStatus.OK);
        event.writeHeaders();
        
        try (OutputStream bodyStream = event.getResponseStream()) {
            bodyStream.write(response);
        }
    }
    
    private static DayOfWeek parseDayOfWeek(String str) {
        String upper = str.toUpperCase();
        switch (upper) {
            case "MO":
            case "MON": return DayOfWeek.MONDAY;
            case "TU":
            case "TUE": return DayOfWeek.TUESDAY;
            case "WE":
            case "WED": return DayOfWeek.WEDNESDAY;
            case "TH":
            case "THU": return DayOfWeek.THURSDAY;
            case "FR":
            case "FRI": return DayOfWeek.FRIDAY;
            case "SA":
            case "SAT": return DayOfWeek.SATURDAY;
            case "SU":
            case "SUN": return DayOfWeek.SUNDAY;
            default: return DayOfWeek.valueOf(upper);
        }
    }
    
}
