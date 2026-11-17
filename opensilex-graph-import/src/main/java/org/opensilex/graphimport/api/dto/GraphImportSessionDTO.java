//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.api.dto.SheetStructureDTO;
import org.opensilex.graphimport.session.GraphImportSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An open import: its files, and the sheets of all of them read together.
 *
 * @author Arnaud Charleroy
 */
public class GraphImportSessionDTO {

    /**
     * Rows of each sheet shown before anything is decided about it: enough to recognise the sheet,
     * little enough to keep the answer small.
     */
    public static final int SAMPLE_ROWS = 5;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("files")
    private List<GraphImportFileDTO> files = new ArrayList<>();

    @JsonProperty("sheets")
    private List<SheetStructureDTO> sheets = new ArrayList<>();

    public static GraphImportSessionDTO fromModel(GraphImportSession session) {
        GraphImportSessionDTO dto = new GraphImportSessionDTO();
        dto.sessionId = session.getId();
        dto.title = session.getTitle();
        dto.createdAt = session.getCreatedAt();
        dto.files = GraphImportFileDTO.fromModel(session);
        dto.sheets = session.getWorkbook().getSheets().stream()
                .map(sheet -> SheetStructureDTO.fromModel(sheet, SAMPLE_ROWS))
                .toList();
        return dto;
    }

    @ApiModelProperty(value = "Import identifier", required = true)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @ApiModelProperty(value = "Name of the import's first file", required = true)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @ApiModelProperty(value = "When the import was opened", required = true)
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @ApiModelProperty(value = "Files of the import, in the order they were added", required = true)
    public List<GraphImportFileDTO> getFiles() {
        return files;
    }

    public void setFiles(List<GraphImportFileDTO> files) {
        this.files = files;
    }

    @ApiModelProperty(value = "Sheets of every file, read together", required = true)
    public List<SheetStructureDTO> getSheets() {
        return sheets;
    }

    public void setSheets(List<SheetStructureDTO> sheets) {
        this.sheets = sheets;
    }
}
