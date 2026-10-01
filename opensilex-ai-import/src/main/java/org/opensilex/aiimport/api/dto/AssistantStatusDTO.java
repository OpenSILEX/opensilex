//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import io.swagger.annotations.ApiModelProperty;

/**
 * Whether the language model can hold a conversation. The page folds the conversation away when it
 * cannot; everything else — the report, the mapping, the creation forms — works without it.
 *
 * @author Arnaud Charleroy
 */
public class AssistantStatusDTO {

    private boolean configured;
    private boolean reachable;

    public static AssistantStatusDTO of(boolean configured, boolean reachable) {
        AssistantStatusDTO dto = new AssistantStatusDTO();
        dto.configured = configured;
        dto.reachable = reachable;
        return dto;
    }

    @ApiModelProperty(value = "An endpoint and a model are configured", example = "true")
    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    @ApiModelProperty(value = "The endpoint answered, so a question can be asked", example = "true")
    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }
}
