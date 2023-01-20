package org.opensilex.brapi.model;

/**
 * @see Brapi documentation V2.1 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI-Core/2.1
 * @author Bernhard Gschloessl
 */
public class ObservationLevelsDTO {
    private String levelName; //v2.1 : A name for this level **Standard Level Names: study, field, entry, rep, block, sub-block, plot, sub-plot, plant, pot, sample**
    private Integer levelOrder; //v2.1 : `levelOrder` defines where that level exists in the hierarchy of levels.

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public Integer getLevelOrder() {
        return levelOrder;
    }

    public void setLevelOrder(Integer levelOrder) {
        this.levelOrder = levelOrder;
    }
}
