package org.opensilex.security.user.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.OpenSilex;

import java.net.URI;

public class UserDTO {

    protected URI uri;

    protected String email;

    protected boolean admin;

    protected String language;

    @ApiModelProperty(value = "User URI", example = "http://opensilex.dev/users#jean.michel.inrae")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(value = "User email", example = "jean.michel@example.com")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @ApiModelProperty(value = "User admin flag", example = "false")
    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    @ApiModelProperty(value = "User language", example = OpenSilex.DEFAULT_LANGUAGE)
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
