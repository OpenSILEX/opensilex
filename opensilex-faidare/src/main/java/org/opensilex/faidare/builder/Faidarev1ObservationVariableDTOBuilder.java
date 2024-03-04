package org.opensilex.faidare.builder;

import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.faidare.model.Faidarev1ObservationVariableDTO;
import org.opensilex.faidare.model.Faidarev1TraitDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

public class Faidarev1ObservationVariableDTOBuilder {

    public Faidarev1ObservationVariableDTOBuilder() {
    }

    public Faidarev1ObservationVariableDTO fromModel(VariableModel variableModel) {
        Faidarev1ObservationVariableDTO dto = new Faidarev1ObservationVariableDTO();

        Faidarev1MethodDTOBuilder methodDTOBuilder = new Faidarev1MethodDTOBuilder();
        Faidarev1ScaleDTOBuilder scaleDTOBuilder = new Faidarev1ScaleDTOBuilder();
        dto.setObservationVariableDbId(variableModel.getUri().toString())
                .setName(variableModel.getName())
                .setDate(Objects.toString(variableModel.getPublicationDate(), null))
                .setTrait(
                        new Faidarev1TraitDTO()
                                .setName(
                                        Optional.ofNullable(variableModel.getTraitName())
                                                .orElse(String.format("%s_%s",
                                                        variableModel.getEntity().getName(),
                                                        variableModel.getCharacteristic().getName())
                                                )
                                )
                                .setTraitDbId(Objects.toString(variableModel.getTraitUri(), null))
                                .setAttribute(Objects.toString(variableModel.getCharacteristic().getName(), null))
                                .setEntity(Objects.toString(variableModel.getEntity().getName(), null))
                )
                .setMethod(methodDTOBuilder.fromModel(variableModel.getMethod()))
                .setScale(scaleDTOBuilder.fromModel(variableModel.getUnit(), variableModel.getDataType().toString()))
                .setCrop(
                        Optional.ofNullable(variableModel.getSpecies())
                                .filter(list -> list.size() == 1)
                                .map(list -> list.get(0).toString())
                                .orElse(null)
                )
                .setSynonyms(new ArrayList<>(Collections.singletonList(variableModel.getAlternativeName())))
                .setXref(
                        Optional.ofNullable(variableModel.getExactMatch())
                                .filter(list -> list.size() == 1)
                                .map(list -> list.get(0).toString())
                                .orElse(null)
                );

        return dto;
    }
}
