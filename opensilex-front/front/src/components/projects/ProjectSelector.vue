<template>
<div>
  <opensilex-SelectForm
    :label="label"
    :selected.sync="projectsURI"
    :multiple="multiple"
    :itemLoadingMethod="loadProjects"
    :searchMethod="searchProjects"
    :conversionMethod="projectToSelectNode"
    placeholder="component.project.filter-placeholder"
    noResultsText="component.project.filter-search-no-result"
    @select="select"
    @deselect="deselect"
  ></opensilex-SelectForm>
</div>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue, { PropOptions } from "vue";
import { ProjectsService, ProjectGetDTO } from "opensilex-core/index";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-core/HttpResponse";

@Component
export default class ProjectSelector extends Vue {
  $opensilex: any;

  @PropSync("projects")
  projectsURI;

  @Prop()
  label;

  @Prop()
  multiple;

  searchProjects(searchQuery) {
    return this.$opensilex
      .getService("opensilex.ProjectsService")
      .searchProjects(searchQuery)
      .then(
        (http: HttpResponse<OpenSilexResponse<Array<ProjectGetDTO>>>) =>
          http.response.result
      );
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
      label: dto.shortname || dto.label,
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
