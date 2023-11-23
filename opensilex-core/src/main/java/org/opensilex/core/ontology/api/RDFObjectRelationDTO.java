/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import java.net.URI;
import java.net.URISyntaxException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.utils.Ontology;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vmigot
 */
public class RDFObjectRelationDTO {

    public RDFObjectRelationDTO(){}

    public RDFObjectRelationDTO(URI property, String value, boolean isInverse){
        this.property = property;
        this.value = value;
        this.isInverse = isInverse;
    }

    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RDFObjectRelationDTO.class);
    private URI property;

    private String value;

    private boolean isInverse;

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

    public boolean isInverse() {
        return isInverse;
    }

    public void setInverse(boolean inverse) {
        isInverse = inverse;
    }

    public static RDFObjectRelationDTO getDTOFromModel(SPARQLModelRelation relation) {
        RDFObjectRelationDTO dto = new RDFObjectRelationDTO();

        try {
            dto.setProperty(SPARQLDeserializers.formatURI(new URI(relation.getProperty().getURI())));
        } catch (URISyntaxException ex) {
            LOGGER.error("Invalid object relation property URI (should never happend) : " + relation.getProperty(), ex);
        }

        if (URIDeserializer.validateURI(relation.getValue())) {
            try {
                dto.setValue(SPARQLDeserializers.formatURI(new URI(relation.getValue())).toString());
            } catch (URISyntaxException ex) {
                LOGGER.error("URI considered valid but throw exception (should never happend) : " + relation.getProperty() + " - " + relation.getValue(), ex);
            }
        } else {
            dto.setValue(relation.getValue());
        }

        dto.setInverse(relation.getReverse());

        return dto;
    }

    public SPARQLModelRelation toModel(){
        SPARQLModelRelation relation = new SPARQLModelRelation();
        relation.setValue(value);
        relation.setProperty(Ontology.property(property));
        return relation;
    }



}
