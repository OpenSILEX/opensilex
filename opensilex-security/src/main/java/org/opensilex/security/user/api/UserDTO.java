package org.opensilex.security.user.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.opensilex.OpenSilex;
import org.opensilex.security.account.api.AccountDTO;

import java.net.URI;

public class UserDTO extends AccountDTO {

    @Override
    @Schema(description = "User URI", example = "http://opensilex.dev/users#jean.michel.inrae")
    public URI getUri() {
        return super.getUri();
    }

    @Override
    @Schema(description = "User email", example = "jean.michel@example.com")
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    @Schema(description = "User admin flag", example = "false")
    public boolean isAdmin() {
        return super.isAdmin();
    }

    @Override
    @Schema(description = "User language", example = OpenSilex.DEFAULT_LANGUAGE)
    public String getLanguage() {
        return super.getLanguage();
    }

    @Override
    @Schema(description = "User is enable", example = "true")
    public Boolean isEnable() {
        return enable;
    }
}
