//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * The uploaded file as shown in the interface.
 *
 * @author Arnaud Charleroy
 */
public class WorkbookStructureDTO {

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_size_bytes")
    private long fileSizeBytes;

    private List<SheetStructureDTO> sheets = new ArrayList<>();

    public static WorkbookStructureDTO fromModel(WorkbookStructure model, int sampleRows) {
        WorkbookStructureDTO dto = new WorkbookStructureDTO();
        dto.fileName = model.getFileName();
        dto.fileSizeBytes = model.getFileSizeBytes();
        for (SheetStructure sheet : model.getSheets()) {
            dto.sheets.add(SheetStructureDTO.fromModel(sheet, sampleRows));
        }
        return dto;
    }

    @ApiModelProperty(example = "SaisieVitisExplorer20.xlsx")
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public List<SheetStructureDTO> getSheets() {
        return sheets;
    }

    public void setSheets(List<SheetStructureDTO> sheets) {
        this.sheets = sheets;
    }
}
