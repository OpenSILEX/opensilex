//******************************************************************************
//                                GermplasmPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 24 juin 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.germplasm;

import java.util.ArrayList;
import opensilex.service.dao.GermplasmDAO;
import opensilex.service.model.Germplasm;
import opensilex.service.ontology.Contexts;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.utils.UriGenerator;

/**
 * GermplasmPostDTO
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class GermplasmPostDTO extends AbstractVerifiedClass {
    /*voir https://www.genesys-pgr.org/fr/doc/0/basics*/
    
    //private String germplasmName;
    private String accessionURI;
    private String accessionNumber;
    private String varietyURI; 
    private String varietyLabel; 
    private String speciesURI;
    private String speciesLabel;
    private ArrayList<String> seedLots;
    private String instituteCode;
    private String instituteName;  
    
    @Override
    public Germplasm createObjectFromDTO() throws Exception {
        boolean annotationInsert = true;
        Germplasm germplasm = new Germplasm();
        GermplasmDAO germplasmDAO = new GermplasmDAO();
        
        //species is mandatory
        if (speciesURI != null | speciesLabel != null) {
            
            //If accessionURI exists in DB then, even if accessionNumber is given then we don't retrieve it
            if (accessionURI != null) {
                if (!germplasmDAO.existUriInGraph(accessionURI, Contexts.ACCESSION.toString())) {
                    germplasm.setAccessionNumber(accessionNumber);
                } else {
                    accessionNumber = germplasmDAO.findLabelsForUri(accessionURI).get(0);                    
                }
            } else {
                if (accessionNumber != null) {  
                    germplasm.setAccessionNumber(accessionNumber);
                    if (germplasmDAO.askExistLabelInContext(accessionNumber, Contexts.ACCESSION.toString())) {
                        germplasm.setAccessionURI(germplasmDAO.getURIFromLabel(accessionNumber));
                    } else {
                        try {
                            accessionURI = UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_ACCESSION.toString(), null, accessionNumber);
                            germplasm.setAccessionURI(accessionURI);
                        } catch (Exception ex) { //In the sensors case, no exception should be raised
                            annotationInsert = false;
                        }
                    }
                }            
            }     

            //If varietyURI exists in DB then, even if varietyLabel is given then we don't retrieve it
            if (varietyURI != null) {
                germplasm.setVarietyURI(varietyURI);
                if (!germplasmDAO.existUriInGraph(varietyURI, Contexts.VARIETY.toString())) {
                    germplasm.setVarietyLabel(varietyLabel);
                } else {
                    varietyLabel = germplasmDAO.findLabelsForUri(varietyURI).get(0);                    
                }
            } else {
                if (varietyLabel != null) {                
                    germplasm.setVarietyLabel(varietyLabel);
                    if (germplasmDAO.askExistLabelInContext(varietyLabel, Contexts.VARIETY.toString())) {
                        germplasm.setVarietyURI(germplasmDAO.getURIFromLabel(varietyLabel));
                    } else {
                        try {
                            varietyURI = UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_VARIETY.toString(), null, varietyLabel);
                            germplasm.setVarietyURI(varietyURI);
                        } catch (Exception ex) { //In the sensors case, no exception should be raised
                            annotationInsert = false;
                        }
                    }
                }            
            }        
        
            //If speciesURI exists in DB then, even if speciesLabel is given then we don't retrieve it
            if (speciesURI != null) {
                germplasm.setSpeciesURI(speciesURI);
                if (!germplasmDAO.existUriInGraph(speciesURI, Contexts.SPECIES.toString())) {
                    germplasm.setSpeciesLabel(speciesLabel);
                } else {
                    speciesLabel = germplasmDAO.findLabelsForUri(speciesURI).get(0);                    
                }
            } else {
                if (speciesLabel != null) {                
                    germplasm.setSpeciesLabel(speciesLabel);
                    if (germplasmDAO.askExistLabelInContext(speciesLabel, Contexts.SPECIES.toString())) {
                        germplasm.setSpeciesURI(germplasmDAO.getURIFromLabel(speciesLabel));
                    } else {
                        try {
                            speciesURI = UriGenerator.generateNewInstanceUri(Oeso.CONCEPT_SPECIES.toString(), null, speciesLabel);
                            germplasm.setSpeciesURI(speciesURI);
                        } catch (Exception ex) { //In the sensors case, no exception should be raised
                            annotationInsert = false;
                        }
                    }
                }            
            }       

            //Set germplasmURI : accessionURI or varietyURI or speciesURI
            if (accessionURI != null) {
                germplasm.setGermplasmURI(accessionURI);
            } else if (varietyURI != null) {
                germplasm.setGermplasmURI(varietyURI);
            } else {
                germplasm.setGermplasmURI(speciesURI);                
            }
            
//            //Set germplasmName by following this priority order : given germplasmName, accessionName, varietyLabel, speciesLabel
//            if (germplasmName != null) {
//                germplasm.setGermplasmName(germplasmName);
//            } else {
//                if (accessionNumber != null) {
//                    germplasm.setGermplasmName(germplasmName);
//                }
//                if (varietyLabel != null) {
//                    germplasm.setGermplasmName(varietyLabel);
//                } else {
//                    germplasm.setGermplasmName(speciesLabel);
//                }
//            }   
        
            if (seedLots != null && accessionURI != null) {
                germplasm.setSeedLots(seedLots);
            }
            
            if (instituteCode != null && accessionURI != null){
                germplasm.setInstituteCode(instituteCode);
            }
            
            if (instituteName != null && accessionURI != null){
                germplasm.setInstituteName(instituteName);
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
