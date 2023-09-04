<template>
  <div class="container-fluid">
    <opensilex-PageHeader
        icon="fa#sun"
        description="VariableView.type"
        :title="variable.name"
        class="detail-element-header"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true" class="navigationTabs">
      <template v-slot>
        <b-nav-item
            :active="isDetailsTab()"
            :to="{ path: getPath('details') }"
        >{{ $t('component.common.details-label') }}
        </b-nav-item>
        <b-nav-item
            :active="isAnnotationTab()"
            :to="{ path: getPath('annotations') }"
        >{{ $t("Annotation.list-title") }}
          <span
            v-if="!annotationsCountIsLoading && annotations > 0"
            class="tabWithElements"
          >
            {{$opensilex.$numberFormatter.formateResponse(annotations)}}
          </span>
        </b-nav-item>
        <b-nav-item
            v-if="onLocalInstance"
            :active="isVisualizationTab()"
            :to="{ path: getPath('visualization') }"
        >{{ $t('VariableDetails.visualization') }}
        </b-nav-item>
        <b-nav-item
            :active="isDocumentTab()"
            :to="{ path: getPath('documents') }"
        >{{ $t('component.project.documents') }}
          <span
            v-if="!documentsCountIsLoading && documents > 0"
            class="tabWithElements"
          >
            {{$opensilex.$numberFormatter.formateResponse(documents)}}
          </span>
        </b-nav-item>

      </template>
    </opensilex-PageActions>
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-VariableDetails
            v-if="isDetailsTab()"
            :variable="variable"
            :displayLocalActions="onLocalInstance"
            @onUpdate="updateVariable($event)"
        ></opensilex-VariableDetails>

        <opensilex-AnnotationList
        class="projectAnnotations"
          v-else-if="isAnnotationTab()"
          ref="annotationList"
          :target="uri"
          :displayTargetColumn="false"
          :enableActions="true"
          :modificationCredentialId="credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID"
          :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
        ></opensilex-AnnotationList>

        <opensilex-VariableVisualizationTab
          v-else-if="isVisualizationTab()"
          :variable="uri"
          :elementName="variable.name"
          :modificationCredentialId="credentials.CREDENTIAL_DEVICE_MODIFICATION_ID"
        ></opensilex-VariableVisualizationTab>

        <opensilex-DocumentTabList
          v-else-if="isDocumentTab()"
          :uri="uri"
          :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
        ></opensilex-DocumentTabList>
      </template>

    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {VariablesService} from "opensilex-core/api/variables.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import AnnotationList from "../../annotations/list/AnnotationList.vue";

import { VariableDetailsDTO } from 'opensilex-core/index';
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {DocumentsService} from "opensilex-core/api/documents.service";

@Component
export default class VariableView extends Vue {

  $opensilex: OpenSilexVuePlugin;
  service: VariablesService;
  $store: any;
  $route: any;
  $router: any;

  $t: any;
  $i18n: any;

  $AnnotationsService: AnnotationsService
  $DocumentsService: DocumentsService

  annotations: number;
  documents: number;

  annotationsCountIsLoading: boolean = true;
  documentsCountIsLoading: boolean = true;

  static getEmptyDetailsDTO() : VariableDetailsDTO{
    return {
        uri: undefined,
        alternative_name: undefined,
        name: undefined,
        entity: undefined,
        entity_of_interest: undefined,
        characteristic: undefined,
        description: undefined,
        time_interval: undefined,
        sampling_interval: undefined,
        datatype: undefined,
        trait: undefined,
        trait_name: undefined,
        method: undefined,
        unit: undefined,
        exact_match: [],
        close_match: [],
        broad_match: [],
        narrow_match: [],
        species: undefined,
        publisher: undefined,
        publication_date: undefined,
        last_updated_date: undefined
    };
  }


  variable: VariableDetailsDTO = VariableView.getEmptyDetailsDTO();
  onLocalInstance: boolean = true;
  uri: string;
  resource: string;

  @Ref("annotationList") readonly annotationList!: AnnotationList;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.service = this.$opensilex.getService<VariablesService>("opensilex.VariablesService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    const encodedSriUrl = this.$route.query["sharedResourceInstance"];
    const sriUrl = encodedSriUrl ? decodeURIComponent(encodedSriUrl) : undefined;
    this.onLocalInstance = sriUrl === undefined;
    this.loadVariable(this.uri, sriUrl);
    this.$AnnotationsService = this.$opensilex.getService<AnnotationsService>("opensilex.AnnotationsService");
    this.$DocumentsService = this.$opensilex.getService<DocumentsService>("opensilex.DocumentsService");
    this.searchAnnotations();
    this.searchDocuments();
  }

  getPath(tab: string): string {
    let path = "/variable/" + tab + "/" + encodeURIComponent(this.uri);
    const sri = this.$route.query.sharedResourceInstance;
    if (sri) {
      return path + "?sharedResourceInstance=" + sri;
    } else {
      return path;
    }
  }

  isDetailsTab() {
    return this.$route.path.startsWith("/variable/details/");
  }

  isAnnotationTab() {
    return this.$route.path.startsWith("/variable/annotations/");
  }

  isVisualizationTab() {
    return this.$route.path.startsWith("/variable/visualization/");
  }

  isDocumentTab() {
    return this.$route.path.startsWith("/variable/documents/");
  }

  loadVariable(uri: string, resource: string) {
    this.service
        .getVariable(uri, resource)
        .then((http: HttpResponse<OpenSilexResponse<VariableDetailsDTO>>) => {
          this.variable = http.response.result;
        }).catch(this.$opensilex.errorHandler);
  }

  updateVariable(variable) {
    this.uri = variable.uri;
    this.loadVariable(this.uri, undefined);
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
        this.uri,
        undefined,
        undefined
      ).then((http: HttpResponse<OpenSilexResponse<number>>) => {
        if(http && http.response){
          this.documents = http.response.result as number;
          this.documentsCountIsLoading = false;
          return this.documents
        }
      }
    ).catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
.projectAnnotations{
  margin-top: 18px;
}

.navigationTabs {
  margin-bottom: -9px
}
</style>

