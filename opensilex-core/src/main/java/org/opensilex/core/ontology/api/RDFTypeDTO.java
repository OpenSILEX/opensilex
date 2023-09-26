/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.sparql.ontology.dal.ClassModel;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author vmigot
 */
public class RDFTypeDTO {

    protected URI uri;

    protected String name;

    protected String comment;

    protected URI parent;

    protected UserGetDTO publisher;

    protected OffsetDateTime publicationDate;

    protected OffsetDateTime lastUpdatedDate;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public OffsetDateTime getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(OffsetDateTime publicationDate) {
        this.publicationDate = publicationDate;
    }

    public OffsetDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(OffsetDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public RDFTypeDTO() {

    }

    public RDFTypeDTO(ClassModel model) {
        setUri(model.getUri());

        if (model.getLabel() != null) {
            setName(model.getLabel().getDefaultValue());
        }
        if (model.getComment() != null) {
            setComment(model.getComment().getDefaultValue());
        }

        if (model.getParent() != null) {
            setParent(model.getParent().getUri());
        }

        if (Objects.nonNull(model.getPublicationDate())) {
            setPublicationDate(model.getPublicationDate());
        }
        if (Objects.nonNull(model.getLastUpdateDate())) {
            setLastUpdatedDate(model.getLastUpdateDate());
        }
    }

}
