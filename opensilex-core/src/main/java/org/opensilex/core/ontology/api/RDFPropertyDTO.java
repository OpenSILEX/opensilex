/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jsonldjava.utils.Obj;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.ontology.dal.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * @author vmigot
 */
public class RDFPropertyDTO {

    protected URI uri;

    @JsonProperty("rdf_type")
    protected URI type;

    protected String name;

    protected String comment;

    @JsonProperty("name_translations")
    protected Map<String, String> labelTranslations;

    @JsonProperty("comment_translations")
    protected Map<String, String> commentTranslations;

    protected URI domain;

    @JsonProperty("domain_rdf_type")
    protected URI domainType;

    public URI getDomainType() {
        return domainType;
    }

    public void setDomainType(URI domainType) {
        this.domainType = domainType;
    }

    protected URI range;

    @JsonProperty("range_label")
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

    public Map<String, String> getLabelTranslations() {
        return labelTranslations;
    }

    public void setLabelTranslations(Map<String, String> labelTranslations) {
        this.labelTranslations = labelTranslations;
    }

    public Map<String, String> getCommentTranslations() {
        return commentTranslations;
    }

    public void setCommentTranslations(Map<String, String> commentTranslations) {
        this.commentTranslations = commentTranslations;
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

    @JsonIgnore
    public boolean isDataProperty() {
        return isDataProperty(getType());
    }

    public RDFPropertyDTO() {

    }

    public RDFPropertyDTO(AbstractPropertyModel<?> model) {

        setUri(model.getUri());
        setType(model.getType());

        if (model.getParent() != null) {
            setParent(model.getParent().getUri());
        }
        setName(model.getLabel().getDefaultValue());
        setLabelTranslations(model.getLabel().getAllTranslations());
        if (model.getComment() != null) {
            setComment(model.getComment().getDefaultValue());
            setCommentTranslations(model.getComment().getAllTranslations());
        } else {
            setCommentTranslations(new HashMap<>());
        }

        if (model.getDomain() != null) {
            setDomain(model.getDomain().getUri());
        }

        if(model instanceof DatatypePropertyModel){
            setRange(((DatatypePropertyModel) model).getRange());
        }else if(model instanceof ObjectPropertyModel){
            if(((ObjectPropertyModel) model).getRange() != null){
                setRange(((ObjectPropertyModel) model).getRange().getUri());
                setRangeLabel(((ObjectPropertyModel) model).getRange().getName());
            }
        }
    }

    public AbstractPropertyModel<?> toModel(AbstractPropertyModel<?> model) {

        model.setUri(getUri());
        model.setLabel(SPARQLLabel.fromMap(getLabelTranslations()));
        model.setComment(SPARQLLabel.fromMap(getCommentTranslations()));
        if (getDomain() != null) {
            ClassModel pDomain = new ClassModel();
            pDomain.setUri(getDomain());
            model.setDomain(pDomain);
        }

        return model;
    }

    public static boolean isDataProperty(URI propertyType) {
        return SPARQLDeserializers.compareURIs(propertyType.toString(), OWL2.DatatypeProperty.getURI());
    }
}
