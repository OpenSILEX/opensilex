/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api.quality;

import java.net.URI;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.QualityModel;
import org.opensilex.server.rest.validation.Required;

/**
 *
 * @author vidalmor
 */
public class QualityCreationDTO extends SKOSReferencesDTO {

    @Required
    private String name;

    private String comment;

    private List<OntologyReference> relations;

    private URI type;

    private URI uri;

    public QualityModel newModel() {
        QualityModel model = new QualityModel();
        model.setName(name);
        if(!StringUtils.isEmpty(comment)){
            model.setComment(comment);
        }
        if(type != null){
            model.setType(type);
        }
        model.setUri(uri);
        setSkosReferencesToModel(model);
        return model;
    }

    @ApiModelProperty(example = "Height", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "Describe the height of a an entity")
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

    @ApiModelProperty(example = "http://www.opensilex.org/vocabulary/oeso#Quality")
    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/quality/Height")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
