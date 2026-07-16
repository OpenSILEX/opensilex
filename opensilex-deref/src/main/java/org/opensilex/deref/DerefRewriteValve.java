package org.opensilex.deref;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.valves.rewrite.RewriteValve;

import java.io.BufferedReader;
import java.io.StringReader;

public class DerefRewriteValve extends RewriteValve {
    protected void parse(BufferedReader reader) throws LifecycleException {
        reader = new BufferedReader(new StringReader(getRewriteRules()));
        super.parse(reader);
    }

    public void initRules() throws Exception {
        setConfiguration(getRewriteRules());
    }

    private String getRewriteRules() {
        return """
                    RewriteCond %{REQUEST_URI} ^.*/part0/.+$
                    RewriteRule (.*)/part0/(.*)$ $0 [L,NE]
                    RewriteCond %{REQUEST_URI} ^.*/part1/.+$
                    RewriteRule (.*)/part1/(.*)$ $1 [L,NE]
                    RewriteCond %{REQUEST_URI} ^.*/part2/.+$
                    RewriteRule (.*)/part2/(.*)$ $2 [L,NE]
                    RewriteCond %{REQUEST_PATH} ^.*/example/.+$
                    RewriteRule .* http://example.org/myredirect [R=303,L,NE]
                    RewriteCond %{REQUEST_PATH} ^/example3/$
                    RewriteRule .* http://example.org/myredirect [R=303,L,NE]
                    RewriteRule .* http://example.org/myredirect?res=%{REQUEST_URI}&host=${REMOTE_HOST} [R=303,L]
                """;

    }
}
