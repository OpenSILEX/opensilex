package org.opensilex.security.account.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.security.account.dal.AccountModel;

import java.util.Objects;

public class AccountGetDTO extends AccountDTO{
    @JsonProperty("person_first_name")
    protected String personFirstName;

    @JsonProperty("person_last_name")
     protected String personLastName;

    @ApiModelProperty(value = "first name of the linked person")
    public String getPersonFirstName() {
        return personFirstName;
    }
    public void setPersonFirstName(String personFirstName) {
        this.personFirstName = personFirstName;
    }

    @ApiModelProperty(value = "last name of the linked person")
    public String getPersonLastName() {
        return personLastName;
    }

    public void setPersonLastName(String personLastName) {
        this.personLastName = personLastName;
    }

    public static AccountGetDTO fromModel(AccountModel accountModel) {
        AccountGetDTO accountGetDTO = new AccountGetDTO();
        accountGetDTO.uri = accountModel.getUri();
        accountGetDTO.email = accountModel.getEmail().toString();
        accountGetDTO.admin = accountModel.isAdmin();
        accountGetDTO.enable = accountModel.getIsEnabled();
        accountGetDTO.language = accountModel.getLanguage();
        accountGetDTO.favorites = accountModel.getFavorites();

        if ( Objects.nonNull(accountModel.getLinkedPerson()) ) {
            accountGetDTO.linkedPerson = accountModel.getLinkedPerson().getUri();
            accountGetDTO.personFirstName = accountModel.getLinkedPerson().getFirstName();
            accountGetDTO.personLastName = accountModel.getLinkedPerson().getLastName();
        }
        return accountGetDTO;
    }
}
