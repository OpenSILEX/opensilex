package org.opensilex.deref;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.valves.rewrite.RewriteValve;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;

public class DerefRewriteValve extends RewriteValve {
    private final URI baseUri;
    private final URI docGenUri;

    public DerefRewriteValve(URI baseUri, URI docGenUri) {
        super();
        this.baseUri = baseUri;
        this.docGenUri = docGenUri;
    }


    protected void parse(BufferedReader reader) throws LifecycleException {
        reader = new BufferedReader(new StringReader(getRewriteRules()));
        super.parse(reader);
    }

    public void initRules() throws Exception {
        setConfiguration(getRewriteRules());
    }

    private String getRewriteRules() {
        return String.format("RewriteRule .* %s?res=%s%%{REQUEST_PATH} [R=303,L]", docGenUri, baseUri);
    }
}
