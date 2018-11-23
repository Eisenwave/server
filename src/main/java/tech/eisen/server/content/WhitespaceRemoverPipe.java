package tech.eisen.server.content;

import tech.eisen.util.TextPipe;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class WhitespaceRemoverPipe implements TextPipe {
    
    @Override
    public void pipeToWriter(Reader input, Writer output) throws IOException {
        for (int c = input.read(); c > 0; c = input.read())
            if (!Character.isWhitespace(c))
                output.write(c);
    }
    
}
