//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.api.dto.ReportMessageDTO;
import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.interpret.Interpretation;
import org.opensilex.graphimport.workbook.WorkbookAdditions;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * What the files of an import hold, sheet by sheet, with the evidence behind every suggestion.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterpretationDTO {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("experiment")
    @ApiModelProperty(value = "The experiment whose scientific objects are recognised; absent while none is chosen")
    private URI experiment;

    @JsonProperty("classes")
    @ApiModelProperty(value = "The classes a sheet may hold, as trees, to choose from")
    private List<ClassOptionDTO> classes;

    @JsonProperty("sheets")
    private List<SheetInterpretationDTO> sheets;

    @JsonProperty("notes")
    private List<ReportMessageDTO> notes;

    @JsonProperty("declared")
    @ApiModelProperty(value = "The resources the user declared in the assistant")
    private List<DeclaredResourceDTO> declared;

    @JsonProperty("provenance")
    @ApiModelProperty(value = "The provenance the data of the import are created with")
    private URI provenance;

    /**
     * @param fits for each sheet, for each of its link properties, the sheets its rows may point to
     */
    public static InterpretationDTO fromModel(String sessionId, URI experiment, Interpretation interpretation,
                                              ClassCatalog catalog, String language,
                                              Map<String, Map<URI, List<String>>> fits,
                                              WorkbookAdditions.ReadWorkbook read) {
        InterpretationDTO dto = new InterpretationDTO();
        dto.sessionId = sessionId;
        dto.experiment = experiment;
        dto.classes = ClassOptionDTO.treeOf(catalog, language);
        dto.sheets = interpretation.sheets().stream()
                .map(sheet -> SheetInterpretationDTO.fromModel(sheet, catalog, language,
                        fits.getOrDefault(sheet.name(), Map.of()), read))
                .toList();
        dto.notes = ReportMessageDTO.fromModels(interpretation.notes());
        dto.declared = read.decisions().declared().stream().map(DeclaredResourceDTO::fromModel).toList();
        return dto;
    }

    public String getSessionId() {
        return sessionId;
    }

    public URI getExperiment() {
        return experiment;
    }

    public List<ClassOptionDTO> getClasses() {
        return classes;
    }

    public List<SheetInterpretationDTO> getSheets() {
        return sheets;
    }

    public URI getProvenance() {
        return provenance;
    }

    /** Says the provenance chosen for the import's data. */
    public InterpretationDTO withProvenance(URI chosen) {
        provenance = chosen;
        return this;
    }

    public List<DeclaredResourceDTO> getDeclared() {
        return declared;
    }

    public List<ReportMessageDTO> getNotes() {
        return notes;
    }
}
