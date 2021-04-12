<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      :title="name"
      description="component.project.project"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true">
      <template v-slot>
        <b-nav-item
          :active="isDetailsTab()"
          :to="{ path: '/project/details/' + encodeURIComponent(uri) }"
          >{{ $t("component.project.details") }}
        </b-nav-item>

        <b-nav-item
          :active="isAnnotationTab()"
          :to="{ path: '/project/annotations/' + encodeURIComponent(uri) }"
          >{{ $t("Annotation.list-title") }}
        </b-nav-item>

        <b-nav-item
          :active="isDocumentTab()"
          :to="{ path: '/project/documents/' + encodeURIComponent(uri) }"
          >{{ $t("component.project.documents") }}
        </b-nav-item>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ProjectDescription
          v-if="isDetailsTab()"
          :uri="uri"
        ></opensilex-ProjectDescription>

        <opensilex-DocumentTabList
            v-else-if="isDocumentTab()"
            :modificationCredentialId="credentials.CREDENTIAL_PROJECT_MODIFICATION_ID"
            :uri="uri"
        ></opensilex-DocumentTabList>

        <opensilex-AnnotationList
          v-else-if="isAnnotationTab()"
          ref="annotationList"
          :target="uri"
          :displayTargetColumn="false"
          :enableActions="true"
          :modificationCredentialId="
            credentials.CREDENTIAL_PROJECT_MODIFICATION_ID
          "
          :deleteCredentialId="credentials.CREDENTIAL_PROJECT_DELETE_ID"
        ></opensilex-AnnotationList>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { ProjectGetDetailDTO, ProjectsService } from "opensilex-core/index";
import AnnotationList from "../annotations/list/AnnotationList.vue";

@Component
export default class ProjectDetails extends Vue {
  $opensilex: any;
  $route: any;

  service: ProjectsService;
  uri = null;
  name: string = "";

  @Ref("annotationList") readonly annotationList!: AnnotationList;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

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

  isAnnotationTab() {
    return this.$route.path.startsWith("/project/annotations/");
  }
}
</script>

<style lang="scss">
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