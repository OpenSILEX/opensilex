package org.opensilex.security.user.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.OpenSilex;
import org.opensilex.security.account.api.AccountWithoutHolderDTO;

import java.net.URI;

public class UserDTO extends AccountWithoutHolderDTO {

    @Override
    @ApiModelProperty(value = "User URI", example = "http://opensilex.dev/users#jean.michel.inrae")
    public URI getUri() {
        return super.getUri();
    }

    @Override
    @ApiModelProperty(value = "User email", example = "jean.michel@example.com")
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    @ApiModelProperty(value = "User admin flag", example = "false")
    public boolean isAdmin() {
        return super.isAdmin();
    }

    @Override
    @ApiModelProperty(value = "User language", example = OpenSilex.DEFAULT_LANGUAGE)
    public String getLanguage() {
        return super.getLanguage();
    }

    @Override
    @ApiModelProperty(value = "User is enable", example = "true")
    public Boolean isEnable() {
        return enable;
    }
}
