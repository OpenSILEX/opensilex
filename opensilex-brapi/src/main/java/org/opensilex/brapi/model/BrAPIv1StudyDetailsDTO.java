//******************************************************************************
//                          BrAPIv1StudyDetailsDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.experiment.dal.ExperimentModel;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1StudyDetailsDTO extends BrAPIv1StudyDTO {
    private List<BrAPIv1ContactDTO> contacts;
    private List<BrAPIv1DataLinkDTO> datalinks;
    private BrAPIv1LocationDTO location;

    public List<BrAPIv1ContactDTO> getContacts() {
        return contacts;
    }

    public void setContacts(List<BrAPIv1ContactDTO> contacts) {
        this.contacts = contacts;
    }

    public List<BrAPIv1DataLinkDTO> getDatalinks() {
        return datalinks;
    }

    public void setDatalinks(List<BrAPIv1DataLinkDTO> datalinks) {
        this.datalinks = datalinks;
    }

    public BrAPIv1LocationDTO getLocation() {
        return location;
    }

    public void setLocation(BrAPIv1LocationDTO location) {
        this.location = location;
    }
    
    public static BrAPIv1StudyDetailsDTO fromModel(ExperimentModel model) {
        BrAPIv1StudyDetailsDTO study = new BrAPIv1StudyDetailsDTO();
        if (model.getUri() != null) {
           study.setStudyDbId(model.getUri().toString()); 
        }
        
        study.setName(model.getName());
        study.setStudyName(model.getName());
        
        if (model.getStartDate() != null) {
            study.setStartDate(model.getStartDate().toString());
        }
        
        if (model.getEndDate() != null) {
            study.setEndDate(model.getEndDate().toString());
        } 
        
        LocalDate date = LocalDate.now();
        if ((model.getStartDate() != null && model.getStartDate().isAfter(date)) || (model.getEndDate() != null && model.getEndDate().isBefore(date)))  {
            study.setActive("false");
        } else {
            study.setActive("true");
        }
        
        List<BrAPIv1SeasonDTO> seasons = new ArrayList<>();
        BrAPIv1SeasonDTO season = new BrAPIv1SeasonDTO();
        //season.setYear(model.getCampaign());
        seasons.add(season);
        study.setSeasons(seasons);
        
        return study;
    }
    
}
