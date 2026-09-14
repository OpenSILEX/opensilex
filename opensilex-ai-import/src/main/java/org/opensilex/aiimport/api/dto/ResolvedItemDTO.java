//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.resolve.ResolvedComponent;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.resolve.ResourceReference;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * One name read from the file, and what the instance knows about it.
 *
 * @author Arnaud Charleroy
 */
public class ResolvedItemDTO {

    @JsonProperty("source_value")
    private String sourceValue;

    @JsonProperty("external_id")
    private String externalId;

    private String status;

    private List<MatchDTO> matches = new ArrayList<>();

    private String hint;

    /**
     * The same hint as a translation key and its parameters.
     */
    @JsonProperty("hint_message")
    private ReportMessageDTO hintMessage;

    /**
     * For a variable: the parts it is made of, and where each one already exists here.
     */
    private List<ComponentDTO> components = new ArrayList<>();

    /**
     * One part of a variable, named by the file and located here when it exists.
     */
    public static class ComponentDTO {

        private String role;
        private String name;
        private String accession;
        private URI uri;

        static ComponentDTO fromModel(ResolvedComponent model) {
            ComponentDTO dto = new ComponentDTO();
            dto.role = model.getRole();
            dto.name = model.getName();
            dto.accession = model.getAccession();
            dto.uri = model.getUri();
            return dto;
        }

        @ApiModelProperty(value = "entity, characteristic, method or unit", example = "unit")
        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        @ApiModelProperty(example = "°C day")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @ApiModelProperty(example = "CO_322:0000510")
        public String getAccession() {
            return accession;
        }

        public void setAccession(String accession) {
            this.accession = accession;
        }

        @ApiModelProperty(value = "Where it is here; absent when it has to be chosen or created")
        public URI getUri() {
            return uri;
        }

        public void setUri(URI uri) {
            this.uri = uri;
        }
    }

    /**
     * A resource that exists, here or on a shared resource instance.
     */
    public static class MatchDTO {

        private URI uri;
        private String name;

        @JsonProperty("shared_resource_instance")
        private String sharedResourceInstance;

        @JsonProperty("shared_resource_instance_label")
        private String sharedResourceInstanceLabel;

        public static MatchDTO fromModel(ResourceReference model) {
            MatchDTO dto = new MatchDTO();
            dto.uri = model.getUri();
            dto.name = model.getName();
            dto.sharedResourceInstance = model.getSharedResourceInstance();
            dto.sharedResourceInstanceLabel = model.getSharedResourceInstanceLabel();
            return dto;
        }

        public URI getUri() {
            return uri;
        }

        public void setUri(URI uri) {
            this.uri = uri;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSharedResourceInstance() {
            return sharedResourceInstance;
        }

        public void setSharedResourceInstance(String sharedResourceInstance) {
            this.sharedResourceInstance = sharedResourceInstance;
        }

        public String getSharedResourceInstanceLabel() {
            return sharedResourceInstanceLabel;
        }

        public void setSharedResourceInstanceLabel(String sharedResourceInstanceLabel) {
            this.sharedResourceInstanceLabel = sharedResourceInstanceLabel;
        }
    }

    public static ResolvedItemDTO fromModel(ResolvedItem model) {
        ResolvedItemDTO dto = new ResolvedItemDTO();
        dto.sourceValue = model.getSourceValue();
        dto.externalId = model.getExternalId();
        dto.status = model.getStatus().name();
        dto.hint = model.getHint();
        dto.hintMessage = ReportMessageDTO.fromModel(model.getHintMessage());
        model.getComponents().forEach(component ->
                dto.components.add(ComponentDTO.fromModel(component)));
        for (ResourceReference reference : model.getMatches()) {
            dto.matches.add(MatchDTO.fromModel(reference));
        }
        return dto;
    }

    @ApiModelProperty(value = "The value as written in the file", example = "Bai_Suc_g")
    public String getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(String sourceValue) {
        this.sourceValue = sourceValue;
    }

    @ApiModelProperty(value = "An ontology identifier read from the file", example = "CO_356:1000217")
    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @ApiModelProperty(value = "FOUND, AMBIGUOUS, FOUND_IN_SHARED_RESOURCE, MISSING or NOT_CHECKED",
            example = "MISSING")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<MatchDTO> getMatches() {
        return matches;
    }

    public void setMatches(List<MatchDTO> matches) {
        this.matches = matches;
    }

    @ApiModelProperty(value = "What to do about this item, in one sentence")
    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public ReportMessageDTO getHintMessage() {
        return hintMessage;
    }

    public void setHintMessage(ReportMessageDTO hintMessage) {
        this.hintMessage = hintMessage;
    }

    @ApiModelProperty(value = "For a variable: what it is made of, and what already exists here")
    public List<ComponentDTO> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentDTO> components) {
        this.components = components;
    }
}
