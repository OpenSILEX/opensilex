/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.service;

import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;

/**
 *
 * @author vmigot
 */
public class SPARQLPrefixMapping extends PrefixMappingImpl {

    private Map<String, String> cachedPrefixToUri;
    private Map<String,String> cachedUriToPrefix;

    private TreeSet<String> uriSortedSet;
    private TreeSet<String> prefixSortedSet;

    public SPARQLPrefixMapping() {
        super();
    }

    @Override
    public PrefixMapping setNsPrefixes(Map<String, String> other) {
        super.setNsPrefixes(other);

        cachedPrefixToUri = new PatriciaTrie<>();
        cachedUriToPrefix = new PatriciaTrie<>();

        super.getNsPrefixMap().forEach((prefix,uri) -> {
            cachedPrefixToUri.put(prefix,uri);
            cachedUriToPrefix.put(uri,prefix);
        });

        uriSortedSet = new TreeSet<>(cachedPrefixToUri.values());
        prefixSortedSet = new TreeSet<>(cachedPrefixToUri.keySet());

        return this;
    }

    /**
     * @param uri the URI string to try and prefix-compress
     * @return
     */
    @Override
    // #TODO this method seem to be useless since the returned result is the same as super.shortForm(uri)
    public String shortForm(String uri) {
        String candidateKey = null;
        String candidateValue = null;

        // use the cached prefix map, else getNsPrefixMap() create a new copy of the map at each call
        Map<String, String> map = cachedPrefixToUri;
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

    public String shortFormIndexed(String uriToFormat){
        String candidateURI = uriSortedSet.floor(uriToFormat);
        if(candidateURI != null){
            String prefix = cachedUriToPrefix.get(candidateURI);
            String part = uriToFormat.substring(candidateURI.length());
            return prefix + ":" + part;
        }
        return super.shortForm(uriToFormat);
    }

    public String expandedFormIndexed(String uriToFormat){
        String candidatePrefix = prefixSortedSet.floor(uriToFormat);
        if(candidatePrefix != null){
            String uri = cachedPrefixToUri.get(candidatePrefix);
            String part = uri.substring(candidatePrefix.length());
            return uri + "#" + part;
        }
        return super.expandPrefix(uriToFormat);
    }

}
