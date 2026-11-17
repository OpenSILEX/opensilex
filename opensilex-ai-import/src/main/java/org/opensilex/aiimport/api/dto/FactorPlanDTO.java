//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.create.drafts.FactorPlan;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One factor of the experiment to declare, with the levels the platform's factor form starts with.
 *
 * @author Arnaud Charleroy
 */
public class FactorPlanDTO {

    /**
     * A level of the factor, already there or still to be created.
     */
    public static class LevelDTO {
        private URI uri;
        private String name;
        private String description;
        private boolean exists;

        @ApiModelProperty(value = "The level on the instance, when it already exists")
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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @ApiModelProperty(value = "Whether the factor already has this level")
        public boolean isExists() {
            return exists;
        }

        public void setExists(boolean exists) {
            this.exists = exists;
        }
    }

    private String name;
    private URI uri;
    private String description;
    private String status;
    private List<LevelDTO> levels = new ArrayList<>();

    public static FactorPlanDTO fromModel(FactorPlan model) {
        FactorPlanDTO dto = new FactorPlanDTO();
        dto.name = model.name();
        dto.uri = model.uri();
        dto.description = model.description();
        dto.status = model.status().name();
        dto.levels = model.levels().stream().map(level -> {
            LevelDTO levelDto = new LevelDTO();
            levelDto.uri = level.uri();
            levelDto.name = level.name();
            levelDto.description = level.description();
            levelDto.exists = level.exists();
            return levelDto;
        }).collect(Collectors.toList());
        return dto;
    }

    @ApiModelProperty(value = "The factor's name, or none when the files do not name it")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "The factor on the instance, when the experiment already has it")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "NEW (created with its levels), INCOMPLETE (edited, to add levels) or COMPLETE",
            example = "NEW")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @ApiModelProperty(value = "The levels the factor form starts with: those the factor has, then those the files add")
    public List<LevelDTO> getLevels() {
        return levels;
    }

    public void setLevels(List<LevelDTO> levels) {
        this.levels = levels;
    }
}
