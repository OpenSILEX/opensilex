//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.agroportal.api;

import org.opensilex.core.external.agroportal.AgroportalTermModel;
import org.opensilex.core.external.agroportal.AgroportalLinksModel;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a term from AgroPortal
 *
 * @author brice
 */
public class AgroportalTermDTO {

    @NotNull
    private String id;
    private String name;
    private List<String> synonym;
    private List<String> definitions;
    @NotNull
    private String ontologyName;
    private boolean obsolete;
    private String type;
    private AgroportalLinksModel links;


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

    public String getOntologyName() {
        return ontologyName;
    }

    public void setOntologyName(String ontologyName) {
        this.ontologyName = ontologyName;
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

    public AgroportalLinksModel getLinks() {
        return links;
    }

    public void setLinks(AgroportalLinksModel links) {
        this.links = links;
    }

    public AgroportalTermDTO() {
        this.definitions = new ArrayList();
    }

    public static AgroportalTermDTO fromModel(AgroportalTermModel model) {
        AgroportalTermDTO dto = new AgroportalTermDTO();

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

        if (model.getLinks() != null) {
            String str = model.getLinks().getOntology();
            String last = str.substring(str.lastIndexOf('/') + 1);
            dto.setOntologyName(last);
        }

        return dto;
    }

}
