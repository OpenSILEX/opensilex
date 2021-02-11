//******************************************************************************
//                          StudyDetailsDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.experiment.dal.ExperimentModel;

/**
 * @see Brapi documentation V1.3 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3
 * @author Alice Boizet
 */
public class StudyDetailsDTO extends StudyDTO {
    private List<Contact> contacts;
    private List<DataLink> datalinks;
    private Location location;

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public List<DataLink> getDatalinks() {
        return datalinks;
    }

    public void setDatalinks(List<DataLink> datalinks) {
        this.datalinks = datalinks;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
    
    public static StudyDetailsDTO fromModel(ExperimentModel model) {
        StudyDetailsDTO study = new StudyDetailsDTO();
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
        
        List<Season> seasons = new ArrayList<>();
        Season season = new Season();
        //season.setYear(model.getCampaign());
        seasons.add(season);
        study.setSeasons(seasons);
        
        return study;
    }
    
}
