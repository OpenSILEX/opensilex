<template>
  <opensilex-SelectForm
    :label="label"
    :helpMessage="helpMessage"
    :selected.sync="usersURI"
    :multiple="multiple"
    :itemLoadingMethod="loadUsers"
    :searchMethod="searchUsers"
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
// @ts-ignore
import { SecurityService, UserGetDTO } from "opensilex-security/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";

@Component
export default class UserSelector extends Vue {
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

  loadUsers(usersURI) {
    return this.$opensilex
      .getService("opensilex.SecurityService")
      .getUsersByURI(usersURI)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<UserGetDTO>>>) =>
          http.response.result
      );
  }

  searchUsers(searchQuery, page, pageSize) {
    return this.$opensilex
      .getService("opensilex.SecurityService")
      .searchUsers(searchQuery, undefined, page, pageSize);
  }

  userToSelectNode(dto: UserGetDTO) {
    let userLabel = dto.first_name + " " + dto.last_name + " <" + dto.email + ">";
    return {
      label: userLabel,
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
