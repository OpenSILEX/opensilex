//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.SessionFile;
import org.opensilex.aiimport.workbook.WorkbookAssembler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A file of a conversation, and the sheets it brings.
 *
 * @author Arnaud Charleroy
 */
public class SessionFileDTO {

    private String id;
    private String name;

    @JsonProperty("size_bytes")
    private long sizeBytes;

    private String charset;
    private List<String> sheets = new ArrayList<>();

    /**
     * @return the files of the conversation in order, each with the name its sheets carry once the
     * files are put together
     */
    public static List<SessionFileDTO> fromModel(AiImportSession session) {
        List<List<String>> sheetNames = WorkbookAssembler.sheetNames(session.getFiles().stream()
                .map(SessionFile::getStructure).collect(Collectors.toList()));
        List<SessionFileDTO> dtos = new ArrayList<>();
        for (int i = 0; i < session.getFiles().size(); i++) {
            SessionFile file = session.getFiles().get(i);
            SessionFileDTO dto = new SessionFileDTO();
            dto.id = file.getId();
            dto.name = file.getName();
            dto.sizeBytes = file.getStructure().getFileSizeBytes();
            dto.charset = file.getStructure().getSheets().stream().map(sheet -> sheet.getCharset())
                    .filter(Objects::nonNull).findFirst().orElse(null);
            dto.sheets.addAll(sheetNames.get(i));
            dtos.add(dto);
        }
        return dtos;
    }

    @ApiModelProperty(value = "Identifier of the file in the conversation")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "plan.csv")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    @ApiModelProperty(value = "The charset a CSV file was read in because it is not UTF-8, when it is not")
    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    @ApiModelProperty(value = "The sheets the file brings to the conversation")
    public List<String> getSheets() {
        return sheets;
    }

    public void setSheets(List<String> sheets) {
        this.sheets = sheets;
    }
}
