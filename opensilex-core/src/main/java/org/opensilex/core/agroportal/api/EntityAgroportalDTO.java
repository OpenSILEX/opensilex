//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.agroportal.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.codehaus.plexus.util.StringUtils;
import org.opensilex.core.agroportal.dal.EntityAgroportalModel;
import org.opensilex.core.agroportal.dal.LinksAgroportalModel;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author brice
 */
@JsonPropertyOrder({
    "id", "name", "definitions", "type", "ontologyType"
})
public class EntityAgroportalDTO {

    @NotNull
    @JsonProperty("id")
    private String id;
    @NotNull
    @JsonProperty("name")
    private String name;
    @JsonProperty("synonym")
    private List<String> synonym;
    @JsonProperty("definitions")
    private List<String> definitions;
    @JsonProperty("obsolete")
    private boolean obsolete;
    @JsonProperty("type")
    private String type;
    @JsonProperty("links")
    private LinksAgroportalModel links;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSynonym() {
        return synonym;
    }

    public void setSynonym(List<String> synonym) {
        this.synonym = synonym;
    }

    public List<String> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(List<String> definitions) {
        this.definitions = definitions;
    }

    public boolean isObsolete() {
        return obsolete;
    }

    public void setObsolete(boolean obsolete) {
        this.obsolete = obsolete;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LinksAgroportalModel getLinks() {
        return links;
    }

    public void setLinks(LinksAgroportalModel links) {
        this.links = links;
    }

    public EntityAgroportalDTO() {
        this.definitions = new ArrayList();
    }

    public static EntityAgroportalDTO fromModel(EntityAgroportalModel model) {
        EntityAgroportalDTO dto = new EntityAgroportalDTO();

        dto.setId(model.getId());
        dto.setName(model.getPrefLabel());

        if (model.getSynonym() != null && model.getSynonym().length > 0) {
            dto.setSynonym(Arrays.asList(model.getSynonym()));
        }
        if (model.getDefinitions() != null && model.getDefinitions().length > 0) {
            dto.setDefinitions(Arrays.asList(model.getDefinitions()));
        }

        dto.setObsolete(model.isObsolete());
        dto.setType(model.getType());
        dto.setLinks(model.getLinks());

        return dto;
    }

    public EntityAgroportalModel toModel() {
        EntityAgroportalModel model = new EntityAgroportalModel();

        model.setId(id);
        model.setPrefLabel(name);
        model.setObsolete(obsolete);
        model.setLinks(links);

        if (!synonym.isEmpty()) {
            model.setSynonym(synonym.toArray(new String[0]));
        }
        if (!definitions.isEmpty()) {
            model.setDefinitions(definitions.toArray(new String[0]));
        }
        if(!StringUtils.isEmpty(type)){
            model.setType(type);
        }

        return model;
    }

}
