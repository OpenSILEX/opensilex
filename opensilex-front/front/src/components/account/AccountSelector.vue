<template>
  <opensilex-SelectForm
    :label="label"
    :helpMessage="helpMessage"
    :selected.sync="usersURI"
    :multiple="multiple"
    :itemLoadingMethod="loadAccounts"
    :searchMethod="searchAccounts"
    :conversionMethod="userToSelectNode"
    placeholder="component.account.filter-placeholder"
    noResultsText="component.account.filter-search-no-result"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue, { PropOptions } from "vue";
import { SecurityService } from "opensilex-security/index";
import {AccountGetDTO} from "opensilex-security/model/accountGetDTO";

@Component
export default class AccountSelector extends Vue {
  $opensilex: any;
  service: SecurityService;

  @PropSync("users")
  usersURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  helpMessage: string;

  loadAccounts(accountsURIs) {
    return this.$opensilex
      .getService("opensilex.SecurityService")
      .getAccountsByURI(accountsURIs)
      .then(
        (http) =>
          http.response.result
      );
  }

  searchAccounts(searchQuery, page, pageSize) {
    return this.$opensilex
      .getService("opensilex.SecurityService")
      .searchAccounts(searchQuery, undefined, page, pageSize);
  }

  userToSelectNode(dto: AccountGetDTO) {
    let personName = dto.linked_person ? dto.person_first_name + " " + dto.person_last_name : ""
    let accountLabel = personName + " <" + dto.email + ">";
    return {
      label: accountLabel,
      id: dto.uri
    };
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }

  onEnter() {
    this.$emit("handlingEnterKey")
  }
}
</script>

<style scoped lang="scss">
</style>
