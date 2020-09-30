/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api.unit;

import java.net.URI;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.server.rest.validation.Required;

/**
 *
 * @author vidalmor
 */
public class UnitCreationDTO extends SKOSReferencesDTO {

    @Required
    private String name;

    private String comment;

    private List<OntologyReference> relations;

    private URI type;

    private URI uri;

    private String symbol;

    private String alternativeSymbol;

    public UnitModel newModel() {
        UnitModel model = new UnitModel();
        model.setName(name);
        if(!StringUtils.isEmpty(comment)){
            model.setComment(comment);
        }
        if(type != null){
            model.setType(type);
        }
        model.setUri(uri);
        model.setSymbol(getSymbol());
        model.setAlternativeSymbol(getAlternativeSymbol());
        setSkosReferencesToModel(model);
        return model;
    }

    @ApiModelProperty(example = "Centimeter")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "A common unit for describing a length")
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<OntologyReference> getRelations() {
        return relations;
    }

    public void setRelations(List<OntologyReference> relations) {
        this.relations = relations;
    }

    @ApiModelProperty(example = "http://www.opensilex.org/vocabulary/oeso#Unit")
    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/unit/Centimeter")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = "cm")
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @ApiModelProperty(example = "cm")
    public String getAlternativeSymbol() {
        return alternativeSymbol;
    }

    public void setAlternativeSymbol(String alternativeSymbol) {
        this.alternativeSymbol = alternativeSymbol;
    }
    
}
