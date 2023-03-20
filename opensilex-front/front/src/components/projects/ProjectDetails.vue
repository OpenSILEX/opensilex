<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      :title="name"
      class="detail-element-header"
      description="component.project.project"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true" class="ProjectDetailsTabs">
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
            <span
            v-if="!annotationsCountIsLoading && annotations > 0"
            class ="tabWithElements"
          >
            {{$opensilex.$numberFormatter.formateResponse(annotations)}}
          </span>
        </b-nav-item>

        <b-nav-item
          :active="isDocumentTab()"
          :to="{ path: '/project/documents/' + encodeURIComponent(uri) }"
          >{{ $t("component.project.documents") }}
          <span
            v-if="!documentsCountIsLoading && documents > 0"
            class ="tabWithElements"
          >
            {{$opensilex.$numberFormatter.formateResponse(documents)}}
          </span>
        </b-nav-item>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <opensilex-ProjectDescription
        class="projectDescription"
          v-if="isDetailsTab()"
          :uri="uri"
        ></opensilex-ProjectDescription>

        <opensilex-DocumentTabList
            class="projectDocuments"
            v-else-if="isDocumentTab()"
            :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
            :uri="uri"
            :debounce="300"
            :lazy="false"
        ></opensilex-DocumentTabList>

        <opensilex-AnnotationList
          class="projectAnnotations"
          v-else-if="isAnnotationTab()"
          ref="annotationList"
          :target="uri"
          :displayTargetColumn="false"
          :enableActions="true"
          :modificationCredentialId="
            credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID
          "
          :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
        ></opensilex-AnnotationList>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
// @ts-ignore
import { ProjectGetDetailDTO, ProjectsService } from "opensilex-core/index";
import AnnotationList from "../annotations/list/AnnotationList.vue";
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {DocumentsService} from "opensilex-core/api/documents.service";

@Component
export default class ProjectDetails extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $route: any;

  service: ProjectsService;
  $AnnotationsService: AnnotationsService
  $DocumentsService: DocumentsService
  uri = null;
  name: string = "";

  annotations: number;
  documents: number;

  annotationsCountIsLoading: boolean = true;
  documentsCountIsLoading: boolean = true;

  @Ref("annotationList") readonly annotationList!: AnnotationList;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.service = this.$opensilex.getService<ProjectsService>("opensilex.ProjectsService");
    this.$AnnotationsService = this.$opensilex.getService<AnnotationsService>("opensilex.AnnotationsService");
    this.$DocumentsService = this.$opensilex.getService<DocumentsService>("opensilex.DocumentsService");
    this.loadProject(this.uri);
    this.searchAnnotations();
    this.searchDocuments();
  }

  loadProject(uri: string) {
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

  searchAnnotations() {
    return this.$AnnotationsService
      .countAnnotations(
        this.uri,
        undefined,
        undefined
      ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
        if(http && http.response){
          this.annotations = http.response.result as number;
          this.annotationsCountIsLoading = false;
          return this.annotations
        }
      }
    ).catch(this.$opensilex.errorHandler);
  }

    searchDocuments(){
    return this.$DocumentsService
      .countDocuments(
        this.uri, // target filter
        undefined,
        undefined
      ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
        if(http && http.response){
          this.documents = http.response.result as number;
          this.documentsCountIsLoading = false;
          return this.documents
        }
      }).catch(this.$opensilex.errorHandler);
  }
}
</script>

<style lang="scss">
.projectDescription, .projectAnnotations, .projectDocuments{
  margin-top: 18px; 
}

.ProjectDetailsTabs {
  margin-bottom: -9px
}
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