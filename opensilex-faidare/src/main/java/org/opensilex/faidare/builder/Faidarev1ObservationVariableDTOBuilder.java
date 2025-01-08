/*
 * *****************************************************************************
 *                         Faidarev1ObservationVariableDTOBuilder.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 06/06/2024 17:14
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.faidare.builder;

import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.faidare.model.Faidarev1ObservationVariableDTO;
import org.opensilex.faidare.model.Faidarev1TraitDTO;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

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
        dto.setObservationVariableDbId(SPARQLDeserializers.getExpandedURI(variableModel.getUri()))
                .setName(variableModel.getName())
                .setDate(Objects.toString(variableModel.getPublicationDate(), null))
                .setMethod(methodDTOBuilder.fromModel(variableModel.getMethod()))
                .setScale(scaleDTOBuilder.fromModel(variableModel.getUnit(), variableModel.getDataType()))
                .setCrop(
                        Optional.ofNullable(variableModel.getSpecies())
                                .filter(list -> list.size() == 1)
                                .map(list -> list.get(0).getName())
                                .orElse(null)
                )
                .setSynonyms(new ArrayList<>(Collections.singletonList(variableModel.getAlternativeName())))
                .setXref(
                        Optional.ofNullable(variableModel.getExactMatch())
                                .filter(list -> list.size() == 1)
                                .map(list -> SPARQLDeserializers.getExpandedURI(list.get(0)))
                                .orElse(null)
                );

        Faidarev1TraitDTO traitDTO = new Faidarev1TraitDTO()
                .setName(
                        Optional.ofNullable(variableModel.getTraitName())
                                .orElse(String.format("%s_%s",
                                        variableModel.getEntity().getName(),
                                        variableModel.getCharacteristic().getName())
                                )
                )
                .setAttribute(Objects.toString(variableModel.getCharacteristic().getName(), null))
                .setEntity(Objects.toString(variableModel.getEntity().getName(), null));
        if (Objects.nonNull(variableModel.getTraitUri())) {
            traitDTO.setTraitDbId(SPARQLDeserializers.getExpandedURI(variableModel.getTraitUri()));
        }
        dto.setTrait(traitDTO);

        return dto;
    }
}
