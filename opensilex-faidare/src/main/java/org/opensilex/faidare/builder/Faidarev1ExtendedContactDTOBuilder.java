package org.opensilex.faidare.builder;

import org.opensilex.faidare.model.Faidarev1ExtendedContactDTO;
import org.opensilex.security.person.dal.PersonModel;

import java.util.Objects;

public class Faidarev1ExtendedContactDTOBuilder {

    public static Faidarev1ExtendedContactDTO fromModel(PersonModel personModel){
        Faidarev1ExtendedContactDTO dto = new Faidarev1ExtendedContactDTO();
        dto.setContactDbId(Objects.toString(personModel.getUri(), null))
                .setEmail(Objects.toString(personModel.getEmail(), null))
                .setName(personModel.getFirstName() + " " + personModel.getLastName())
                .setOrcid(Objects.toString(personModel.getOrcid(), null))
                .setInstituteName(personModel.getAffiliation());
        return dto;
    }
}
