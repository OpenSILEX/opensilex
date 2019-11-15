//******************************************************************************
//                                       ScientificObjectDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 29 mars 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.scientificObject;

import java.util.ArrayList;
import opensilex.service.dao.GermplasmDAO;
import opensilex.service.model.Property;
import opensilex.service.model.ScientificObject;
import opensilex.service.resource.dto.germplasm.GermplasmDTO;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.dto.rdfResourceDefinition.PropertyDTO;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ScientificObjectDTO extends AbstractVerifiedClass {
    //scientific object uri
    //@example http://www.opensilex.org/opensilex/2019/o19000019
    private String uri;
    //type of the scientific object
    //@example http://www.opensilex.org/vocabulary/oeso#Plot
    private String rdfType;
    //geometry of the scientific object
    //@example {\"type\":\"Polygon\",\"coordinates\":[[[3.97167246,43.61328981],
    //[3.97171243,43.61332417],[3.9717427,43.61330558],[3.97170272,43.61327122],
    //[3.97167246,43.61328981],[3.97167246,43.61328981]]]}
    private String geometry;
    //experiment of the scientific object
    //@example http://www.opensilex.org/demo/DMO2018-1
    private String experiment;
    //object which has part the scientific object
    //@example http://www.opensilex.org/opensilex/2019/o19000012
    private String isPartOf;
    //label of the scientific object
    //@example M02_W
    private String label;
    //germplasm
    private GermplasmDTO germplasm;
    //properties of the scientific object
    private ArrayList<PropertyDTO> properties = new ArrayList<>();

    public ScientificObjectDTO() {
       
    }
    
    public ScientificObjectDTO(ScientificObject scientificObject) {
        setUri(scientificObject.getUri());
        setRdfType(scientificObject.getRdfType());
        setGeometry(scientificObject.getGeometry());
        setExperiment(scientificObject.getExperiment());
        setIsPartOf(scientificObject.getIsPartOf());
        setLabel(scientificObject.getLabel());
        
        if (scientificObject.getGermplasmURI() != null) {
            GermplasmDAO germplasmDAO = new GermplasmDAO();
            setGermplasm(germplasmDAO.getGermplasmDTO(germplasmDAO.findById(scientificObject.getGermplasmURI()), "en"));
        }        
       
        for (Property property : scientificObject.getProperties()) {
            addProperty(new PropertyDTO(property));
        }
    }
    
    @Override
    public ScientificObject createObjectFromDTO() {
        ScientificObject scientificObject = new ScientificObject();
        scientificObject.setUri(uri);
        scientificObject.setRdfType(rdfType);
        scientificObject.setGeometry(geometry);
        scientificObject.setExperiment(experiment);
        scientificObject.setIsPartOf(isPartOf);
        scientificObject.setLabel(label);
        
        for (PropertyDTO property : properties) {
            scientificObject.addProperty(property.createObjectFromDTO());
        }
        
        return scientificObject;
    }
    
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getUriExperiment() {
        return experiment;
    }

    public void setUriExperiment(String uriExperiment) {
        this.experiment = uriExperiment;
    }
    
    public ArrayList<PropertyDTO> getProperties() {
        return properties;
    }
    
    public void addProperty(PropertyDTO property) {
        properties.add(property);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    public String getIsPartOf() {
        return isPartOf;
    }

    public void setIsPartOf(String isPartOf) {
        this.isPartOf = isPartOf;
    }

    public GermplasmDTO getGermplasm() {
        return germplasm;
    }

    public void setGermplasm(GermplasmDTO germplasm) {
        this.germplasm = germplasm;
    }
        
}
