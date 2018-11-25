package tech.eisen.server.content;

import com.github.rjeschke.txtmark.DefaultDecorator;

public class EisenMarkdownDecorator extends DefaultDecorator {
    
    @Override
    public void openParagraph(StringBuilder out) {
        out.append("<p class=\"mdParagraph\">");
    }
    
    @Override
    public void openCodeBlock(StringBuilder out) {
        super.openCodeBlock(out);
        out.append("<div style=\"width: 100%\"><div class=\"mdCodeBlock\">");
    }
    
    @Override
    public void closeCodeBlock(StringBuilder out) {
        out.append("</div></div>");
        super.closeCodeBlock(out);
    }
    
    @Override
    public void openHeadline(StringBuilder out, int level) {
        out.append("<h")
            .append(level)
            .append(" class=")
            .append('"')
            .append("mdH")
            .append(level)
            .append("\"");
    }
    
    @Override
    public void openImage(StringBuilder out) {
        out.append("<img class=\"mdImg\"");
    }
    
    @Override
    public void closeImage(StringBuilder out) {
        out.append("</img>");
    }
    
    @Override
    public void openBlockquote(StringBuilder out) {
        out.append("<div class=\"mdBlockQuoteContainer\"><blockquote class=\"mdBlockQuote\">");
    }
    
    @Override
    public void closeBlockquote(StringBuilder out) {
        out.append("</blockquote></div>");
    }
    
}
