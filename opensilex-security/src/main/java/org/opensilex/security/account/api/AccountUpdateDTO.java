package org.opensilex.security.account.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.server.rest.validation.ValidURI;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class AccountUpdateDTO extends AccountDTO {

    protected String password;

    @Override
    @ValidURI
    @NotNull
    public URI getUri() { return super.getUri(); }

    @ApiModelProperty(value = "Account password", example = "mdpInviolable1")
    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }
}
