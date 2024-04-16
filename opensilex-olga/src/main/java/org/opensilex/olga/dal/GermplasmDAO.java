package org.opensilex.olga.dal;

import java.util.Collections;
import java.util.List;

public class GermplasmDAO {

    public void updateGermplasms(List<GermplasmModel> germplasmModels) {
        // update olga germplasms in mongo here
    }

    public List<GermplasmModel> searchGermplasms(String germplasmName) {
        // search by name but could be later extended
        return Collections.singletonList(new GermplasmModel());
    }

    // add index on germplasmName/germplasmDbId
}
