
package org.opensilex.security.account.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.OpenSilex;
import org.opensilex.server.rest.validation.ValidURI;

import javax.validation.constraints.Email;
import java.net.URI;
import java.util.List;

public abstract class AccountDTO {

    protected URI uri;

    @Email
    protected String email;

    protected boolean admin;

    protected String language;

    protected Boolean enable;

    @JsonProperty("linked_person")
    protected URI linkedPerson;

    protected List<URI> favorites;

    @ValidURI
    @ApiModelProperty(value = "Account URI", example = "http://opensilex.dev/users#jean.michel.inrae")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(value = "Account email", example = "jean.michel@example.com")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @ApiModelProperty(value = "Account admin flag", example = "false")
    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    @ApiModelProperty(value = "Account language", example = OpenSilex.DEFAULT_LANGUAGE)
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @ApiModelProperty(value = "Account is enable", example = "true")
    public Boolean isEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    @ValidURI
    @ApiModelProperty(value = "URI of the Person linked to this account", example = "http://opensilex.dev/person#Jean.Michel.mistea")
    public URI getLinkedPerson() {
        return linkedPerson;
    }

    public void setLinkedPerson(URI uri) {
        this.linkedPerson = uri;
    }

    @ApiModelProperty(value = "Favorites URI")
    public List<URI> getFavorites() { return favorites; }

    public void setFavorites(List<URI> favorites) { this.favorites = favorites; }

}
