//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.profile.custom.DescribedSheet;
import org.opensilex.aiimport.profile.custom.ExperimentNaming;
import org.opensilex.aiimport.profile.custom.SheetDescription;
import org.opensilex.aiimport.profile.custom.SheetKind;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One sheet as the description reads it: what it is taken to be, what each column is taken to be,
 * what is wrong with that — and the first rows, to choose by.
 *
 * @author Arnaud Charleroy
 */
public class SheetDescriptionDTO {

    /**
     * The rows shown; the count says how many there are in all.
     */
    public static final int MAX_ROWS = 50;

    private String sheet;
    private String kind;

    @JsonProperty("kind_chosen")
    private boolean kindChosen;

    private boolean readable;
    private List<String> headers = new ArrayList<>();
    private Map<String, String> roles = new LinkedHashMap<>();

    @JsonProperty("roles_chosen")
    private List<String> rolesChosen = new ArrayList<>();

    @JsonProperty("row_count")
    private int rowCount;

    private List<BulkValidationDTO.RowValuesDTO> rows = new ArrayList<>();

    @JsonProperty("rdf_type")
    private URI rdfType;

    @JsonProperty("base_type")
    private URI baseType;

    private Map<String, String> properties = new LinkedHashMap<>();
    private SheetSuggestionDTO suggestion;
    private String factor;

    @JsonProperty("needs_experiment")
    private boolean needsExperiment;

    @JsonProperty("names_experiment")
    private boolean namesExperiment;
    private List<ReportMessageDTO> problems = new ArrayList<>();

    public static SheetDescriptionDTO fromModel(DescribedSheet model) {
        SheetStructure data = model.sheet();
        SheetDescription said = model.description();
        SheetDescriptionDTO dto = new SheetDescriptionDTO();
        dto.sheet = model.name();
        dto.needsExperiment = ExperimentNaming.needs(model.kind());
        dto.namesExperiment = ExperimentNaming.names(model);
        dto.kind = model.kind().name();
        dto.kindChosen = said != null && said.getKind() != null;
        dto.readable = model.isReadable();
        dto.headers.addAll(data.getHeaders());
        model.roles().forEach((header, role) -> dto.roles.put(header, role.name()));
        if (said != null) {
            dto.rolesChosen.addAll(said.getRoles().keySet());
            dto.rdfType = said.getType();
            dto.properties.putAll(said.getProperties());
            dto.factor = said.getFactor();
        }
        SheetKind kind = model.kind();
        dto.baseType = kind.baseType().orElse(null);
        dto.rowCount = data.getRows().size();
        int shown = Math.min(MAX_ROWS, data.getRows().size());
        for (int i = 0; i < shown; i++) {
            Map<String, String> values = new LinkedHashMap<>();
            for (String header : data.getHeaders()) {
                if (header != null && !header.isEmpty()) {
                    values.put(header, data.cell(data.getRows().get(i), header));
                }
            }
            dto.rows.add(new BulkValidationDTO.RowValuesDTO(i + 2, values));
        }
        dto.problems.addAll(ReportMessageDTO.fromModels(model.problems()));
        return dto;
    }

    @ApiModelProperty(value = "Whether the rows of the sheet belong to an experiment")
    public boolean isNeedsExperiment() {
        return needsExperiment;
    }

    @ApiModelProperty(value = "Whether a column of the sheet names the experiment")
    public boolean isNamesExperiment() {
        return namesExperiment;
    }

    @ApiModelProperty(value = "The factor the user said a sheet of levels lists the levels of, when no column "
            + "names it")
    public String getFactor() {
        return factor;
    }

    public void setFactor(String factor) {
        this.factor = factor;
    }

    @ApiModelProperty(value = "What a template proposes for this sheet, when one looks like it and the user has not "
            + "described the sheet")
    public SheetSuggestionDTO getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(SheetSuggestionDTO suggestion) {
        this.suggestion = suggestion;
    }

    @ApiModelProperty(value = "The sheet's name", example = "essai")
    public String getSheet() {
        return sheet;
    }

    @ApiModelProperty(value = "What the rows of the sheet are: the user's choice, else what the columns point at",
            example = "OBJECTS_AND_DATA")
    public String getKind() {
        return kind;
    }

    @ApiModelProperty(value = "Whether the user chose the kind, rather than it being a default")
    public boolean isKindChosen() {
        return kindChosen;
    }

    @ApiModelProperty(value = "Whether the sheet is read: described as something, and without a problem")
    public boolean isReadable() {
        return readable;
    }

    public List<String> getHeaders() {
        return headers;
    }

    @ApiModelProperty(value = "The role of each column, by header: the user's choice, else a default")
    public Map<String, String> getRoles() {
        return roles;
    }

    @ApiModelProperty(value = "The columns whose role the user chose")
    public List<String> getRolesChosen() {
        return rolesChosen;
    }

    @ApiModelProperty(value = "How many rows the sheet has; at most " + MAX_ROWS + " are sent")
    public int getRowCount() {
        return rowCount;
    }

    public List<BulkValidationDTO.RowValuesDTO> getRows() {
        return rows;
    }

    @ApiModelProperty(value = "The type the user chose for the objects or facilities of the sheet")
    public URI getRdfType() {
        return rdfType;
    }

    @ApiModelProperty(value = "The class the type descends from, or none when the rows have no type")
    public URI getBaseType() {
        return baseType;
    }

    @ApiModelProperty(value = "The property each column is written to, by header, for the columns "
            + "the user chose one for")
    public Map<String, String> getProperties() {
        return properties;
    }

    @ApiModelProperty(value = "What is wrong with the description of the sheet")
    public List<ReportMessageDTO> getProblems() {
        return problems;
    }
}
