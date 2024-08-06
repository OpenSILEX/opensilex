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

  @PropSync("groups")
  groupsURI;

  @Prop()
  label;

  @Prop({
        default: "component.group.filter-placeholder"
  })
  placeholder;

  @Prop()
  noResultsText;

  @Prop()
  multiple;

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
