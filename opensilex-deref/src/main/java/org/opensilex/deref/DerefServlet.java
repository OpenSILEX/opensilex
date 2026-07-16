package org.opensilex.deref;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DerefServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");

        var resourceUri = req.getParameter("res");
        var accept = req.getHeader("Accept");

        var out = resp.getWriter();
        out.println(String.format("""
                <!DOCTYPE html>
                <html>
                    <head>
                        <title>OpenSILEX Deref Module</title>
                    </head>
                    <body>
                        <h1>Generated document from dereferencing request</h1>
                        <p>The requested URI is <a href="%s"><code>%s</code></p></a>
                        <p>The clients accepts the following formats : <code>%s</code></p>
                    </body>
                </html>
                """, resourceUri, resourceUri, accept));
        out.close();
    }
}
