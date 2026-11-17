//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.profile.custom.DescribedRole;
import org.opensilex.aiimport.template.FileTemplateMatcher;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a template proposes for a sheet the user has not described yet. Nothing is applied: the
 * screen fills its choices with it, and the user keeps them or not.
 *
 * @author Arnaud Charleroy
 */
public class SheetSuggestionDTO {

    @JsonProperty("template_id")
    private String templateId;

    @JsonProperty("template_name")
    private String templateName;

    @JsonProperty("author_name")
    private String authorName;

    @JsonProperty("updated_at")
    private String updatedAt;

    private double score;
    private int recognised;
    private int total;
    private String kind;
    private Map<String, String> roles = new LinkedHashMap<>();

    @JsonProperty("rdf_type")
    private URI rdfType;

    private Map<String, String> properties = new LinkedHashMap<>();

    public static SheetSuggestionDTO fromModel(FileTemplateMatcher.Match match) {
        SheetSuggestionDTO dto = new SheetSuggestionDTO();
        dto.templateId = match.template().getId();
        dto.templateName = match.template().getName();
        dto.authorName = match.template().getAuthorName();
        dto.updatedAt = String.valueOf(match.template().getUpdatedAt());
        dto.score = match.score();
        dto.recognised = match.recognised();
        dto.total = match.total();
        dto.kind = match.sheet().kind().map(Enum::name).orElse(null);
        for (Map.Entry<String, DescribedRole> role : match.sheet().roles().entrySet()) {
            dto.roles.put(role.getKey(), role.getValue().name());
        }
        dto.rdfType = match.sheet().getType();
        dto.properties.putAll(match.sheet().getProperties());
        return dto;
    }

    @ApiModelProperty(value = "The template the proposal comes from")
    public String getTemplateId() {
        return templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    @ApiModelProperty(value = "The share of headers the sheet and the template's sheet have in common, from 0 to 1")
    public double getScore() {
        return score;
    }

    @ApiModelProperty(value = "How many of the sheet's headers the template knows")
    public int getRecognised() {
        return recognised;
    }

    @ApiModelProperty(value = "How many headers the sheet has")
    public int getTotal() {
        return total;
    }

    public String getKind() {
        return kind;
    }

    @ApiModelProperty(value = "The role the template gives each column, by the template's header")
    public Map<String, String> getRoles() {
        return roles;
    }

    public URI getRdfType() {
        return rdfType;
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
