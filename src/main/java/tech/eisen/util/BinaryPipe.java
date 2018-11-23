package tech.eisen.util;

import java.io.InputStream;
import java.io.OutputStream;

public interface BinaryPipe {
    
    abstract void run(InputStream input, OutputStream output);
    
}
