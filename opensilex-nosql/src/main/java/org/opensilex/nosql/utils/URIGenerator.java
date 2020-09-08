/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sammy
 * 
 * /!\ Class copié-collé
 * J'ai copié cette class depuis org.opensilex.sparql.utils:
 *      - la version originale sépare les composants par des #, mais demandé des /
 *      - la version originale est utilisé dans de nombreux autres services
 * Peut-être intéressant: uniformiser les URI ou génraliser cette class
 */
public interface URIGenerator<T> {
     public default URI generateURI(String prefix, T instance, int retryCount) throws Exception {
        if (retryCount > 0) {
            return new URI(prefix + "/" + getInstanceURI(instance) + "/" + retryCount);
        } else {
            return new URI(prefix + "/" + getInstanceURI(instance));
        }
    }

    public default String getInstanceURI(T instance) throws UnsupportedEncodingException {
        return instance.toString();
    }

    public static String normalize(Object txt) throws UnsupportedEncodingException {

        String value = txt.toString()
                .toLowerCase()
                .trim()
                .replaceAll(" +", " ")
                .replace(" ", "-")
                .replaceAll("\\$|&|\\+|\\||,|/|:|;|=|\\?|@|<|>|#|%|\\{|\\}|\\(|\\)|\\^|~|\\[|\\]|\\\\|\"|'|`|\\*|\\!|\\.", "")
                .replaceAll("%", "");

        value = Normalizer
                .normalize(value, Normalizer.Form.NFD)
                .replaceAll(" ", "")
                .replaceAll("[^\\p{ASCII}]", "");

        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }

    public static String normalize(String[] parts) throws UnsupportedEncodingException {
        List<String> normalizedString = new ArrayList<>(parts.length);
        for (int i = 0; i < parts.length; i++) {
            normalizedString.add(normalize(parts[i]));
        }

        return String.join("/", normalizedString.toArray(parts));
    }
}
