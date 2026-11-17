//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.api.dto.ReportMessageDTO;
import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.interpret.MeasureChoice;
import org.opensilex.graphimport.interpret.SheetInterpretation;
import org.opensilex.graphimport.workbook.WorkbookAdditions;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * What a sheet holds: the class of its rows, the column identifying them, what each column holds.
 *
 * @author Arnaud Charleroy
 */
public class SheetInterpretationDTO {

    @JsonProperty("name")
    private String name;

    @JsonProperty("row_count")
    @ApiModelProperty(value = "Number of data rows", example = "120")
    private int rowCount;

    @JsonProperty("sheet_class")
    private ChoiceDTO sheetClass;

    @JsonProperty("row_classes")
    @ApiModelProperty(value = "The classes the type column gives the rows; empty without a type column")
    private List<URI> rowClasses;

    @JsonProperty("identifier")
    private ChoiceDTO identifier;

    @JsonProperty("columns")
    private List<ColumnInterpretationDTO> columns;

    @JsonProperty("properties")
    @ApiModelProperty(value = "Every property the class of the sheet accepts; empty while the class is undecided")
    private List<PropertyOptionDTO> properties;

    @JsonProperty("notes")
    private List<ReportMessageDTO> notes;

    @JsonProperty("given")
    @ApiModelProperty(value = "The values the user gave for every row, by property")
    private List<PropertyValueDTO> given;

    @JsonProperty("unfilled_required")
    @ApiModelProperty(value = "The properties the class requires that no column of the sheet gives")
    private List<URI> unfilledRequired;

    @JsonProperty("declared")
    @ApiModelProperty(value = "True for a sheet the assistant added for the resources the user declared")
    private boolean declared;

    @JsonProperty("declared_ids")
    @ApiModelProperty(value = "For an added sheet, the identifier of the declared resource on each row")
    private List<String> declaredIds;

    /**
     * @param fits for each link property of the sheet, the sheets its rows may point to
     * @param read the workbook read, which tells what the user added to it
     */
    static SheetInterpretationDTO fromModel(SheetInterpretation sheet, ClassCatalog catalog, String language,
                                            Map<URI, List<String>> fits, WorkbookAdditions.ReadWorkbook read) {
        SheetInterpretationDTO dto = new SheetInterpretationDTO();
        dto.name = sheet.name();
        dto.rowCount = sheet.rowCount();
        dto.sheetClass = ChoiceDTO.ofUri(sheet.sheetClass(), sheet.sheetClass().candidates().stream()
                .map(candidate -> CandidateDTO.ofUri(candidate, catalog.labelOf(candidate.target(), language)))
                .toList());
        dto.rowClasses = sheet.rowClasses().stream().sorted(Comparator.comparing(URI::toString)).toList();
        dto.identifier = ChoiceDTO.ofColumn(sheet.identifier(), sheet.identifier().candidates().stream()
                .map(candidate -> CandidateDTO.ofColumn(candidate,
                        sheet.columns().isEmpty() ? null : sheet.columns().get(candidate.target()).header()))
                .toList());
        dto.columns = sheet.columns().stream()
                .map(column -> {
                    ColumnInterpretationDTO columnDTO = ColumnInterpretationDTO.fromModel(column, sheet.classView(),
                            language);
                    columnDTO.setGiven(read.isGiven(sheet.name(), column.index()));
                    MeasureChoice measure = sheet.measures().get(column.index());
                    if (measure != null) {
                        columnDTO.setMeasure(measure);
                    }
                    return columnDTO;
                })
                .toList();
        dto.properties = sheet.classView() == null ? List.of() : sheet.classView().getProperties().stream()
                .map(property -> PropertyOptionDTO.fromModel(property, catalog, language, fits.get(property.uri())))
                .sorted(Comparator.comparing(PropertyOptionDTO::getLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();
        dto.notes = ReportMessageDTO.fromModels(sheet.notes());
        dto.given = new ArrayList<>();
        read.decisions().of(sheet.name()).given().forEach((property, value) ->
                dto.given.add(new PropertyValueDTO(property, value)));
        dto.unfilledRequired = sheet.unfilledRequired();
        dto.declared = read.isDeclared(sheet.name());
        dto.declaredIds = read.declaredRowsOf(sheet.name());
        return dto;
    }

    public List<PropertyValueDTO> getGiven() {
        return given;
    }

    public List<URI> getUnfilledRequired() {
        return unfilledRequired;
    }

    public boolean isDeclared() {
        return declared;
    }

    public List<String> getDeclaredIds() {
        return declaredIds;
    }

    public String getName() {
        return name;
    }

    public int getRowCount() {
        return rowCount;
    }

    public ChoiceDTO getSheetClass() {
        return sheetClass;
    }

    public List<URI> getRowClasses() {
        return rowClasses;
    }

    public ChoiceDTO getIdentifier() {
        return identifier;
    }

    public List<ColumnInterpretationDTO> getColumns() {
        return columns;
    }

    public List<PropertyOptionDTO> getProperties() {
        return properties;
    }

    public List<ReportMessageDTO> getNotes() {
        return notes;
    }
}
