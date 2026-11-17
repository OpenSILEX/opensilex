//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.service.SessionFile;
import org.opensilex.graphimport.session.GraphImportSession;

import java.util.ArrayList;
import java.util.List;

/**
 * A file of an import, with the sheets it brings.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportFileDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("sheets")
    private List<String> sheets = new ArrayList<>();

    public static GraphImportFileDTO fromModel(SessionFile file) {
        GraphImportFileDTO dto = new GraphImportFileDTO();
        dto.id = file.getId();
        dto.name = file.getName();
        dto.sheets = file.getStructure().getSheetNames();
        return dto;
    }

    public static List<GraphImportFileDTO> fromModel(GraphImportSession session) {
        return session.getFiles().stream().map(GraphImportFileDTO::fromModel).toList();
    }

    @ApiModelProperty(value = "Identifier the server gave the file", required = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Name the user gave the file", required = true, example = "plan.xlsx")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "Names of the sheets the file brings, as the import reads them")
    public List<String> getSheets() {
        return sheets;
    }

    public void setSheets(List<String> sheets) {
        this.sheets = sheets;
    }
}
