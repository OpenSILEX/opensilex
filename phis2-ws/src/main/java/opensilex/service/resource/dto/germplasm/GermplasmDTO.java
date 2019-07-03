//******************************************************************************
//                                GermplasmDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 24 juin 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.germplasm;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.model.Germplasm;
import opensilex.service.resource.dto.experiment.ExperimentDTO;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GermplasmPostDTO
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class GermplasmDTO extends AbstractVerifiedClass {
    
    final static Logger LOGGER = LoggerFactory.getLogger(ExperimentDTO.class);
    
    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SENSOR_URI)
    private String uri;
    private String species;
    private String variety;
    private String accessionNumber;
    private String accessionName;
    private ArrayList<String> plantMaterialLots;
    private String institute;

    public GermplasmDTO(Germplasm germplasm) {
        uri = germplasm.getUri();
        species = germplasm.getSpecies();
        variety = germplasm.getVariety();
        accessionName = germplasm.getAccessionName();
        accessionNumber = germplasm.getAccessionNumber();
        plantMaterialLots = germplasm.getPlantMaterialLots();
        institute = germplasm.getInstitute();
    }

    @Override
    public Germplasm createObjectFromDTO() throws Exception {
        Germplasm germplasm = new Germplasm();
        germplasm.setUri(uri);
        germplasm.setSpecies(species);
        germplasm.setVariety(variety);
        germplasm.setAccessionName(accessionName);
        germplasm.setAccessionNumber(accessionNumber);
        germplasm.setPlantMaterialLots(plantMaterialLots);
        germplasm.setInstitute(institute);
                
        return germplasm;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
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

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getAccessionName() {
        return accessionName;
    }

    public void setAccessionName(String accessionName) {
        this.accessionName = accessionName;
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
