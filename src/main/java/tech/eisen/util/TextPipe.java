package tech.eisen.util;

import java.io.*;
import java.nio.charset.Charset;

public interface TextPipe {
    
    abstract void pipeToWriter(Reader input, Writer output) throws IOException;
    
    default byte[] pipeBetweenBytes(byte[] input, Charset commonCharset) throws IOException {
        return pipeToBytes(new InputStreamReader(new ByteArrayInputStream(input), commonCharset), commonCharset);
    }
    
    default char[] pipeBetweenChars(char[] input) throws IOException {
        return pipeToChars(new CharArrayReader(input));
    }
    
    default String pipeBetweenStrings(String input) throws IOException {
        return pipeToString(new StringReader(input));
    }
    
    default void pipeFromBytes(byte[] input, Charset charset, Writer output) throws IOException {
        Reader reader = new InputStreamReader(new ByteArrayInputStream(input), charset);
        pipeToWriter(reader, output);
    }
    
    default void pipeFromBytes(byte[] input, Writer output) throws IOException {
        Reader reader = new InputStreamReader(new ByteArrayInputStream(input));
        pipeToWriter(reader, output);
    }
    
    default void pipeFromChars(char[] input, Writer output) throws IOException {
        Reader reader = new CharArrayReader(input);
        pipeToWriter(reader, output);
    }
    
    default void pipeFromString(String input, Writer output) throws IOException {
        StringReader reader = new StringReader(input);
        pipeToWriter(reader, output);
    }
    
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
    
    default char[] pipeToChars(Reader input) throws IOException {
        CharArrayWriter writer = new CharArrayWriter();
        pipeToWriter(input, writer);
        return writer.toCharArray();
    }
    
}
