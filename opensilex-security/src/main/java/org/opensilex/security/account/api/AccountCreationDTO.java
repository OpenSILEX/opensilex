package org.opensilex.security.account.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.server.rest.validation.Required;

public class AccountCreationDTO extends AccountDTO {

    protected String password;

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

}
