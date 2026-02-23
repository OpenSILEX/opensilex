package org.opensilex.core.utils;

import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A utility class to have a String -> T map, where we ensure the same uri format is used for key
 * @param <T>
 */
public class StringUriMap <T> {
    private Map<String, T> map;

    public StringUriMap(){
        map = new HashMap<>();
    }

    public StringUriMap(Map<URI, T> fromMap){
        map = new HashMap<>();
        for(Map.Entry<URI, T> entry : fromMap.entrySet()){
            String stringKey = convertUriToStringCommonFormat(entry.getKey());
            map.put(stringKey, entry.getValue());
        }
    }

    public T put(URI uriKey, T value){
        return map.put(convertUriToStringCommonFormat(uriKey), value);
    }

    public T get(URI uriKey){
        return map.get(convertUriToStringCommonFormat(uriKey));
    }

    public Set<String> keySet(){
        return map.keySet();
    }

    public Collection<T> values(){
        return map.values();
    }

    /**
     * To be used to cut out any URIs that are already keys in the map from a list of URIs
     *
     * @param potentialKeysList list to check
     * @return a new List of URIs of any URI that is not already a key
     */
    public List<URI> removeAlreadyPresentKeysFromList(List<URI> potentialKeysList){
        List<String> keysAsStrings = new java.util.ArrayList<>(potentialKeysList.stream().map(this::convertUriToStringCommonFormat).toList());
        keysAsStrings.removeAll(keySet());
        return keysAsStrings.stream().map(URI::create).toList();
    }

    public void remove(URI uriKey){
        map.remove(convertUriToStringCommonFormat(uriKey));
    }

    public T computeIfAbsent(URI key,
                           Function<String, T> mappingFunction) {
        String stringUriKey = this.convertUriToStringCommonFormat(key);
        return this.map.computeIfAbsent(stringUriKey, mappingFunction);
    }

    public void forEach(BiConsumer<String, T> action){
        map.forEach(action::accept);
    }

    public boolean isEmpty(){
        return this.map.isEmpty();
    }

    public StringUriMap<T> crushFromOtherStringUriMap(StringUriMap<T> fromMap){
        this.map = new HashMap<>();
        if(fromMap == null){
            return this;
        }
        this.map.putAll(fromMap.map);
        return this;
    }

    private String convertUriToStringCommonFormat(URI uri){
        return SPARQLDeserializers.getShortURI(uri);
    }

}
