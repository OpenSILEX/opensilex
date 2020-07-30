/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OwlRestrictionModel;
import org.opensilex.core.ontology.dal.PropertyModel;

/**
 *
 * @author vmigot
 */
public class RDFClassDTO {

    private URI uri;

    private String label;

    private String comment;

    private URI parent;

    protected List<RDFClassPropertyDTO> properties;

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

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public List<RDFClassPropertyDTO> getProperties() {
        return properties;
    }

    public void setProperties(List<RDFClassPropertyDTO> properties) {
        this.properties = properties;
    }

    public static RDFClassDTO fromModel(RDFClassDTO dto, ClassModel<?> model) {
        dto.setUri(model.getUri());
        dto.setLabel(model.getName());
        if (model.getComment() != null) {
            dto.setComment(model.getComment().getDefaultValue());
        }

        if (model.getParent() != null) {
            dto.setParent(model.getParent().getUri());
        }

        List<RDFClassPropertyDTO> properties = new ArrayList<>();

        for (OwlRestrictionModel restriction : model.getRestrictions().values()) {
            URI propertyURI = restriction.getOnProperty();
            if (model.isDatatypePropertyRestriction(propertyURI)) {
                PropertyModel property = model.getDatatypeProperty(propertyURI);
                properties.add(RDFClassPropertyDTO.fromModel(property, restriction, true));
            } else if (model.isObjectPropertyRestriction(propertyURI)) {
                PropertyModel property = model.getObjectProperty(propertyURI);
                properties.add(RDFClassPropertyDTO.fromModel(property, restriction, false));
            }
        }

        dto.setProperties(properties);

        return dto;
    }
}
