//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

/**
 * What the author changes of a template: its name, and whether it is shared. What is left out stays.
 *
 * @author Arnaud Charleroy
 */
public class FileTemplateUpdateDTO {

    private String id;
    private String name;
    private Boolean shared;

    @NotNull
    @ApiModelProperty(value = "The template", required = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "Whether other users can see and use it")
    public Boolean getShared() {
        return shared;
    }

    public void setShared(Boolean shared) {
        this.shared = shared;
    }
}
