//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * The user saying which existing resource a misspelt name of the file means.
 * <p>
 * Everything travels in the body, the conversation included: a body alongside a path parameter
 * produces a TypeScript client whose signature does not compile.
 *
 * @author Arnaud Charleroy
 */
public class MatchConfirmationDTO {

    @NotNull
    @JsonProperty("session_id")
    private String sessionId;

    /**
     * The report category, with the key the interface uses: {@code germplasm}, {@code variables}…
     */
    @NotNull
    private String category;

    /**
     * The name as the file writes it.
     */
    @NotNull
    private String value;

    /**
     * The resource it means. Must be one of the suggestions the report made for that name; the
     * user picks among what was proposed, nothing else.
     */
    private URI uri;

    @ApiModelProperty(value = "The conversation", required = true)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @ApiModelProperty(value = "Report category", required = true, example = "germplasm")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @ApiModelProperty(value = "The name as the file writes it", required = true,
            example = "Chardonay")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @ApiModelProperty(value = "The suggested resource it means; not needed to forget a match",
            example = "http://opensilex.test/id/germplasm/chardonnay")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
