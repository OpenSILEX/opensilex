<template>
  <div>
    <opensilex-SelectForm
      :label="label"
      :selected.sync="projectsURI"
      :multiple="multiple"
      :itemLoadingMethod="loadProjects"
      :searchMethod="searchProjects"
      :conversionMethod="projectToSelectNode"
      placeholder="component.project.selector-placeholder"
      noResultsText="component.project.selector-search-no-result"
      @select="select"
      @deselect="deselect"
    ></opensilex-SelectForm>
  </div>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { ProjectGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";

@Component
export default class ProjectSelector extends Vue {
  $opensilex: any;

  @PropSync("projects")
  projectsURI;

  @Prop()
  label;

  @Prop()
  multiple;

  searchProjects(searchQuery, page, pageSize) {
    return this.$opensilex
      .getService("opensilex.ProjectsService")
      .searchProjects(
        searchQuery, //name
        undefined,
        undefined, 
        undefined,
        undefined,
        page,
        pageSize
      )
  }

  loadProjects(projectsURI) {
    return this.$opensilex
      .getService("opensilex.ProjectsService")
      .getProjectsByURI(projectsURI)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ProjectGetDTO>>>) =>
          http.response.result
      );
  }

  projectToSelectNode(dto: ProjectGetDTO) {
    return {
      label: dto.shortname || dto.name,
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
