package tech.eisen.server.http.header;

import org.junit.Test;
import tech.eisen.server.http.HttpHeaders;

import static org.junit.Assert.*;

public class HttpHeadersTest {
    
    @Test
    public void testAcceptEncoding() {
        helpTestPreferredEncoding("*", "");
        helpTestPreferredEncoding("*", "*");
        helpTestPreferredEncoding("gzip", "gzip");
        helpTestPreferredEncoding("deflate", "deflate; q=5, identity; q=3");
        helpTestPreferredEncoding("identity", "deflate; q=3, identity; q=6");
    }
    
    private static void helpTestPreferredEncoding(String expected, String acceptEncoding) {
        assertEquals(expected, new HttpHeaders.AcceptEncoding(acceptEncoding).getPreferredEncoding());
    }
    
}
