//******************************************************************************
//                                AccessionPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 24 juin 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.accession;

import java.util.ArrayList;
import opensilex.service.dao.AccessionDAO;
import opensilex.service.model.Accession;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.utils.UriGenerator;

/**
 * AccessionPostDTO
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class AccessionPostDTO extends AbstractVerifiedClass {
    /*voir https://www.genesys-pgr.org/fr/doc/0/basics*/
    
    private String accessionName;
    private String accessionURI;
    private String accessionNumber;
    private String varietyURI; 
    private String varietyLabel; 
    private String speciesURI;
    private String genusURI;
    private ArrayList<String> seedLots;
    private String instituteCode;
    private String instituteName;  
    
    @Override
    public Accession createObjectFromDTO() throws Exception {
        boolean annotationInsert = true;
        Accession accession = new Accession();
        AccessionDAO accessionDAO = new AccessionDAO();
        
        //If accessionURI exists in DB then error
        if (accessionURI != null) {
            if (!accessionDAO.existUriInGraph(accessionURI, Contexts.ACCESSION.toString())) {
                accession.setAccessionNumber(accessionNumber);
            }
        } else {
            // if accessionNumber doesn't exist                    
                accession.setAccessionNumber(accessionNumber);
                try {
                    accessionURI = UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_ACCESSION.toString(), null, accessionNumber);
                    accession.setAccessionURI(accessionURI);
                } catch (Exception ex) { //In the sensors case, no exception should be raised
                    annotationInsert = false;
                }  

        }     

        if (accessionName != null) {
            accession.setAccessionName(accessionName);
        }

        //If varietyURI exists in DB then, even if varietyLabel is given then we don't retrieve it
        if (varietyURI != null) {
            accession.setVarietyURI(varietyURI);
            if (!accessionDAO.existUriInGraph(varietyURI, Contexts.VARIETY.toString())) {
                accession.setVarietyLabel(varietyLabel);
            } else {
                varietyLabel = accessionDAO.findLabelsForUri(varietyURI).get(0);                    
            }
        } else {
            if (varietyLabel != null) {                
                accession.setVarietyLabel(varietyLabel);
                if (accessionDAO.askExistLabelInContext(varietyLabel, Contexts.VARIETY.toString())) {
                    accession.setVarietyURI(accessionDAO.getURIFromLabel(varietyLabel));
                } else {
                    try {
                        varietyURI = UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_VARIETY.toString(), null, varietyLabel);
                        accession.setVarietyURI(varietyURI);
                    } catch (Exception ex) { //In the sensors case, no exception should be raised
                        annotationInsert = false;
                    }
                }
            }            
        }        

        if (speciesURI != null) {
            accession.setSpeciesURI(speciesURI);
        }

        if (genusURI != null) {
            accession.setSpeciesURI(genusURI);
        }

        if (seedLots != null && accessionURI != null) {
            accession.setSeedLots(seedLots);
        }

        if (instituteCode != null && accessionURI != null){
            accession.setInstituteCode(instituteCode);
        }

        if (instituteName != null && accessionURI != null){
            accession.setInstituteName(instituteName);
        }        
               
        return accession;
    }

    public String getAccessionName() {
        return accessionName;
    }

    public void setAccessionName(String accessionName) {
        this.accessionName = accessionName;
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

    public String getGenusURI() {
        return genusURI;
    }

    public void setGenusURI(String genusURI) {
        this.genusURI = genusURI;
    }

    public ArrayList<String> getSeedLots() {
        return seedLots;
    }

    public void setSeedLots(ArrayList<String> seedLots) {
        this.seedLots = seedLots;
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
