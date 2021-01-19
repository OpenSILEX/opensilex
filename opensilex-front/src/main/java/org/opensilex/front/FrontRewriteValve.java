/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front;

import java.io.BufferedReader;
import java.io.StringReader;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.valves.rewrite.RewriteValve;

/**
 *
 * @author vince
 */
public class FrontRewriteValve extends RewriteValve {

    protected void parse(BufferedReader reader) throws LifecycleException {
        reader = new BufferedReader(new StringReader(getRewriteRules()));
        super.parse(reader);
    }

    public void initRules() throws Exception {
        setConfiguration(getRewriteRules());
    }

    private String getRewriteRules() {
        return "RewriteCond %{REQUEST_URI} ^.*/osfront/.+$\n"
                + "RewriteRule (.*)/osfront/(.*)$ " + "/$2 [L,NE]\n"
                + "RewriteCond %{REQUEST_PATH} !-f\n"
                + "RewriteRule ^/(.*)$ /index.html [L,NE]";

    }
}
