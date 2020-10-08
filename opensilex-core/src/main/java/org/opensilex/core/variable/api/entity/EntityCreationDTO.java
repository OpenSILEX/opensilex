//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.entity;

import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.server.rest.validation.Required;

import java.net.URI;
import java.util.List;

public class EntityCreationDTO extends SKOSReferencesDTO {

    @Required
    private String name;

    private String comment;

    private List<OntologyReference> relations;

    private URI type;

    private URI uri;

    public EntityModel newModel() {
        EntityModel model = new EntityModel();
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

    @ApiModelProperty(example = "Plant", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "The entity which describe a plant")
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

    @ApiModelProperty(example = "http://www.opensilex.org/vocabulary/oeso#Entity")
    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
