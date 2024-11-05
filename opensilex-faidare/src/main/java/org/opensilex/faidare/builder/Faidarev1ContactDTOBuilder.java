package org.opensilex.faidare.builder;

import org.opensilex.faidare.model.Faidarev1ContactDTO;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.util.Objects;

public class Faidarev1ContactDTOBuilder {

    public Faidarev1ContactDTOBuilder() {
    }

    public Faidarev1ContactDTO fromModel(PersonModel personModel, String role) {
        Faidarev1ContactDTO dto = new Faidarev1ContactDTO();

        dto.setEmail(Objects.toString(personModel.getEmail(), null))
                .setContactDbId(SPARQLDeserializers.getExpandedURI(personModel.getUri()))
                .setName(
                        personModel.getLastName().toUpperCase()
                                + " "
                                + personModel.getFirstName().substring(0,1).toUpperCase()
                                + personModel.getFirstName().substring(1)
                )
                .setOrcid(Objects.toString(personModel.getOrcid(), null))
                .setType(role)
                .setInstitutionName(personModel.getAffiliation());
        return dto;
    }
}
