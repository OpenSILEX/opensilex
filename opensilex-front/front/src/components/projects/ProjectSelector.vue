<template>
  <div>
    <opensilex-SelectForm
      ref="selectForm"
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
      @loadMoreItems="loadMoreItems(selectForm)"
    ></opensilex-SelectForm>
  </div>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { ProjectGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import SelectForm from "../../common/forms/SelectForm.vue";

@Component
export default class ProjectSelector extends Vue {
  $opensilex: any;
  pageSize = 10;

  @PropSync("projects")
  projectsURI;

  @Prop()
  label;

  @Prop()
  multiple;

  @Ref("selectForm") readonly selectForm!: SelectForm;

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
        this.pageSize
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

  loadMoreItems(ref){
    this.pageSize = 0;
    ref.refresh();
    this.$nextTick(() => {
      ref.openTreeselect();
    })
  }
}
</script>

<style scoped lang="scss">
</style>
