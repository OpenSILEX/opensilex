package org.opensilex.deref;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DerefServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");

        var out = resp.getWriter();
        out.println(String.format("""
                <!DOCTYPE html>
                <html>
                    <head>
                        <title>Deref servlet</title>
                    </head>
                    <body>
                        <h1>Deref</h1>
                        <p>The requested URI is <pre>%s</pre></p>
                    </body>
                </html>
                """, req.getParameter("res")));
        out.close();
    }
}
