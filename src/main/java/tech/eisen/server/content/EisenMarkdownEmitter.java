package tech.eisen.server.content;

import com.github.rjeschke.txtmark.*;

import java.util.*;

public class EisenMarkdownEmitter implements BlockEmitter {
    
    private final static String[] JAVA_KEYWORDS = {
        "abstract",
        "boolean",
        "break",
        "byte",
        "case",
        "catch",
        "char",
        "class",
        "const *",
        "continue",
        "default",
        "do",
        "double",
        "else",
        "extends",
        "final",
        "finally",
        "float",
        "for",
        "goto *",
        "if",
        "implements",
        "import",
        "instanceof",
        "int",
        "interface",
        "long",
        "native",
        "new",
        "null",
        "package",
        "private",
        "protected",
        "public",
        "return",
        "short",
        "static",
        "super",
        "switch",
        "synchronized",
        "this",
        "throw",
        "throws",
        "transient",
        "try",
        "void",
        "volatile",
        "while",
        "assert",
        "enum",
        "strictfp"
    };
    
    private final static Set<String> JAVA_KEYWORDS_SET = new HashSet<>(Arrays.asList(JAVA_KEYWORDS));
    
    private final static int S_DEFAULT = 0, S_NUMBER = 1, S_STR = 2, S_IDENTIFIER = 3, S_CHAR = 4;
    
    // TODO FINISH
    @Override
    public void emitBlock(StringBuilder out, List<String> lines, String meta) {
        int state = S_DEFAULT;
        StringBuilder buffer = new StringBuilder();
        
        for (String line : lines) {
            for (char c : line.toCharArray()) {
                switch (state) {
                    
                    case S_DEFAULT:
                        if (Character.isDigit(c))
                            state = S_NUMBER;
                        else if (c == '"')
                            state = S_STR;
                        else if (c == '\'')
                            state = S_CHAR;
                        else if (!Character.isJavaIdentifierPart(S_IDENTIFIER))
                            state = S_IDENTIFIER;
                        
                        buffer.append(c);
                        break;
                    
                    case S_NUMBER:
                        if (Character.isDigit(c))
                            buffer.append(c);
                        else
                            buffer.append("</div>");
                    
                }
                
                
            }
        }
        
    }
    
}
