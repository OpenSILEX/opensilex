//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.resolve.LearnedCorrection;
import org.opensilex.aiimport.resolve.ResolvedComponent;
import org.opensilex.aiimport.resolve.ResolvedItem;
import org.opensilex.aiimport.resolve.ResourceReference;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @JsonProperty("parent_value")
    private String parentValue;

    private Map<String, String> details = new LinkedHashMap<>();

    private String status;

    private List<MatchDTO> matches = new ArrayList<>();

    /**
     * For a missing item: existing resources with a close name, closest first. Offered only — the
     * item stays missing until the user confirms one.
     */
    private List<MatchDTO> suggestions = new ArrayList<>();

    @JsonProperty("confirmed_by_user")
    private boolean confirmedByUser;

    /**
     * Set when the item was recognised from a correction the instance remembers: on whose word,
     * and since when.
     */
    @JsonProperty("learned_correction")
    private LearnedCorrectionDTO learnedCorrection;

    /**
     * Who taught a correction, and when. The misspelling itself is the item's source value.
     */
    public static class LearnedCorrectionDTO {

        private String author;
        private String created;

        static LearnedCorrectionDTO fromModel(LearnedCorrection model) {
            LearnedCorrectionDTO dto = new LearnedCorrectionDTO();
            dto.author = model.getAuthor();
            dto.created = model.getCreated() == null ? null : model.getCreated().toLocalDate().toString();
            return dto;
        }

        @ApiModelProperty(value = "Who taught it", example = "Alice Martin")
        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        @ApiModelProperty(value = "When, as a date", example = "2026-09-26")
        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }
    }

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

        @JsonProperty("suggested_name")
        private String suggestedName;

        @JsonProperty("suggested_uri")
        private URI suggestedUri;

        static ComponentDTO fromModel(ResolvedComponent model) {
            ComponentDTO dto = new ComponentDTO();
            dto.role = model.getRole();
            dto.name = model.getName();
            dto.accession = model.getAccession();
            dto.uri = model.getUri();
            if (model.getSuggestion() != null) {
                dto.suggestedName = model.getSuggestion().getName();
                dto.suggestedUri = model.getSuggestion().getUri();
            }
            return dto;
        }

        @ApiModelProperty(value = "The closest existing one when the name matches none exactly")
        public String getSuggestedName() {
            return suggestedName;
        }

        public void setSuggestedName(String suggestedName) {
            this.suggestedName = suggestedName;
        }

        public URI getSuggestedUri() {
            return suggestedUri;
        }

        public void setSuggestedUri(URI suggestedUri) {
            this.suggestedUri = suggestedUri;
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

        /**
         * On a suggestion only: how close its name is to the file's, from 0 to 1.
         */
        private Double similarity;

        public static MatchDTO fromModel(ResourceReference model) {
            MatchDTO dto = new MatchDTO();
            dto.uri = model.getUri();
            dto.name = model.getName();
            dto.sharedResourceInstance = model.getSharedResourceInstance();
            dto.sharedResourceInstanceLabel = model.getSharedResourceInstanceLabel();
            dto.similarity = model.getSimilarity();
            return dto;
        }

        @ApiModelProperty(value = "On a suggestion: how close its name is to the file's, 0 to 1",
                example = "0.9")
        public Double getSimilarity() {
            return similarity;
        }

        public void setSimilarity(Double similarity) {
            this.similarity = similarity;
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
        dto.parentValue = model.getParentValue();
        dto.details = new LinkedHashMap<>(model.getDetails());
        dto.status = model.getStatus().name();
        dto.hint = model.getHint();
        dto.hintMessage = ReportMessageDTO.fromModel(model.getHintMessage());
        model.getComponents().forEach(component ->
                dto.components.add(ComponentDTO.fromModel(component)));
        for (ResourceReference reference : model.getMatches()) {
            dto.matches.add(MatchDTO.fromModel(reference));
        }
        for (ResourceReference suggestion : model.getSuggestions()) {
            dto.suggestions.add(MatchDTO.fromModel(suggestion));
        }
        dto.confirmedByUser = model.isConfirmedByUser();
        if (model.getLearnedCorrection() != null) {
            dto.learnedCorrection = LearnedCorrectionDTO.fromModel(model.getLearnedCorrection());
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

    @ApiModelProperty(value = "What the file names as containing this item, such as the institution of a unit")
    public String getParentValue() {
        return parentValue;
    }

    public void setParentValue(String parentValue) {
        this.parentValue = parentValue;
    }

    @ApiModelProperty(value = "What else the file says of the resource, for the form that creates it")
    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
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

    @ApiModelProperty(value = "For a missing item: existing resources with a close name")
    public List<MatchDTO> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<MatchDTO> suggestions) {
        this.suggestions = suggestions;
    }

    @ApiModelProperty(value = "True when the match was confirmed by the user among suggestions")
    public boolean isConfirmedByUser() {
        return confirmedByUser;
    }

    public void setConfirmedByUser(boolean confirmedByUser) {
        this.confirmedByUser = confirmedByUser;
    }

    @ApiModelProperty(value = "Set when recognised from a correction the instance remembers")
    public LearnedCorrectionDTO getLearnedCorrection() {
        return learnedCorrection;
    }

    public void setLearnedCorrection(LearnedCorrectionDTO learnedCorrection) {
        this.learnedCorrection = learnedCorrection;
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
