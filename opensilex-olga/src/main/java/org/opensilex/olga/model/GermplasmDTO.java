package org.opensilex.olga.model;

public class GermplasmDTO {

    private String germplasmDbId;
    private String germplasmName;

    public String getGermplasmDbId() {
        return germplasmDbId;
    }

    public GermplasmDTO setGermplasmDbId(String germplasmDbId) {
        this.germplasmDbId = germplasmDbId;
        return this;
    }

    public String getGermplasmName() {
        return germplasmName;
    }

    public GermplasmDTO setGermplasmName(String germplasmName) {
        this.germplasmName = germplasmName;
        return this;
    }
}
