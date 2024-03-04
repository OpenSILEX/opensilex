package org.opensilex.faidare.builder;

import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.faidare.model.Faidarev1GermplasmDTO;
import org.opensilex.security.account.dal.AccountModel;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Faidarev1GermplasmDTOBuilder {

    private final GermplasmDAO germplasmDAO;

    public Faidarev1GermplasmDTOBuilder(GermplasmDAO germplasmDAO) {
        this.germplasmDAO = germplasmDAO;
    }

    public Faidarev1GermplasmDTO fromModel(GermplasmModel model, AccountModel accountModel) throws Exception {
        Faidarev1GermplasmDTO dto = new Faidarev1GermplasmDTO();
        dto.setGermplasmDbId(model.getUri().toString())
                .setGermplasmPUI(model.getUri().toString())
                .setGermplasmName(model.getLabel().toString())
                .setDefaultDisplayName(model.getLabel().toString())
                .setAccessionNumber(model.getCode())
                .setSubtaxa(
                        model.getVariety() != null ? "var. " + model.getVariety().getLabel() : null //TODO : change this to latin label when Multilabels are added because the name isn't necessarily in latin
                )
                .setInstituteCode(model.getInstitute())
                .setDocumentationURL(Objects.toString(model.getWebsite(), null))
                .setSynonyms(model.getSynonyms())
                .setSpecies(model.getSpecies().getName());

        List<String> studiesUri = germplasmDAO.getExpFromGermplasm(accountModel, model.getUri(), null, null, null, null)
                .getList()
                .stream()
                .map(experimentModel -> experimentModel.getUri().toString())
                .collect(Collectors.toList());
        if (!studiesUri.isEmpty()) {
            dto.setStudyDbId(studiesUri);
        }

        return dto;
    }
}
