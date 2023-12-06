//******************************************************************************
//                          BrAPIv1StudyDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.security.account.dal.AccountModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1StudyDTO extends BrAPIv1SuperStudyDTO{
    private List<BrAPIv1SeasonDTO> seasons;

    public List<BrAPIv1SeasonDTO> getSeasons() {
        return seasons;
    }

    public void setSeasons(List<BrAPIv1SeasonDTO> seasons) {
        this.seasons = seasons;
    }

    public BrAPIv1StudyDTO extractFromModel(ExperimentModel model, GermplasmDAO germplasmDAO, AccountModel user) throws Exception {

        super.extractFromModel(model, germplasmDAO, user);

        if (model.getEndDate() != null){
            List<BrAPIv1SeasonDTO> seasons = new ArrayList<>();
            for (int studyYear = model.getStartDate().getYear(); studyYear <= model.getEndDate().getYear(); studyYear++){
                BrAPIv1SeasonDTO season = new BrAPIv1SeasonDTO();
                season.setYear(String.valueOf(studyYear));
                seasons.add(season);
            }
            this.setSeasons(seasons);
        }

        List<FacilityModel> facilitiesList = model.getFacilities();
        if (facilitiesList.size() >= 1){
            FacilityModel facility = facilitiesList.get(0);
            this.setLocationDbId(facility.getUri().toString());
            this.setLocationName(facility.getName());
        }

        return this;
    }

    public static BrAPIv1StudyDTO fromModel(ExperimentModel model, GermplasmDAO germplasmDAO, AccountModel user) throws Exception {
        BrAPIv1StudyDTO study = new BrAPIv1StudyDTO();
        return study.extractFromModel(model, germplasmDAO, user);

    }
}
