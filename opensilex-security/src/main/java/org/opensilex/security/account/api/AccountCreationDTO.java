package org.opensilex.security.account.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.opensilex.server.rest.validation.Required;

public class AccountCreationDTO extends AccountDTO {

    protected String password;

    @Override
    @Required
    @Schema(description = "Account email", example = "jean.michel@example.com")
    public String getEmail() {
        return email;
    }

    @Required
    @Schema(description = "Account password", example = "mdpInviolable1")
    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

}
