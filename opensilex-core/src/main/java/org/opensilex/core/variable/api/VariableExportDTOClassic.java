//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variable.api;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opensilex.core.germplasm.api.GermplasmAPI;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variable.dal.MultiLabelsModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

/**
 *
 * @author Hamza IKIOU
 */

@JsonPropertyOrder({
        "uri","prefLabels", "shortLabels", "altLabels", "definitions", "dataType", "species", "timeInterval", "samplingInterval",
        "entityURI", "entity_prefLabels", "entity_shortLabel", "entity_altLabels", "entity_definitions","entityOfInterestURI", "entityOfInterestName",
        "characteristicURI", "characteristicName", "methodURI", "methodName", "unitURI", "unitName"
})

public class VariableExportDTOClassic extends MultiLabelsDTO {

    private URI uri;
    private URI entity_uri;

    private Map<String,String> entity_prefLabels;

    private Map<String,String> entity_shortLabel;

    private Map<String,List<String>> entity_altLabels;

    private Map<String,String> entity_definitions;

    private URI entity_of_interest_uri;
    
    private String entity_of_interest_label;
    
    private URI characteristic_uri;
    
    private String characteristic_label;
        
    private URI method_uri;
    
    private String method_label; 
    
    private URI unit_uri;
    
    private String unit_label;

    private String timeInterval;

    private String samplingInterval;

    private URI dataType;
    
    private List<URI> species;
    
    public VariableExportDTOClassic(VariableModel model) {
        super(model);

        this.entity_uri = model.getEntity().getUri();

        this.entity_prefLabels = model.getEntity().getPrefLabels().getAllTranslations();

        this.entity_shortLabel = model.getEntity().getShortLabels().getAllTranslations();

        this.entity_altLabels = model.getEntity().getAltsLabels().getTranslations();

        this.entity_definitions = model.getEntity().getDefinitions().getAllTranslations();

        if(model.getEntityOfInterest() != null){
            this.entity_of_interest_uri = model.getEntityOfInterest().getUri();
            this.entity_of_interest_label = model.getEntityOfInterest().getName();
        }
        
        this.characteristic_uri = model.getCharacteristic().getUri();
        this.characteristic_label = model.getCharacteristic().getName();

        this.method_uri = model.getMethod().getUri();
        this.method_label = model.getMethod().getName();
        
        this.unit_uri = model.getUnit().getUri();
        this.unit_label = model.getUnit().getName();

        this.timeInterval = model.getTimeInterval();
        
        this.samplingInterval = model.getSamplingInterval();

        URI dataType = model.getDataType();
        try {
            this.dataType = new URI(SPARQLDeserializers.getExpandedURI(dataType));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        if(model.getSpecies() != null){
            List<URI> uris = new ArrayList<>();
            for(SpeciesModel species : model.getSpecies()){
                uris.add(species.getUri());
            }
            this.species = uris;
        }
        
    }

    public VariableExportDTOClassic() {
    }
    
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/Plant_Height")
    public URI getUri() {
        return this.uri;
    }
    
    public URI getEntityURI(){
        return this.entity_uri;
    }
    
    public void setEntityURI(URI entityURI){
        this.entity_uri = entityURI;
    }


    public URI getEntityOfInterestURI(){
        return this.entity_of_interest_uri;
    }
    
    public void setEntityOfInterestURI(URI entityOfInterestURI){
        this.entity_of_interest_uri = entityOfInterestURI;
    }
    
    public String getEntityOfInterestName(){
        return this.entity_of_interest_label;
    }
    
    public void setEntityOfInterestName(String entityOfInterestLabel){
        this.entity_of_interest_label = entityOfInterestLabel;
    }
    
    public URI getCharacteristicURI(){
        return this.characteristic_uri;
    }
    
    public void setCharacteristicURI(URI characteristicURI){
        this.characteristic_uri = characteristicURI;
    }
    
    public String getCharacteristicName(){
        return this.characteristic_label;
    }
    
    public void setCharacteristicName(String characteristicLabel){
        this.characteristic_label = characteristicLabel;
    }
    
    public URI getMethodURI(){
        return this.method_uri;
    }
    
    public void setMethodURI(URI methodURI){
        this.method_uri = methodURI;
    }
    
    public String getMethodName(){
        return this.method_label;
    }
    
    public void setMethodName(String methodLabel){
        this.method_label = methodLabel;
    }
    
    public URI getUnitURI(){
        return this.unit_uri;
    }
    
    public void setUnitURI(URI unitURI){
        this.unit_uri = unitURI;
    }
    
    public String getUnitName(){
        return this.unit_label;
    }
    
    public void setUnitName(String unitLabel){
        this.unit_label = unitLabel;
    }
    
    @ApiModelProperty(notes = "Define the time between two data recording", example = "minutes")
    public String getTimeInterval() {
        return this.timeInterval;
    }

    public void setTimeInterval(String timeInterval) {
        this.timeInterval = timeInterval;
    }

    @ApiModelProperty(notes = "Define the distance between two data recording", example = "minutes")
    public String getSamplingInterval() {
        return this.samplingInterval;
    }

    public void setSamplingInterval(String samplingInterval) {
        this.samplingInterval = samplingInterval;
    }

    public URI getDataType() {
        return this.dataType;
    }

    @ApiModelProperty(notes = "XSD type of the data associated with the variable", example = "http://www.w3.org/2001/XMLSchema#integer")
    public void setDataType(URI dataType) {
        this.dataType = dataType;
    }
    
    @ValidURI
    @ApiModelProperty(notes = "Species associated with the variable", example = GermplasmAPI.GERMPLASM_EXAMPLE_SPECIES)
    public List<URI> getSpecies() {
        return this.species;
    }

    public void setSpecies(List<URI> species) {
        this.species = species;
    }
    
    public static VariableExportDTOClassic fromModel(VariableModel model){
        VariableExportDTOClassic dto = new VariableExportDTOClassic(model);       
        return dto;        
    }
}
