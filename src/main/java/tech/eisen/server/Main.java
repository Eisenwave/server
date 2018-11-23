package tech.eisen.server;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;

import java.io.File;

public class Main {
    
    /** "JJJJ-MM-DD", "HH-mm-ss.mmm", "IP", "topic", "meta" */
    public final static CSVFormat CSV_FORMAT = CSVFormat.DEFAULT
        .withQuoteMode(QuoteMode.ALL)
        .withRecordSeparator('\n')
        .withSkipHeaderRecord()
        .withIgnoreEmptyLines();
    
    public static void main(String... args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java -jar JAR_PATH SERVER_PORT SERVER_PATH [KEYSTORE_PATH]");
            System.exit(1);
        }
        
        int port = Integer.parseInt(args[0]);
        File directory = new File(args[1]);
        File keyStore = args.length > 2? new File(args[2]) : new File(System.getProperty("user.home"), ".keystore");
        
        EisenServer server = new EisenServer(port, directory, true, "keykex-JKS-10", keyStore);
        server.start();
        System.err.println("started server on port: " + server.getPort() + " in directory '" +
            server.getDirectory() + "'");
    }
    
}
