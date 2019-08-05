//******************************************************************************
//                                GermplasmPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 24 juin 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.germplasm;

import java.util.ArrayList;
import opensilex.service.model.Germplasm;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.utils.UriGenerator;
import static org.apache.jena.util.FileUtils.isURI;

/**
 * GermplasmPostDTO
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class GermplasmPostDTO extends AbstractVerifiedClass {
    /*voir https://www.genesys-pgr.org/fr/doc/0/basics*/
    
    private String accessionURI;
    private String accessionNumber;
    private String varietyURI; 
    private String varietyLabel; 
    private String speciesURI;
    private String instituteCode;
    private String instituteName;
    



    @Override
    public Germplasm createObjectFromDTO() throws Exception {
        boolean annotationInsert = true;
        Germplasm germplasm = new Germplasm();
        
        if (accessionURI != null) {
            germplasm.setAccessionURI(accessionURI);             
        } else {
            if (accessionNumber != null) {   
                try {
                    accessionURI = UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_ACCESSION.toString(), null, accessionNumber);
                    germplasm.setAccessionURI(accessionURI);
                } catch (Exception ex) { //In the sensors case, no exception should be raised
                    annotationInsert = false;
                }
            }
        }
        
        if (accessionNumber != null) {   
            germplasm.setAccessionNumber(accessionNumber);
        }              

        if (varietyURI != null) {
            germplasm.setVarietyURI(varietyURI);
        } else {
            if (varietyLabel != null) {
                try {
                    varietyURI = UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_VARIETY.toString(), null, varietyLabel);
                    germplasm.setVarietyURI(varietyURI);
                } catch (Exception ex) { //In the sensors case, no exception should be raised
                    annotationInsert = false;
                }
            }            
        }        
        
        if (varietyLabel != null) {
            germplasm.setVarietyLabel(varietyLabel);
        }
        
        if (speciesURI != null) {
            germplasm.setSpeciesURI(speciesURI);
        }        
        
        if (accessionURI != null) {
            germplasm.setGermplasmURI(accessionURI);
        } else {
            if (varietyURI != null) {
                germplasm.setGermplasmURI(varietyURI);
            } else {
                germplasm.setGermplasmURI(speciesURI);
            }
        }
         
        return germplasm;
    }

    public String getAccessionURI() {
        return accessionURI;
    }

    public void setAccessionURI(String accessionURI) {
        this.accessionURI = accessionURI;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getVarietyURI() {
        return varietyURI;
    }

    public void setVarietyURI(String varietyURI) {
        this.varietyURI = varietyURI;
    }

    public String getVarietyLabel() {
        return varietyLabel;
    }

    public void setVarietyLabel(String varietyLabel) {
        this.varietyLabel = varietyLabel;
    }

    public String getSpeciesURI() {
        return speciesURI;
    }

    public void setSpeciesURI(String speciesURI) {
        this.speciesURI = speciesURI;
    }

    public String getInstituteCode() {
        return instituteCode;
    }

    public void setInstituteCode(String instituteCode) {
        this.instituteCode = instituteCode;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public void setInstituteName(String instituteName) {
        this.instituteName = instituteName;
    }

   
}
