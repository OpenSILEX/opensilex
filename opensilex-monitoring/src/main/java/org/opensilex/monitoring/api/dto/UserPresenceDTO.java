//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

/**
 * Who is on the instance right now, by two different measurements.
 *
 * <p>A bean and not a record: the swagger generator introspects properties by bean
 * convention, and a record's accessors would produce an empty model and a broken
 * TypeScript client.</p>
 *
 * @author Arnaud Charleroy
 */
public class UserPresenceDTO {

    @ApiModelProperty(value = "accounts holding a live token")
    @JsonProperty("connected_accounts")
    private Integer connectedAccounts;

    @ApiModelProperty(value = "false when multi connection is allowed, in which case the token registry is never pruned and the count is only an upper bound")
    @JsonProperty("connected_accounts_reliable")
    private boolean connectedAccountsReliable;

    @ApiModelProperty(value = "distinct accounts that called within the activity window")
    @JsonProperty("active_accounts")
    private Integer activeAccounts;

    @ApiModelProperty(value = "length of that window")
    @JsonProperty("active_window_minutes")
    private Integer activeWindowMinutes;

    @ApiModelProperty(value = "the connected accounts themselves")
    @JsonProperty("accounts")
    private List<ConnectedUserDTO> accounts;

    public Integer getConnectedAccounts() {
        return connectedAccounts;
    }

    public void setConnectedAccounts(Integer connectedAccounts) {
        this.connectedAccounts = connectedAccounts;
    }

    public boolean isConnectedAccountsReliable() {
        return connectedAccountsReliable;
    }

    public void setConnectedAccountsReliable(boolean connectedAccountsReliable) {
        this.connectedAccountsReliable = connectedAccountsReliable;
    }

    public Integer getActiveAccounts() {
        return activeAccounts;
    }

    public void setActiveAccounts(Integer activeAccounts) {
        this.activeAccounts = activeAccounts;
    }

    public Integer getActiveWindowMinutes() {
        return activeWindowMinutes;
    }

    public void setActiveWindowMinutes(Integer activeWindowMinutes) {
        this.activeWindowMinutes = activeWindowMinutes;
    }

    public List<ConnectedUserDTO> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<ConnectedUserDTO> accounts) {
        this.accounts = accounts;
    }

}
