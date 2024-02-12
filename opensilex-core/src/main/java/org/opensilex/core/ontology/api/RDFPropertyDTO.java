/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.ontology.dal.*;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author vmigot
 */
public class RDFPropertyDTO {

    @ApiModelProperty(
            value = "URI of property",
            required = true,
            example = "http://opensilex.org/custom_object_property")
    protected URI uri;

    @JsonProperty("rdf_type")
    @ApiModelProperty(
            value = "The type of property",
            notes = "Allowed values are owl:DatatypeProperty (for data-property) or owl:ObjectProperty (for object-property)",
            required = true,
            example = "owl:ObjectProperty"
    )
    protected URI type;

    @JsonProperty("name_translations")
    @ApiModelProperty(
            value = "Name by languages, at least one name/language is required. Use '' as language if no language is specified",
            required = true
    )
    protected Map<String, String> labelTranslations;

    @JsonProperty("comment_translations")
    @ApiModelProperty(
            value = "Description by languages, at least one description/language is required. Use '' as language if no language is specified",
            required = true
    )
    protected Map<String, String> commentTranslations;

    @ApiModelProperty(
            value = "Domain of the property : the rdf:type of any concept concerned by this property.",
            required = true,
            notes = "This domain definition must exist into the repository",
            example = "vocabulary:SensingDevice"
    )
    protected URI domain;

    @ApiModelProperty(
            value = "Range of the property : the rdf:type of any value(can be a literal type or a concept type) concerned by this property.",
            required = true,
            notes = "The range definition(class or literal type) must exist into the repository",
            example = "vocabulary:ScientificObject"
    )
    protected URI range;

    @ApiModelProperty(
            value = "Parent of the property.",
            notes = "The parent definition as a property must exist into the repository",
            example = "http://opensilex.org/parent_custom_object_property"
    )
    protected URI parent;

    @JsonProperty("publisher")
    protected UserGetDTO publisher;

    @JsonProperty("publication_date")
    protected OffsetDateTime publicationDate;

    @JsonProperty("last_updated_date")
    protected OffsetDateTime lastUpdatedDate;

    @ValidURI
    @NotNull
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ValidURI
    @NotNull
    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
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

    @ValidURI
    @NotNull
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

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    @JsonIgnore
    public boolean isDataProperty() {
        return RDFPropertyDTO.isDataProperty(getType());
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

    public RDFPropertyDTO() {

    }

    public RDFPropertyDTO(AbstractPropertyModel<?> model) {

        setUri(model.getUri());
        setType(model.getType());

        if (Objects.nonNull(model.getPublicationDate())) {
            setPublicationDate(model.getPublicationDate());
        }

        if (Objects.nonNull(model.getLastUpdateDate())) {
            setLastUpdatedDate(model.getLastUpdateDate());
        }

        if (model.getParent() != null) {
            setParent(model.getParent().getUri());
        }
        setLabelTranslations(model.getLabel().getAllTranslations());
        if (model.getComment() != null) {
            setCommentTranslations(model.getComment().getAllTranslations());
        } else {
            setCommentTranslations(Collections.emptyMap());
        }

        if (model.getDomain() != null) {
            setDomain(model.getDomain().getUri());
        }

        if(model instanceof DatatypePropertyModel){
            setRange(((DatatypePropertyModel) model).getRange());
        }else if(model instanceof ObjectPropertyModel){
            if(((ObjectPropertyModel) model).getRange() != null) {
                ObjectPropertyModel objectProperty = (ObjectPropertyModel) model;
                setRange(objectProperty.getRange().getUri());
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

    /**
     *
     * @param propertyType URI to check
     * @return true if {@code propertyType} is equals to {@code owl:DatatypeProperty} IRI (by handling prefix)
     */
    public static boolean isDataProperty(URI propertyType) {
        return SPARQLDeserializers.compareURIs(propertyType.toString(), OWL2.DatatypeProperty.getURI());
    }

    /**
     *
     * @param propertyType URI to check
     * @return true if {@code propertyType} is equals to {@code owl:ObjectProperty} IRI (by handling prefix)
     */
    public static boolean isObjectProperty(URI propertyType){
        return SPARQLDeserializers.compareURIs(propertyType.toString(), OWL2.ObjectProperty.getURI());
    }

}
