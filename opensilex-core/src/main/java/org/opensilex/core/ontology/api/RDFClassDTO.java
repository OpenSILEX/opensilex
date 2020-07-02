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
import org.opensilex.core.ontology.dal.DatatypePropertyModel;
import org.opensilex.core.ontology.dal.ObjectPropertyModel;
import org.opensilex.core.ontology.dal.OwlRestrictionModel;

/**
 *
 * @author vmigot
 */
public class RDFClassDTO {

    private URI uri;

    private String label;

    private String comment;

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

    public List<RDFClassPropertyDTO> getProperties() {
        return properties;
    }

    public void setProperties(List<RDFClassPropertyDTO> properties) {
        this.properties = properties;
    }

    public static RDFClassDTO fromModel(ClassModel model) {
        RDFClassDTO dto = new RDFClassDTO();

        dto.setUri(model.getUri());
        dto.setLabel(model.getName());
        if (model.getComment() != null) {
            dto.setComment(model.getComment().getDefaultValue());
        }

        List<RDFClassPropertyDTO> properties = new ArrayList<>();
        for (OwlRestrictionModel restriction : model.getRestrictions()) {
            if (restriction.isDatatypePropertyRestriction()) {
                DatatypePropertyModel property = model.getDatatypeProperties().get(restriction.getOnProperty());
                properties.add(RDFClassPropertyDTO.fromModel(property, restriction));
            } else if (restriction.isObjectPropertyRestriction()) {
                ObjectPropertyModel property = model.getObjectProperties().get(restriction.getOnProperty());
                properties.add(RDFClassPropertyDTO.fromModel(property, restriction));
            }
        }

        dto.setProperties(properties);

        return dto;
    }
}
