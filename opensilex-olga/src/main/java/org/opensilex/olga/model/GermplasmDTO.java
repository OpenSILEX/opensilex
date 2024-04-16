package org.opensilex.olga.model;

import org.brapi.v2.model.germ.BrAPIGermplasm;

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

    public static GermplasmDTO fromBrapiGermplasm(BrAPIGermplasm brAPIGermplasm) {
        return new GermplasmDTO()
                .setGermplasmDbId(brAPIGermplasm.getGermplasmDbId())
                .setGermplasmName(brAPIGermplasm.getGermplasmName());
    }
}
