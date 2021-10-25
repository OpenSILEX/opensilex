/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.dal;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.opensilex.sparql.utils.URIGenerator;

/**
 *
 * @author vmigot
 */
public class ScientificObjectURIGenerator implements URIGenerator<String> {

    private final String rootURI;

    public ScientificObjectURIGenerator(URI rootURI) {
        this.rootURI = rootURI.toString();
    }

    @Override
    public URI generateURI(String prefix, String name, int retryCount) throws UnsupportedEncodingException, URISyntaxException {
        String baseURI;
        if (name != null) {
            baseURI = rootURI + "/so-" + URIGenerator.normalize(name);
        } else {
            baseURI = rootURI + "/so-" + randomAlphaNumeric(7);
        }

        if (retryCount > 0) {
            baseURI += "-" + retryCount;
        }

        return new URI(baseURI);
    }

    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static String randomAlphaNumeric(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }

        return builder.toString().toLowerCase();
    }

}
