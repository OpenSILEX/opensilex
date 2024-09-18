<!--
  - ******************************************************************************
  -                         GroupSelector.vue
  - OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
  - Copyright © INRAE 2024.
  - Last Modification: 23/08/2024 11:18
  - Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
  -
  - ******************************************************************************
  -->

<template>
  <opensilex-FormSelector
    :label="label"
    :selected.sync="groupsURI"
    :multiple="multiple"
    :itemLoadingMethod="loadGroups"
    :searchMethod="searchGroups"
    :conversionMethod="groupToSelectNode"
    :placeholder="placeholder"
    :noResultsText="noResultsText"
    :helpMessage="helpMessage"
    @select="select"
    @deselect="deselect"
  ></opensilex-FormSelector>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { SecurityService, GroupDTO } from "opensilex-security/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";

@Component
export default class GroupSelector extends Vue {
  $opensilex: any;
  service: SecurityService;

  //#region Props
  @PropSync("groups")
  private groupsURI;

  @Prop()
  private readonly label;

  @Prop({
    default: "component.group.filter-placeholder"
  })
  private readonly placeholder;

  @Prop()
  private readonly noResultsText;

  @Prop()
  private readonly multiple;

  @Prop()
  private readonly helpMessage: string;

  //#endregion

  searchGroups(searchQuery, page, pageSize) {
    return this.$opensilex
        .getService("opensilex.SecurityService")
        .searchGroups(searchQuery, undefined, page, pageSize)
  }

  loadGroups(groupsURI) {
    return this.$opensilex
      .getService("opensilex.SecurityService")
      .getGroupsByURI(groupsURI)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<GroupDTO>>>) =>
          http.response.result
      );
  }

  groupToSelectNode(dto: GroupDTO) {
    return {
      label: dto.name,
      id: dto.uri
    };
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }
}
</script>

<style scoped lang="scss">
</style>
