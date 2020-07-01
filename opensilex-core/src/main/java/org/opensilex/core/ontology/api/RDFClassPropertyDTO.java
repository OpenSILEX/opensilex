/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import java.net.URI;
import org.opensilex.core.ontology.dal.OwlRestrictionModel;
import org.opensilex.core.ontology.dal.PropertyModel;

/**
 *
 * @author vince
 */
public class RDFClassPropertyDTO {

    URI uri;

    String label;

    String comment;

    boolean literal;

    URI typeRestriction;

    Integer minCardinality;

    Integer maxCardinality;

    Integer cardinality;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
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

    public boolean isLiteral() {
        return literal;
    }

    public void setLiteral(boolean literal) {
        this.literal = literal;
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

    public static RDFClassPropertyDTO fromModel(PropertyModel property, OwlRestrictionModel restriction) {
        RDFClassPropertyDTO dto = new RDFClassPropertyDTO();
        dto.setUri(property.getUri());
        dto.setLabel(property.getName());
        if (property.getComment() != null) {
            dto.setComment(property.getComment().getDefaultValue());
        }
        dto.setLiteral(restriction.isDatatypePropertyRestriction());

        if (dto.isLiteral()) {
            dto.setTypeRestriction(restriction.getOnDataRange());
        } else {
            dto.setTypeRestriction(restriction.getOnClass());
        }

        dto.setMinCardinality(restriction.getMinCardinality());
        dto.setMaxCardinality(restriction.getMaxCardinality());
        dto.setCardinality(restriction.getCardinality());

        return dto;
    }
}
