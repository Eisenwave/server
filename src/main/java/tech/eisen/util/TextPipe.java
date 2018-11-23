package tech.eisen.util;

import java.io.*;
import java.nio.charset.Charset;

public interface TextPipe {
    
    abstract void pipeToWriter(Reader input, Writer output) throws IOException;
    
    default String pipeToString(Reader input) throws IOException {
        StringWriter writer = new StringWriter();
        pipeToWriter(input, writer);
        return writer.toString();
    }
    
    default byte[] pipeToBytes(Reader input, Charset charset) throws IOException {
        String str = pipeToString(input);
        return str.getBytes(charset);
    }
    
    default byte[] pipeToBytes(Reader input) throws IOException {
        return pipeToBytes(input, Charset.defaultCharset());
    }
    
}
