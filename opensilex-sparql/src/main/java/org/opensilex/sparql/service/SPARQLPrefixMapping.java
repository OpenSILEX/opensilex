/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.service;

import java.util.Map;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;

/**
 *
 * @author vmigot
 */
public class SPARQLPrefixMapping extends PrefixMappingImpl {

    private Map<String, String> cachedPrefixMap;

    public SPARQLPrefixMapping() {
        super();
    }

    @Override
    public PrefixMapping setNsPrefixes(Map<String, String> other) {
         super.setNsPrefixes(other);
         cachedPrefixMap = getNsPrefixMap();
         return this;
    }

    @Override
    public String shortForm(String uri) {
        String candidateKey = null;
        String candidateValue = null;

        // use the cached prefix map, else getNsPrefixMap() create a new copy of the map at each call
        Map<String, String> map = cachedPrefixMap;
        for (String key : map.keySet()) {
            String value = map.get(key);

            if (uri.startsWith(value)) {
                if (candidateKey == null) {
                    candidateKey = key;
                    candidateValue = uri.substring(value.length());
                } else {
                    String newCandidateValue = uri.substring(value.length());
                    if (newCandidateValue.length() < candidateValue.length()) {
                        candidateKey = key;
                        candidateValue = newCandidateValue;
                    }
                }
            }
        }

        if (candidateKey == null) {
            return uri;
        } else {
            return candidateKey + ":" + candidateValue;
        }
    }

}
