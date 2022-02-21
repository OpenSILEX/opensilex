//******************************************************************************
//                          SectionGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;

import org.opensilex.mobile.dal.SectionModel;

/**
 * This class is the Data Transfer Object that is used when we get formSections.
 *
 * @author Maximilian Hart
 */
public class SectionGetDTO extends SectionUpdateDTO {
    
    public static SectionGetDTO fromModel(SectionModel model) {
        SectionGetDTO sectionGetDTO = new SectionGetDTO();
        sectionGetDTO.setCreationDate(model.getCreationDate().toString());
        sectionGetDTO.setUpdateDate(model.getLastUpdateDate().toString());
        sectionGetDTO.setSectionData(model.getSectionData());
        sectionGetDTO.setTimezone(model.getOffset());
        sectionGetDTO.setUri( model.getUri());
        sectionGetDTO.setCommitAddress(model.getCommitAddress());
        sectionGetDTO.setName(model.getName());
        return sectionGetDTO;
    }
}
