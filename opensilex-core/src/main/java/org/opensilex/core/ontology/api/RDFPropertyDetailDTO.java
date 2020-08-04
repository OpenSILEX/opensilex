/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.PropertyModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;

/**
 *
 * @author vmigot
 */
public class RDFPropertyDetailDTO {

    protected URI uri;

    protected URI type;

    protected String label;

    protected String comment;

    protected URI domain;

    protected URI range;
    
    protected String rangeLabel;

    protected URI parent;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public URI getDomain() {
        return domain;
    }

    public void setDomain(URI domain) {
        this.domain = domain;
    }

    public URI getRange() {
        return range;
    }

    public void setRange(URI range) {
        this.range = range;
    }

    public String getRangeLabel() {
        return rangeLabel;
    }

    public void setRangeLabel(String rangeLabel) {
        this.rangeLabel = rangeLabel;
    }

    
    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public static RDFPropertyDetailDTO fromModel(PropertyModel model, PropertyModel parentModel) {
        RDFPropertyDetailDTO dto = new RDFPropertyDetailDTO();

        dto.setUri(model.getUri());
        dto.setType(model.getType());
        if (parentModel != null) {
            dto.setParent(parentModel.getUri());
        }
        dto.setLabel(model.getLabel().getDefaultValue());
        if (model.getComment() != null) {
            dto.setComment(model.getComment().getDefaultValue());
        }

        if (model.getDomain() != null) {
            dto.setDomain(model.getDomain().getUri());
        }

        return dto;
    }

}
