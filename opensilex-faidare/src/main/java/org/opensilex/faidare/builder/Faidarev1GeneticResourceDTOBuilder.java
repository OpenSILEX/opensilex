package org.opensilex.faidare.builder;

import org.opensilex.core.geneticResource.dal.GeneticResourceDAO;
import org.opensilex.faidare.model.Faidarev1GeneticResourceDTO;
import org.opensilex.faidare.model.Faidarev1GeneticResourceModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.util.Objects;
import java.util.stream.Collectors;

public class Faidarev1GeneticResourceDTOBuilder {

    public Faidarev1GeneticResourceDTOBuilder() {

    }

    public Faidarev1GeneticResourceDTO fromModel(Faidarev1GeneticResourceModel model) {
        Faidarev1GeneticResourceDTO dto = new Faidarev1GeneticResourceDTO();
        dto.setGeneticResourceDbId(SPARQLDeserializers.getExpandedURI(model.getUri()))
                .setGeneticResourcePUI(SPARQLDeserializers.getExpandedURI(model.getUri()))
                .setGeneticResourceName(model.getLabel().toString())
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
