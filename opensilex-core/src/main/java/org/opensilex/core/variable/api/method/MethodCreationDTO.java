/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api.method;

import java.net.URI;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.server.rest.validation.Required;

/**
 *
 * @author vidalmor
 */
public class MethodCreationDTO extends SKOSReferencesDTO {

    @Required
    private String name;

    private String comment;

    private List<OntologyReference> relations;

    private URI type;

    private URI uri;

    public MethodModel newModel() {
        MethodModel model = new MethodModel();
        model.setName(name);
        model.setComment(comment);
        if(type != null){
            model.setType(type);
        }
        model.setUri(uri);
        setSkosReferencesToModel(model);
        return model;
    }

    @ApiModelProperty(example = "ImageAnalysis")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "Based on a software")
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

    @ApiModelProperty(example = "http://www.opensilex.org/vocabulary/oeso#Method")
    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/method/ImageAnalysis")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
