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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class EisenServer {
    
    private final static ClassLoader CLASS_LOADER = EisenServer.class.getClassLoader();
    
    private final ResourceCache resourceCache = new ResourceCache();
    private final PasswordStore passwordStore = new PasswordStore();
    
    private final boolean https;
    private final int port;
    private final File directory;
    private final File trackerLogFile;
    
    private String passphrase;
    private final File keyStore;
    
    private final EisenHttpHandler rootHandler = new RootHttpHandler(this);
    private final Map<Predicate<HttpRequest>, EisenHttpHandler> handlerMap = new HashMap<>();
    
    public EisenServer(int port, @NotNull File directory, boolean https, String passphrase, File keyStore) {
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("Port must be between 0 and " + 0xFFFF);
        if (!directory.isDirectory() && !directory.mkdirs())
            throw new IllegalArgumentException(String.format("Directory %s is not a valid directory", directory));
        
        this.port = port;
        this.directory = directory;
        this.trackerLogFile = new File(directory, "log.csv");
        
        this.https = https;
        this.passphrase = passphrase;
        this.keyStore = keyStore;
    }
    
    public EisenServer(int port, @NotNull File directory) {
        this(port, directory, false, null, null);
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
        /*char[] pw = SecureHashes.hashPasswordPBDKDF2("".toCharArray());
        System.out.println(pw);
        System.out.println(SecureHashes.validatePasswordPBDKDF2("".toCharArray(), pw));*/
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
        
        handlerMap.put(getLogin, new LoginHandler(this));
    }
    
    private void loadAccounts() throws IOException {
        new DeserializerPasswords()
            .fromResource(EisenServer.class, "passwords.csv")
            .forEach(passwordStore::setHash);
    }
    
    @SuppressWarnings("UnnecessaryLocalVariable")
    private SSLContext createSSLContext(String passphrase) throws IOException, GeneralSecurityException {
        
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream(this.keyStore), passphrase.toCharArray());
        
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, "keykex-CERT-3".toCharArray());
        KeyManager[] km = keyManagerFactory.getKeyManagers();
        
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);
        TrustManager[] tm = trustManagerFactory.getTrustManagers();
        
        SSLContext sslContext = SSLContext.getInstance("TLSv1");
        sslContext.init(km, tm, null);
        
        return sslContext;
    }
    
    private SSLServerSocket initSSL() throws IOException, GeneralSecurityException {
        SSLContext sslContext = createSSLContext(passphrase);
        
        // Create server socket factory
        SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
        
        // Create server socket
        return (SSLServerSocket) sslServerSocketFactory.createServerSocket(this.port);
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
    
    public byte[] getResourceAsBytes(String path) throws IOException {
        return resourceCache.getAllBytes(CLASS_LOADER.getResource(path));
    }
    
    public String getResourceAsString(String path) throws IOException {
        return resourceCache.getAsString(CLASS_LOADER.getResource(path));
    }
    
    public void handleEvent(HttpEvent event) throws IOException {
        for (Map.Entry<Predicate<HttpRequest>, EisenHttpHandler> entry : handlerMap.entrySet()) {
            if (entry.getKey().test(event.getRequest())) {
                entry.getValue().handle(event);
                return;
            }
        }
        
        rootHandler.handle(event);
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
