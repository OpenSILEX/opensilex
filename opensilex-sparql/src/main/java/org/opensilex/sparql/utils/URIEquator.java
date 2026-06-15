package org.opensilex.sparql.utils;

import org.apache.commons.collections4.Equator;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;

/**
 * An implementation of {@link Equator} for OpenSILEX's URIs. This allow for comparison in supported collection methods.
 * For example : {@code CollectionUtils.isEqualCollection(uriListA, uriListB, new URIEquator());}
 */
public class URIEquator implements Equator<URI> {
    @Override
    public boolean equate(URI a, URI b) {
        return SPARQLDeserializers.compareURIs(a, b);
    }

    @Override
    public int hash(URI uri) {
        return SPARQLDeserializers.formatURI(uri).hashCode();
    }
}
