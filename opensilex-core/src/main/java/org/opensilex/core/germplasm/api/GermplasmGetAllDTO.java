/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.germplasm.api;

import java.net.URI;
import org.opensilex.core.germplasm.dal.GermplasmModel;

/**
 *
 * @author boizetal
 */
public class GermplasmGetAllDTO {

    /**
     * Germplasm URI
     */
    protected URI uri;

    /**
     * Germplasm Type : Species, Variety, Accession or subclass of PlantMaterialLot
     */
    protected URI type;

    /**
     * typeLabel
     */
    protected String typeLabel;

    /**
     * Germplasm label
     */
    protected String name;

    /**
     * Germplasm species URI
     */
    protected URI species;

    /**
     * speciesLabel
     */
    protected String speciesLabel;

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

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getSpecies() {
        return species;
    }

    public void setSpecies(URI species) {
        this.species = species;
    }

    public String getSpeciesLabel() {
        return speciesLabel;
    }

    public void setSpeciesLabel(String speciesLabel) {
        this.speciesLabel = speciesLabel;
    }

    /**
     * Convert Germplasm Model into Germplasm DTO
     *
     * @param model Germplasm Model to convert
     * @return Corresponding user DTO
     */
    public static GermplasmGetAllDTO fromModel(GermplasmModel model) {
        GermplasmGetAllDTO dto = new GermplasmGetAllDTO();

        dto.setUri(model.getUri());
        dto.setType(model.getType());
        dto.setTypeLabel(model.getTypeLabel().getDefaultValue());
        dto.setName(model.getName());

        if (model.getSpecies() != null) {
            dto.setSpecies(model.getSpecies().getUri());
            try {
                dto.setSpeciesLabel(model.getSpecies().getName());
            } catch (Exception e) {
            }
        }

        return dto;
    }

}
