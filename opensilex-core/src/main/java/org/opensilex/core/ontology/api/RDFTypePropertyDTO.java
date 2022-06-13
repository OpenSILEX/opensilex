/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.ontology.dal.*;

import java.net.URI;
import java.util.Map;

/**
 *
 * @author vince
 */
public class RDFTypePropertyDTO {

    protected URI uri;

    protected String name;

    protected String comment;

    @JsonProperty("name_translations")
    protected Map<String, String> labelTranslations;

    @JsonProperty("comment_translations")
    protected Map<String, String> commentTranslations;

    protected URI parent;

    protected boolean literal;

    protected boolean list;

    protected boolean required;

    @JsonProperty("type_restriction")
    protected URI typeRestriction;

    @JsonProperty("min_cardinality")
    protected Integer minCardinality;

    @JsonProperty("max_cardinality")
    protected Integer maxCardinality;

    protected Integer cardinality;

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

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public boolean isLiteral() {
        return literal;
    }

    public void setLiteral(boolean literal) {
        this.literal = literal;
    }

    public boolean isList() {
        return list;
    }

    public void setList(boolean list) {
        this.list = list;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public URI getTypeRestriction() {
        return typeRestriction;
    }

    public void setTypeRestriction(URI typeRestriction) {
        this.typeRestriction = typeRestriction;
    }

    public Integer getMinCardinality() {
        return minCardinality;
    }

    public void setMinCardinality(Integer minCardinality) {
        this.minCardinality = minCardinality;
    }

    public Integer getMaxCardinality() {
        return maxCardinality;
    }

    public void setMaxCardinality(Integer maxCardinality) {
        this.maxCardinality = maxCardinality;
    }

    public Integer getCardinality() {
        return cardinality;
    }

    public void setCardinality(Integer cardinality) {
        this.cardinality = cardinality;
    }

    public static RDFTypePropertyDTO fromModel(AbstractPropertyModel<?> property, OwlRestrictionModel restriction, boolean isLiteral) {
        RDFTypePropertyDTO dto = new RDFTypePropertyDTO();
        dto.setUri(restriction.getOnProperty());
        dto.setName(property.getName());
        if (property.getComment() != null) {
            dto.setComment(property.getComment().getDefaultValue());
        }
        dto.setLiteral(isLiteral);
        dto.setList(restriction.isList());
        dto.setRequired(restriction.isRequired());

        dto.setTypeRestriction(restriction.getSubjectURI());

        dto.setMinCardinality(restriction.getMinQualifiedCardinality());
        dto.setMaxCardinality(restriction.getMaxQualifiedCardinality());
        dto.setCardinality(restriction.getQualifiedCardinality());

        return dto;
    }

    public DatatypePropertyModel getDatatypePropertyModel(String lang) {
        DatatypePropertyModel property = new DatatypePropertyModel();
        getPropertyModel(property, lang);
        DatatypePropertyModel parentModel = new DatatypePropertyModel();
        parentModel.setUri(getParent());
        property.setParent(parentModel);
        return property;
    }

    public ObjectPropertyModel getObjectPropertyModel(String lang) {
        ObjectPropertyModel property = new ObjectPropertyModel();
        getPropertyModel(property, lang);
        ObjectPropertyModel parentModel = new ObjectPropertyModel();
        parentModel.setUri(getParent());
        property.setParent(parentModel);
        return property;
    }

    @JsonIgnore
    public OwlRestrictionModel getOwlRestriction() {
        OwlRestrictionModel restriction = new OwlRestrictionModel();

        restriction.setOnProperty(getUri());

        if (isList()) {
            if (isRequired()) {
                restriction.setMinQualifiedCardinality(1);
            } else {
                restriction.setMinQualifiedCardinality(0);
            }
        } else {
            if (isRequired()) {
                restriction.setQualifiedCardinality(1);
            } else {
                restriction.setMaxQualifiedCardinality(1);
                restriction.setMinQualifiedCardinality(0);
            }
        }

        // Literal -> use owl:onDataRange (property used for owl:DataTypeProperty)
        if (isLiteral()) {
            restriction.setOnDataRange(getTypeRestriction());
        } else {
            // Object/Resource -> use owl:onClass (property used for owl:ObjectProperty)
            restriction.setOnClass(getTypeRestriction());
        }

        return restriction;
    }

    private void getPropertyModel(AbstractPropertyModel<?> property, String lang) {
        property.setUri(getUri());

        SPARQLLabel sparqlLabel = new SPARQLLabel(getName(), lang);
        sparqlLabel.addAllTranslations(getLabelTranslations());
        property.setLabel(sparqlLabel);

        SPARQLLabel sparqlComment = new SPARQLLabel(getComment(), lang);
        sparqlComment.addAllTranslations(getCommentTranslations());
        property.setComment(sparqlComment);
    }

}
