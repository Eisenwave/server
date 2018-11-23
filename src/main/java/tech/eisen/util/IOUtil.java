package tech.eisen.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class IOUtil {
    
    private IOUtil() {}
    
    public static void pipe(InputStream source, OutputStream sink, int blockSize) throws IOException {
        byte[] buffer = new byte[blockSize];
        
        for (int read = source.read(buffer); read >= 0; read = source.read(buffer))
            sink.write(buffer, 0, read);
    }
    
    public static void pipeTwice(InputStream source, OutputStream a, OutputStream b, int blockSize) throws IOException {
        byte[] buffer = new byte[blockSize];
        
        for (int read = source.read(buffer); read >= 0; read = source.read(buffer)) {
            a.write(buffer, 0, read);
            b.write(buffer, 0, read);
        }
    }
    
}
