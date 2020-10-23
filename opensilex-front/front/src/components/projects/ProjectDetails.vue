<template>
  <div class="container-fluid">
    <opensilex-PageHeader :title="name" description="component.project.project"></opensilex-PageHeader>

    <opensilex-PageActions :returnButton="true">
        <template v-slot>
        <b-nav-item
          :active="isDetailsTab()"
          :to="{path: '/project/details/' + encodeURIComponent(uri)}"
        >{{ $t('component.project.details') }}</b-nav-item>
        
        <b-nav-item
          :active="isDocumentTab()"
          :to="{path: '/project/documents/' + encodeURIComponent(uri)}"
        >{{ $t('component.project.documents') }}</b-nav-item>-->
      </template>
    </opensilex-PageActions>
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ProjectDescription v-if="isDetailsTab()" :uri="uri"></opensilex-ProjectDescription>
        <opensilex-ProjectDocuments v-else-if="isDocumentTab()" :uri="uri"></opensilex-ProjectDocuments>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import VueRouter from "vue-router";
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { ProjectGetDetailDTO, ProjectsService } from "opensilex-core/index";
@Component
export default class ProjectDetails extends Vue {
  $opensilex: any;
  $route: any;

  service: ProjectsService;
  uri = null;
  name: string = "";

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.service = this.$opensilex.getService("opensilex.ProjectsService");

    this.service
      .getProject(this.uri)
      .then((http: HttpResponse<OpenSilexResponse<ProjectGetDetailDTO>>) => {
        this.name = http.response.result.name;
      })
      .catch(this.$opensilex.errorHandler);
  }

  isDetailsTab() {
    return this.$route.path.startsWith("/project/details/");
  }

  isDocumentTab() {
    return this.$route.path.startsWith("/project/documents/");
  }
}
</script>

<style  lang="scss">
</style>

<i18n>
en:
    ProjectDetails:
        title: Detailled project view
        advanced: Advanced informations
fr:
    ProjectDetails:
        title: Vue détaillée du projet
        advanced: Informations avancées
</i18n>
