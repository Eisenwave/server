package tech.eisen.server;

import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class Main {
    
    //private final static Option OPT_KEYSTORE_PW = new Option()
    
    private final static Options OPTIONS = new Options()
        .addRequiredOption("p", "--port", true, "server port")
        .addRequiredOption("d", "--directory", true, "public root directory")
        .addOption("s", "https", false, "use HTTPS instead of HTTP")
        .addOption("k", "keystore", true, "path to keystore (HTTPS mode)")
        .addOption("S", "keystore-password", true, "keystore password (HTTPS mode)")
        .addOption("K", "key-password", true, "key password in keystore (HTTPS mode)")
        .addOption("q", "quit", false, "quit mode (no logging to stdout)")
        .addOption("v", "verbose", false, "verbose mode (additional logging)");
    
    public final static CSVFormat CSV_FORMAT = CSVFormat.DEFAULT
        .withQuoteMode(QuoteMode.ALL)
        .withRecordSeparator('\n')
        .withSkipHeaderRecord()
        .withIgnoreEmptyLines();
    
    public static void main(String... args) throws Exception {
        CommandLine command = new DefaultParser().parse(OPTIONS, args);
        
        final int port = Integer.parseInt(command.getOptionValue('p'));
        final File directory = new File(command.getOptionValue('d'));
        final Verbosity verbosity = command.hasOption('q')?
            Verbosity.QUIT : command.hasOption('v')?
            Verbosity.VERBOSE : Verbosity.NORMAL;
        
        final boolean https = command.hasOption('s');
        if (!https) {
            new EisenServer(port, directory, verbosity).start();
            return;
        }
    
        final File keyStore = parseKeystoreFile(command.getOptionValue('k'));
        final String keyStorePassword = requireBecauseHTTPS("-S|--keystore-password", command.getOptionValue('S'));
        final String keyPassword = requireBecauseHTTPS("-K|--key-password", command.getOptionValue('K'));
        
        EisenServer server = new EisenServer(port, directory, verbosity,
            true, keyStore, keyStorePassword.toCharArray(), keyPassword.toCharArray());
        
        server.start();
    }
    
    @NotNull
    private static String requireBecauseHTTPS(@NotNull String option, @Nullable String str) {
        if (str == null) {
            System.err.println("HTTPS mode requires " + option + " to be set");
            System.exit(1);
        }
        
        return str;
    }
    
    private static File parseKeystoreFile(@Nullable String str) {
        str = requireBecauseHTTPS("keystore", str);
        
        File file = new File(str);
        if (!file.exists()) {
            System.err.println("-k|--keystore: " + str + " could not be found");
            System.exit(1);
        }
        
        return file;
    }
    
    private static boolean parseNullableBoolean(@Nullable String str) {
        return str != null && Boolean.parseBoolean(str);
    }
    
}
