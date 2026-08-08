//******************************************************************************
//                        ApiLicenseInfoDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.system.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Class that represents informations about software license
 * @author Arnaud Charleroy
 */
@JsonPropertyOrder({"name", "url"})
public class ApiLicenseInfoDTO {

    private String name;

    private String url;

    public ApiLicenseInfoDTO() {
    }

    public ApiLicenseInfoDTO(String name, String url) {
        this.name = name;
        this.url = url;
    }

    @Schema(description = "GNU Affero General Public License v3", example = "GNU Affero General Public License v3")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Schema(description = "https://www.gnu.org/licenses/agpl-3.0.fr.html", example = "https://www.gnu.org/licenses/agpl-3.0.fr.html")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
