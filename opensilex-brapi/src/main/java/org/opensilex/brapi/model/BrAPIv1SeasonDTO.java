//******************************************************************************
//                          BrAPIv1SeasonDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1SeasonDTO {
    private String season;
    private String seasonDbId;
    private Integer year;    

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getSeasonDbId() {
        return seasonDbId;
    }

    public void setSeasonDbId(String seasonDbId) {
        this.seasonDbId = seasonDbId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
    
}
