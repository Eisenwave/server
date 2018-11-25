package tech.eisen.server;

import org.jetbrains.annotations.NotNull;
import tech.eisen.server.handler.*;
import tech.eisen.server.handler.match.*;
import tech.eisen.server.http.*;
import tech.eisen.server.security.DeserializerPasswords;
import tech.eisen.server.security.PasswordStore;

import javax.net.ssl.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.*;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class EisenServer {
    
    private final static ClassLoader CLASS_LOADER = EisenServer.class.getClassLoader();
    
    private void log(HttpEvent event, long millis) {
        if (verbosity == Verbosity.QUIT) return;
        
        HttpRequest request = event.getRequest();
        final String path = request.getURI().getPath(),
            remoteHost = event.getPeer().getHostName(),
            method = request.getMethod().toString(),
            status = Integer.toString(event.getStatus().getCode()),
            statusText = event.getStatus().name().replace('_', ' ');
        
        String msg = String.format("[%tH:%1$tM:%1$tS:%1$tL] %s %s \"%s\" %s (%s) in %sms", LocalTime.now(),
            remoteHost, method, path, status, statusText, millis);
        System.out.println(msg);
    }
    
    private final ResourceCache resourceCache = new ResourceCache();
    private final PasswordStore passwordStore = new PasswordStore();
    
    
    private final int port;
    private final Verbosity verbosity;
    private final File directory;
    
    private final boolean https;
    private final char[] keyStorePass, keyPass;
    private final File keyStore;
    
    private final File trackerLogFile;
    
    
    private final HttpEventHandler rootHandler = new GetHeadRootHttpHandler(this);
    private final Map<Predicate<HttpRequest>, HttpEventHandler> handlerMap = new HashMap<>();
    
    public EisenServer(int port, @NotNull File directory, @NotNull Verbosity verbosity,
                       boolean https, File keyStore, char[] keyStorePass, char[] keyPass) {
        
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("Port must be between 0 and " + 0xFFFF);
        if (!directory.isDirectory() && !directory.mkdirs())
            throw new IllegalArgumentException(String.format("Directory %s is not a valid directory", directory));
        
        this.port = port;
        this.directory = directory;
        this.verbosity = verbosity;
        
        this.trackerLogFile = new File(directory, "log.csv");
        
        this.https = https;
        this.keyStorePass = keyStorePass;
        this.keyPass = keyPass;
        this.keyStore = keyStore;
    }
    
    public EisenServer(int port, @NotNull File directory, @NotNull Verbosity verbosity) {
        this(port, directory, verbosity, false, null, null, null);
    }
    
    @SuppressWarnings("deprecation")
    public void start() throws IOException, GeneralSecurityException {
        if (https)
            startHttps();
        else
            startHttp();
    }
    
    @Deprecated
    private void startHttp() throws IOException {
        throw new UnsupportedOperationException();
    }
    
    private void startHttps() throws IOException, GeneralSecurityException {
        SSLServerSocket serverSocket = initSSL();
        registerEvents();
        loadAccounts();
        
        //noinspection InfiniteLoopStatement
        while (true) {
            SSLSocket sslSocket = (SSLSocket) serverSocket.accept();
            
            new ServerThread(this, sslSocket).start();
        }
    }
    
    private void registerEvents() {
        final URI login;
        try {
            login = new URI("/login");
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
        
        Predicate<HttpRequest> getLogin = new BaseURIAndMethodMatcher(login, HttpRequestMethod.GET);
        
        handlerMap.put(getLogin, new GetLoginHandler(this));
    }
    
    private void loadAccounts() throws IOException {
        new DeserializerPasswords()
            .fromResource(EisenServer.class, "passwords.csv")
            .forEach(passwordStore::setHash);
    }
    
    @SuppressWarnings("UnnecessaryLocalVariable")
    private SSLContext createSSLContext() throws IOException, GeneralSecurityException {
        
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream(this.keyStore), this.keyStorePass);
        
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, this.keyPass);
        KeyManager[] km = keyManagerFactory.getKeyManagers();
        
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);
        TrustManager[] tm = trustManagerFactory.getTrustManagers();
        
        SSLContext sslContext = SSLContext.getInstance("TLSv1");
        sslContext.init(km, tm, null);
        
        return sslContext;
    }
    
    private SSLServerSocket initSSL() throws IOException, GeneralSecurityException {
        SSLContext sslContext = createSSLContext();
        
        // Create server socket factory
        SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
        
        // Create server socket
        return (SSLServerSocket) sslServerSocketFactory.createServerSocket(this.port);
    }
    
    // GETTERS
    
    public Verbosity getVerbosity() {
        return verbosity;
    }
    
    public boolean isVerbose() {
        return verbosity == Verbosity.VERBOSE;
    }
    
    public boolean isQuit() {
        return verbosity == Verbosity.QUIT;
    }
    
    public ResourceCache getResourceCache() {
        return resourceCache;
    }
    
    public PasswordStore getPasswordStore() {
        return passwordStore;
    }
    
    public InputStream getResource(URL url) throws IOException {
        return resourceCache.openStream(url);
    }
    
    public InputStream getResource(String path) throws IOException {
        return getResource(CLASS_LOADER.getResource(path));
    }
    
    public Reader getResourceText(URL url) throws IOException {
        return resourceCache.openReader(url);
    }
    
    public Reader getResourceText(String path) throws IOException {
        return getResourceText(CLASS_LOADER.getResource(path));
    }
    
    /**
     * <p>
     * Handles the event using the server's {@link HttpEventHandler event handlers}.
     * The server will attempt to find the correct handler for a given event based on the kind of request being made.
     * Only one handler may be invoked per event.
     * </p>
     * <p>
     * If no event handler could be found, the server's root handler gets invoked.
     * </p>
     * <p>
     * If an exception occurs when invoking the handler, the server's exception handler gets invoked. If HTTP headers
     * have not be written to the event's response stream yet, an error page will be written as a response instead.
     * </p>
     *
     * @param event the event
     * @throws IOException if an I/O error occurs when reading or writing to the event's streams
     */
    public void handleEvent(HttpEvent event) throws IOException {
        long before = System.currentTimeMillis();
        try {
            for (Map.Entry<Predicate<HttpRequest>, HttpEventHandler> entry : handlerMap.entrySet()) {
                if (entry.getKey().test(event.getRequest())) {
                    entry.getValue().handle(event);
                    log(event, System.currentTimeMillis() - before);
                    return;
                }
            }
    
            rootHandler.handle(event);
            log(event, System.currentTimeMillis() - before);
        } catch (HttpException ex) {
            ErrorHttpHandler handler = new ErrorHttpHandler(this, ex);
            handler.handle(event);
            log(event, System.currentTimeMillis() - before);
        } catch (Exception ex) {
            if (ex instanceof IOException)
                throw ex;
            
            HttpException httpEx = new HttpException(HttpStatus.SERVER_ERROR, "Uncaught exception", ex);
            ErrorHttpHandler handler = new ErrorHttpHandler(this, httpEx);
            handler.handle(event);
            log(event, System.currentTimeMillis() - before);
        }
    }
    
    public boolean isHttps() {
        return https;
    }
    
    public int getPort() {
        return port;
    }
    
    @NotNull
    public File getDirectory() {
        return directory;
    }
    
    @NotNull
    public File getTrackerLogFile() {
        return trackerLogFile;
    }
    
}
