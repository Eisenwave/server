package tech.eisen.server.handler;

import org.jetbrains.annotations.NotNull;
import tech.eisen.server.EisenServer;
import tech.eisen.server.Main;
import tech.eisen.server.QueryMap;
import org.apache.commons.csv.CSVPrinter;
import tech.eisen.server.http.HttpEvent;
import tech.eisen.server.http.HttpStatus;
import tech.eisen.server.http.HttpHeaders;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.ThreadLocalRandom;

public class GetTrackerImageHandler implements HttpEventHandler {
    
    /*
    static {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                image.setRGB(x, y, new Color(1F, 1F, 1F).getRGB());
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", stream);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
    */
    
    private final EisenServer server;
    
    public GetTrackerImageHandler(@NotNull EisenServer server) {
        this.server = server;
    }
    
    @SuppressWarnings("SameParameterValue")
    private static BufferedImage createRandomTrackerImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int noise = random.nextInt(2);
                image.setRGB(x, y, new Color(255-noise, 255-noise, 255-noise).getRGB());
            }
        }
        
        return image;
    }
    
    private static byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", stream);
        return stream.toByteArray();
    }
    
    @Override
    public void handle(HttpEvent event) throws IOException {
        //System.out.println(exchange.getRequestURI());
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        QueryMap queryMap = new QueryMap(event.getRequest().getURI().getQuery());
        
        if (queryMap.containsKey("topic")) {
            String topic = queryMap.get("topic");
            String meta = queryMap.getOrDefault("meta", "");
            
            try (Writer writer = new FileWriter(server.getTrackerLogFile(), true);
                 CSVPrinter printer = new CSVPrinter(writer, Main.CSV_FORMAT)) {
                printer.print(LocalDate.now().toString());
                printer.print(LocalTime.now().toString());
                printer.print(event.getPeer().getHostName());
                printer.print(topic);
                printer.print(meta);
                printer.println();
            }
        }
        
        byte[] response = imageToBytes(createRandomTrackerImage(4, 4));
    
        HttpHeaders responseHeaders = event.getResponseHeaders();
        event.setStatus(HttpStatus.OK);
        responseHeaders.setContentType("image/png", null);
        responseHeaders.setContentLength(response.length);
        event.writeHeaders();
        
        try (OutputStream bodyStream = event.getResponseStream()) {
            bodyStream.write(response);
        }
    }
    
    /*
    private static String handleQueryString(String query) {
        return String.valueOf(query).replace("\"", "'");
    }
    */
    
}
