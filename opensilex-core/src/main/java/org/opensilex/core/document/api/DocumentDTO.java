//******************************************************************************
//                          DocumentDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.document.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.opensilex.server.rest.validation.Required;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * @author Fernandez Emilie 
 */

@JsonPropertyOrder({"uri", "identifier", "rdf_type", "rdf_type_name", "title", "date", "description", "targets", "authors", "language", "format", "keywords", "deprecated", "source" })
public abstract class DocumentDTO {

    protected URI uri;

    protected String identifier;

    @JsonProperty("rdf_type")
    protected URI type;

    @JsonProperty("rdf_type_name")
    protected String typeLabel;

    protected String title;

    protected String date;

    protected List<URI> targets = new ArrayList<>();

    protected List<String> authors = new ArrayList<>();
    
    protected String language;

    protected String format;

    protected String description;

    protected URI source;

    @JsonProperty("keywords")
    protected List<String> subject = new ArrayList<>();
    
    protected boolean deprecated;  
    
    @ApiModelProperty(example = "http://opensilex.dev/set/documents#ProtocolExperimental")
    public URI getUri() {
        return uri;
    }

    public DocumentDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    @ApiModelProperty(example = "doi:10.1340/309registries")
    public String getIdentifier() {
        return identifier;
    }

    public DocumentDTO setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    @ApiModelProperty(example = "http://www.opensilex.org/vocabulary/oeso#ScientificDocument")
    public URI getType() {
        return type;
    }

    public DocumentDTO setType(URI type) {
        this.type = type;
        return this;
    }


    public String getTypeLabel() {
        return typeLabel;
    }

    public DocumentDTO setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
        return this;
    }

    @Required
    @ApiModelProperty(example = "title")
    public String getTitle() {
        return title;
    }

    public DocumentDTO setTitle(String title) {
        this.title = title;
        return this;
    }

    @ApiModelProperty(example = "2020-06-01")
    public String getDate() {
        return date;
    }

    public DocumentDTO setDate(String date) {
        this.date = date;
        return this;
    }

    @ApiModelProperty(example = "http://opensilex.dev/opensilex/id/variables/v001")
    public List<URI> getTargets() {
        return targets;
    }

    public DocumentDTO setTargets(List<URI> targets) {
        this.targets = targets;
        return this;
    }

    @ApiModelProperty(example = "Author name")
    public List<String> getAuthors() {
        return authors;
    }

    public DocumentDTO setAuthors(List<String> authors) {
        this.authors = authors;
        return this;
    }

    @ApiModelProperty(example = "fr")
    public String getLanguage() {
        return language;
    }

    public DocumentDTO setLanguage(String language) {
        this.language = language;
        return this;
    }
    
    @ApiModelProperty(example = "jpg")
    public String getFormat() {
        return format;
    }

    public DocumentDTO setFormat(String format) {
        this.format = format;
        return this;
    }

    @ApiModelProperty(example = "description")
    public String getDescription() {
        return description;
    }

    public DocumentDTO setDescription(String description) {
        this.description = description;
        return this;
    }

    @ApiModelProperty(example = "keyword")
    public List<String> getSubject() {
        return subject;
    }

    public DocumentDTO setSubject(List<String> subject) {
        this.subject = subject;
        return this;
    }

    @ApiModelProperty(example = "false")
    public boolean getDeprecated() {
        return deprecated;
    }

    public DocumentDTO setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    public URI getSource() {
        return source;
    }

    public DocumentDTO setSource(URI source) {
        this.source = source;
        return this;
    }

    @JsonIgnore
    public boolean hasFile() {
        return source != null;
    }
}
