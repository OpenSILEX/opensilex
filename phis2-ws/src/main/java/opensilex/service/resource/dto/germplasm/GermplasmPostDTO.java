//******************************************************************************
//                                GermplasmPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 24 juin 2019
// Contact: alice.boizet@inra.fr, column 15 in file:///home/boizetal/OpenSilex/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.germplasm;

import java.util.ArrayList;
import opensilex.service.model.Germplasm;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.Required;

/**
 * GermplasmPostDTO
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class GermplasmPostDTO extends AbstractVerifiedClass {
    
    private String species;
    private String variety;
    
    @Required
    private String accessionName;
    
    @Required
    private String accessionNumber;
    private ArrayList<String> plantMaterialLots;
    private String institute;

    @Override
    public Germplasm createObjectFromDTO() throws Exception {
        Germplasm germplasm = new Germplasm();
        germplasm.setAccessionName(accessionName);
        germplasm.setAccessionNumber(accessionNumber);
        germplasm.setInstitute(institute);
        germplasm.setPlantMaterialLots(plantMaterialLots);
        germplasm.setSpecies(species);
        germplasm.setVariety(variety);
        
        return germplasm;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getVariety() {
        return variety;
    }

    public void setVariety(String variety) {
        this.variety = variety;
    }

    public String getAccessionName() {
        return accessionName;
    }

    public void setAccessionName(String accessionName) {
        this.accessionName = accessionName;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public ArrayList<String> getPlantMaterialLots() {
        return plantMaterialLots;
    }

    public void setPlantMaterialLots(ArrayList<String> plantMaterialLots) {
        this.plantMaterialLots = plantMaterialLots;
    }

    public String getInstitute() {
        return institute;
    }

    public void setInstitute(String institute) {
        this.institute = institute;
    }

    
}
