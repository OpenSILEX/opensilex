package org.opensilex.sparql.utils;

import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A URI Set class that handles making sure everything is using some URI format to do comparisons
 */
public class StringUriSet {
    private Set<String> actualSet;

    public StringUriSet(){
        actualSet = new HashSet<>();
    }
    public StringUriSet(Collection<URI> passedInitialCollection){
        actualSet = new HashSet<>(passedInitialCollection.stream().map(StringUriSet::normalizeUri).toList());
    }

    public Set<String> getSet(){
        return actualSet;
    }
    public Set<URI> getSetAsURIs(){
        return actualSet.stream().map(URI::create).collect(Collectors.toSet());
    }

    public void add(String uriString){
        String actualUriToUse = normalizeUri(uriString);
        actualSet.add(actualUriToUse);
    }
    public void add(URI uri){
        String actualUriToUse = normalizeUri(uri);
        actualSet.add(actualUriToUse);
    }
    public void addAllUriStrings(Collection<String> uriStrings){
        var actualStringsToAdd = uriStrings.stream().map(StringUriSet::normalizeUri).toList();
        actualSet.addAll(actualStringsToAdd);
    }
    public void addAll(Collection<URI> uris){
        var actualStringsToAdd = uris.stream().map(StringUriSet::normalizeUri).toList();
        actualSet.addAll(actualStringsToAdd);
    }
    public void forEach(Consumer<String> action){
        actualSet.forEach(action);
    }


    private static String normalizeUri(String uri){
        return SPARQLDeserializers.getShortURI(uri);
    }
    private static String normalizeUri(URI uri){
        return SPARQLDeserializers.getShortURI(uri);
    }


}
