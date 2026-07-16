package org.opensilex.deref;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.valves.rewrite.RewriteValve;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;
import java.util.List;

public class DerefRewriteValve extends RewriteValve {
    private final URI baseUri;
    private final URI docGenUri;
    private final URI rdfGenUri;

    public DerefRewriteValve(URI baseUri, URI docGenUri, URI rdfGenUri) {
        super();
        this.baseUri = baseUri;
        this.docGenUri = docGenUri;
        this.rdfGenUri = rdfGenUri;
    }


    protected void parse(BufferedReader reader) throws LifecycleException {
        reader = new BufferedReader(new StringReader(getRewriteRules()));
        super.parse(reader);
    }

    public void initRules() throws Exception {
        setConfiguration(getRewriteRules());
    }

    private String getRewriteRules() {
        return String.join("\n", List.of(
                "RewriteCond %{HTTP:Accept} .*text/html.* [NC]",
                String.format("RewriteRule .* %s?res=%s%%{REQUEST_PATH} [R=303,L]", docGenUri, baseUri),
                "RewriteCond %{HTTP:Accept} .*text/turtle.* [NC,OR]",
                "RewriteCond %{HTTP:Accept} .*application/rdf+xml.* [NC]",
                String.format("RewriteRule .* %s?res=%s%%{REQUEST_PATH} [R=303,L]", rdfGenUri, baseUri)
        ));
    }
}
