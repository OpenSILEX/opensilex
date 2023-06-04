//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.variable.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseMultiLabelIdentifierCreationDTO;
import org.opensilex.core.variable.api.BaseVariableCreationDTO;
import org.opensilex.core.variable.api.LabelDTO;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.EntityMultiLabelModel;
import org.opensilex.core.variable.dal.LabelModel;
import org.opensilex.security.group.api.GroupUserProfileModificationDTO;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class EntityCreationDTO extends BaseMultiLabelIdentifierCreationDTO<EntityMultiLabelModel> {

    @JsonProperty("labels")
    protected List<LabelDTO> labels;


    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return uri;
    }

    public void setPrefLabels(List<String> prefLabels) {
        this.prefLabels = prefLabels;
    }

    public List<String> getAltLabels() {
        return altLabels;
    }

    public void setAltLabels(List<String> altLabels) {
        this.altLabels = altLabels;
    }

    public List<String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<String> definitions) {
        this.definitions = definitions;
    }


    public void setUri(URI uri) {
        this.uri = uri;
    }

    public List<LabelDTO> getLabels() {
        return labels;
    }

    public void setLabels(List<LabelDTO> labels) {
        this.labels = labels;
    }

    public EntityCreationDTO(){

        this.labels = new ArrayList<LabelDTO>();
        this.prefLabels = new ArrayList<String>();
        this.altLabels = new ArrayList<String>();
        this.definitions = new ArrayList<String>();

    }

    @Override
    public EntityMultiLabelModel newModel() {
        EntityMultiLabelModel model = super.newModel();

        for (LabelDTO labeldto : this.labels) {
            System.out.println(labeldto.getPrefLabel());
            System.out.println(model.getPrefLabels());

            model.getPrefLabels().add(labeldto.getPrefLabel());
            for (String altLabel : labeldto.getAltLabels()){
                model.getAltsLabels().add(altLabel);
            }
            model.getDefinitions().add(labeldto.getDefinition());

        }
        return model;
    }

    @Override
    protected EntityMultiLabelModel newModelInstance() {
        return new EntityMultiLabelModel();
    }

}


