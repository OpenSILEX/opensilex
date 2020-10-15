//******************************************************************************
//                          DocumentDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.document.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.document.dal.DocumentModel;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.Required;
import java.io.File;
import java.nio.file.Path;
import org.opensilex.fs.service.FileStorageService;

/**
 * @author Fernandez Emilie A basic DTO class about an {@link DocumentModel}
 */
public abstract class DocumentDTO {

    protected URI uri;

    protected URI type;

    protected URI concerns;

    protected URI creator;
    
    protected String language;

    protected String name;

    protected String date;

    protected String format;

    protected String comment;

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

    @ApiModelProperty(example = "http://www.opensilex.org/vocabulary/oeso#ScientificDocument")
    public URI getType() {
        return type;
    }

    public DocumentDTO setType(URI type) {
        this.type = type;
        return this;
    }

    @ApiModelProperty(example = "http://opensilex.dev/opensilex/id/variables/v001")
    public URI getConcerns() {
        return concerns;
    }

    public DocumentDTO setConcerns(URI concerns) {
        this.concerns = concerns;
        return this;
    }

    @ApiModelProperty(example = "http://opensilex.dev/users#emilie")
    public URI getCreator() {
        return creator;
    }

    public DocumentDTO setCreator(URI creator) {
        this.creator = creator;
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
    
    @Required
    @ApiModelProperty(example = "title")
    public String getName() {
        return name;
    }

    public DocumentDTO setName(String name) {
        this.name = name;
        return this;
    }

    @Required
    @ApiModelProperty(example = "2020-06-01")
    public String getDate() {
        return date;
    }

    public DocumentDTO setDate(String date) {
        this.date = date;
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

    @ApiModelProperty(example = "Comment")
    public String getComment() {
        return comment;
    }

    public DocumentDTO setComment(String comment) {
        this.comment = comment;
        return this;
    }

    @ApiModelProperty(example = "keywords")
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
}
