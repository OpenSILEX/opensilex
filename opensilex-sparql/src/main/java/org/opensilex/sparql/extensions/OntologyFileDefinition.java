/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.extensions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.riot.Lang;

/**
 *
 * @author vince
 */
public class OntologyFileDefinition {

    public static final char PREFIX_CHAR = '#';

    private final URI graphURI;

    private final String filePath;

    private final Lang fileType;

    private final String prefix;

    private final URI namespace;

    /**
     *
     * @param uri full URI of the ontology
     * @param filePath path to the ontology
     * @param fileType rdf file extension
     * @param prefix prefix corresponding with the uri. Can be null or empty
     * @throws IllegalArgumentException if
     * <ul>
     *     <li>uri or filePath is null or empty</li>
     *     <li>fileType is null</li>
     * </ul>
     *
     */
    public OntologyFileDefinition(String uri, String filePath, Lang fileType, String prefix) throws IllegalArgumentException {

        if(StringUtils.isEmpty(uri)){
            throw new IllegalArgumentException("Null or empty uri : "+uri);
        }
        if(StringUtils.isEmpty(filePath)){
            throw new IllegalArgumentException("Null or empty filePath : "+filePath);
        }
        Objects.requireNonNull(fileType);

        this.namespace = URI.create(uri);

        // remove # character for graph storage
        if(uri.charAt(uri.length()-1) == PREFIX_CHAR){
            this.graphURI = URI.create(uri.substring(0,uri.length()-1));
        }else{
            this.graphURI = URI.create(uri);
        }

        this.filePath = filePath;
        this.fileType = fileType;
        this.prefix = prefix;
    }

    /**
     *
     * @return URI of the graph in which the ontology must be stored.
     * if the URI ends with {@link OntologyFileDefinition#PREFIX_CHAR} then this character is deleted for graph storage
     */
    public URI getGraphURI() {
        return graphURI;
    }

    public String getFilePath() {
        return filePath;
    }

    public Lang getFileType() {
        return fileType;
    }

    public String getPrefix() {
        return prefix;
    }

    public URI getNamespace() {
        return namespace;
    }

}
