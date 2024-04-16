package org.opensilex.olga.dal;

import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.olga.model.GermplasmDTO;

public class GermplasmModel extends MongoModel {

    private String germplasmDbId;
    private String germplasmName;

    public String getGermplasmDbId() {
        return germplasmDbId;
    }

    public GermplasmModel setGermplasmDbId(String germplasmDbId) {
        this.germplasmDbId = germplasmDbId;
        return this;
    }

    public String getGermplasmName() {
        return germplasmName;
    }

    public GermplasmModel setGermplasmName(String germplasmName) {
        this.germplasmName = germplasmName;
        return this;
    }

    public static GermplasmModel fromDTO(GermplasmDTO germplasmDTO) {
        return new GermplasmModel()
                .setGermplasmDbId(germplasmDTO.getGermplasmDbId())
                .setGermplasmName(germplasmDTO.getGermplasmName());
    }

    public static GermplasmDTO toDTO(GermplasmModel germplasmModel) {
        return new GermplasmDTO()
                .setGermplasmDbId(germplasmModel.getGermplasmDbId())
                .setGermplasmName(germplasmModel.getGermplasmName());
    }
}
