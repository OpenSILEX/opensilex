//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.template.FileTemplate;
import org.opensilex.aiimport.template.TemplateSheet;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A template the user may use: their own, or one another user shared.
 *
 * @author Arnaud Charleroy
 */
public class FileTemplateDTO {

    private String id;
    private String name;

    @JsonProperty("author_name")
    private String authorName;

    private boolean shared;

    @JsonProperty("is_mine")
    private boolean mine;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    private List<String> sheets = new ArrayList<>();

    public static FileTemplateDTO fromModel(FileTemplate model, URI user) {
        FileTemplateDTO dto = new FileTemplateDTO();
        dto.id = model.getId();
        dto.name = model.getName();
        dto.authorName = model.getAuthorName();
        dto.shared = model.isShared();
        dto.mine = model.isOwnedBy(user);
        dto.createdAt = String.valueOf(model.getCreatedAt());
        dto.updatedAt = String.valueOf(model.getUpdatedAt());
        dto.sheets = model.getSheets().stream().map(TemplateSheet::getName).collect(Collectors.toList());
        return dto;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAuthorName() {
        return authorName;
    }

    @ApiModelProperty(value = "Whether other users can see and use it")
    public boolean isShared() {
        return shared;
    }

    @ApiModelProperty(value = "Whether it is the user's own: only its author changes or deletes it")
    public boolean isMine() {
        return mine;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    @ApiModelProperty(value = "The sheets it describes")
    public List<String> getSheets() {
        return sheets;
    }
}
