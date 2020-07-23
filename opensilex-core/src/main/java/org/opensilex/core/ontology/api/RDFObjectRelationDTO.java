/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import java.net.URI;
import java.net.URISyntaxException;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vmigot
 */
public class RDFObjectRelationDTO {

    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RDFObjectRelationDTO.class);
    private URI property;

    private String value;

    public URI getProperty() {
        return property;
    }

    public void setProperty(URI property) {
        this.property = property;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static RDFObjectRelationDTO getDTOFromModel(SPARQLModelRelation relation) {
        RDFObjectRelationDTO dto = new RDFObjectRelationDTO();

        try {
            dto.setProperty(new URI(relation.getProperty().getURI()));
        } catch (URISyntaxException ex) {
            LOGGER.error("Invalid object relation property URI (should never happend) : " + relation.getProperty(), ex);
        }
        dto.setValue(relation.getValue());

        return dto;
    }

}
