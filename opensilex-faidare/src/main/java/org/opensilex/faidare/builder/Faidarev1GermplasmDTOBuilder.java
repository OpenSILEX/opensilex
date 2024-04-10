package org.opensilex.faidare.builder;

import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.faidare.model.Faidarev1GermplasmDTO;
import org.opensilex.faidare.model.Faidarev1GermplasmModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.util.Objects;
import java.util.stream.Collectors;

public class Faidarev1GermplasmDTOBuilder {

    public Faidarev1GermplasmDTOBuilder() {

    }

    public Faidarev1GermplasmDTO fromModel(Faidarev1GermplasmModel model) {
        Faidarev1GermplasmDTO dto = new Faidarev1GermplasmDTO();
        dto.setGermplasmDbId(SPARQLDeserializers.getExpandedURI(model.getUri()))
                .setGermplasmPUI(SPARQLDeserializers.getExpandedURI(model.getUri()))
                .setGermplasmName(model.getLabel().toString())
                .setDefaultDisplayName(model.getLabel().toString())
                .setAccessionNumber(model.getCode())
                .setSubtaxa(
                        model.getVarietyName() != null ? "var. " + model.getVarietyName() : null //TODO : change this to latin label when Multilabels are added because the name isn't necessarily in latin
                )
                .setInstituteCode(model.getInstitute())
                .setDocumentationURL(Objects.toString(model.getWebsite(), null))
                .setSpecies(model.getSpecies() != null ? SPARQLDeserializers.getExpandedURI(model.getSpecies()) : null)
                .setStudyDbId(model.getExperiments().stream().map(SPARQLDeserializers::getExpandedURI).collect(Collectors.toList()))
                .setInstituteCode(model.getInstitute())
                .setAccessionNumber(model.getCode());

        return dto;
    }
}
