/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.extensions;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.jena.riot.Lang;

/**
 *
 * @author vince
 */
public class OntologyFileDefinition {

    private final URI uri;

    private final String filePath;

    private final Lang fileType;

    private final String prefix;

    private final URI prefixUri;

    public OntologyFileDefinition(String uri, String filePath, Lang fileType, String prefix) throws URISyntaxException {
        String baseUri = uri.toString().replaceAll("#", "");
        this.uri = new URI(baseUri);
        this.prefixUri = new URI(baseUri + "#");
        this.filePath = filePath;
        this.fileType = fileType;
        this.prefix = prefix;
    }

    public URI getUri() {
        return uri;
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

    public URI getPrefixUri() {
        return prefixUri;
    }

}
