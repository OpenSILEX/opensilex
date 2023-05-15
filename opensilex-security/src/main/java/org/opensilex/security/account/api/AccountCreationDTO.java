package org.opensilex.security.account.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

import java.net.URI;

public class AccountCreationDTO extends AccountWithoutHolderDTO {

    protected String password;

    protected URI holderOfTheAccountURI;

    @Override
    @Required
    @ApiModelProperty(value = "Account email", example = "jean.michel@example.com")
    public String getEmail() {
        return email;
    }

    @Required
    @ApiModelProperty(value = "Account password", example = "mdpInviolable1")
    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    @ValidURI
    @ApiModelProperty(value = "URI of the Person who will hold this account", example = "http://opensilex.dev/person#Jean.Michel.mistea")
    public URI getHolderOfTheAccountURI() {
        return holderOfTheAccountURI;
    }

    public void setHolderOfTheAccountURI(URI uri) {
        this.holderOfTheAccountURI = uri;
    }
}
