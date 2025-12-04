<template>
  <div class="container-fluid" v-if="geneticResource.uri">
    <opensilex-PageHeader
      icon="fa#seedling"
      :title="geneticResource.name"
      description="GeneticResourceDetails.title"
      class="detail-element-header"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs=true :returnButton="true">
      <b-nav-item
      :active="isDetailsTab()"
      :to="{path: '/geneticResource/details/' + encodeURIComponent(uri)}"
      >{{ $t('component.common.details-label') }}
      </b-nav-item>

      <b-nav-item
      :active="isAnnotationTab()"
      :to="{ path: '/geneticResource/annotations/' + encodeURIComponent(uri) }"
      >{{ $t("Annotation.list-title") }}
        <span
          v-if="!annotationsCountIsLoading && annotations > 0"
          class="tabWithElements"
        >
          {{$opensilex.$numberFormatter.formateResponse(annotations)}}
        </span>
      </b-nav-item>

      <b-nav-item
      :active="isDocumentTab()"
      :to="{path: '/geneticResource/documents/' + encodeURIComponent(uri)}"
      >{{ $t('component.project.documents') }}
        <span
          v-if="!documentsCountIsLoading && documents > 0"
          class="tabWithElements"
        >
          {{$opensilex.$numberFormatter.formateResponse(documents)}}
        </span>
      </b-nav-item>

    </opensilex-PageActions>

    <opensilex-PageContent>
      <b-row v-if="isDetailsTab()">
        <b-col>
          <opensilex-Card label="component.common.informations" icon="ik#ik-clipboard">
            <template v-slot:rightHeader>              
                <opensilex-EditButton
                  v-if="user.hasCredential(credentials.CREDENTIAL_GENETIC_RESOURCE_MODIFICATION_ID)"
                  @click="updateGeneticResource"
                ></opensilex-EditButton>
                <opensilex-DeleteButton
                  v-if="!geneticResource.rdf_type.endsWith('Species') && user.hasCredential(credentials.CREDENTIAL_GENETIC_RESOURCE_DELETE_ID)"
                  @click="deleteGeneticResource"
                ></opensilex-DeleteButton>
            </template>
            <template v-slot:body>
              <opensilex-UriView
                v-if="geneticResource.uri.startsWith('http')"
                :uri="geneticResource.uri"
                :url="geneticResource.uri"
              ></opensilex-UriView>
              <opensilex-UriView v-else :uri="geneticResource.uri" ></opensilex-UriView>
              <opensilex-StringView label="GeneticResourceDetails.rdfType" :value="geneticResource.rdf_type_name"></opensilex-StringView>
              <opensilex-StringView label="GeneticResourceDetails.name" :value="geneticResource.name"></opensilex-StringView>
              <opensilex-StringView
                v-if="geneticResource.synonyms.length>0 && (geneticResource.rdf_type.endsWith('Accession') || geneticResource.rdf_type.endsWith('Variety'))"
                label="GeneticResourceDetails.subtaxa"
                :value="geneticResource.synonyms.toString()"
              ></opensilex-StringView>
              <opensilex-StringView
                v-if="geneticResource.synonyms.length>0 && !geneticResource.rdf_type.endsWith('Accession') && !geneticResource.rdf_type.endsWith('Variety')"
                label="GeneticResourceDetails.synonyms"
                :value="geneticResource.synonyms.toString()"
              ></opensilex-StringView>
              <opensilex-StringView
                v-if="geneticResource.code != null"
                label="GeneticResourceDetails.code"
                :value="geneticResource.code"
              ></opensilex-StringView>
              <opensilex-StringView
                v-if="geneticResource.institute != null"
                label="GeneticResourceDetails.institute"
                :value="geneticResource.institute"
              ></opensilex-StringView>
              <opensilex-UriView
                v-if="geneticResource.website != null"
                title="GeneticResourceDetails.website"
                :value="geneticResource.website"
                :uri="geneticResource.website"
              ></opensilex-UriView>
              <opensilex-StringView
                v-if="geneticResource.production_year != null"
                label="GeneticResourceDetails.year"
                :value="geneticResource.production_year"
              ></opensilex-StringView>
              <opensilex-StringView
                v-if="geneticResource.description != null"
                label="GeneticResourceDetails.comment"
                :value="geneticResource.description"
              ></opensilex-StringView>
              <opensilex-LabelUriView
                v-if="(geneticResource.species_name != null) || (geneticResource.species != null)"
                label="GeneticResourceDetails.species"
                :value="geneticResource.species_name"
                :uri="geneticResource.species"
                :to="{path: '/geneticResource/details/'+ encodeURIComponent(geneticResource.species)}"
              ></opensilex-LabelUriView>
              <opensilex-LabelUriView
                v-if="(geneticResource.variety_name != null) || (geneticResource.variety != null)"
                label="GeneticResourceDetails.variety"
                :value="geneticResource.variety_name"
                :uri="geneticResource.variety"
                :to="{path: '/geneticResource/details/'+ encodeURIComponent(geneticResource.variety)}"
              ></opensilex-LabelUriView>

              <opensilex-UriListView
                v-if="(geneticResource.groups.length > 0)"
                label="GeneticResourceDetails.groups"
                :list="groupsList"
              ></opensilex-UriListView>

                <opensilex-BooleanView
                  v-if="(geneticResource.is_public != null)"
                  label="GeneticResourceDetails.is_public"
                  :value="geneticResource.is_public"
                ></opensilex-BooleanView>

              <opensilex-LabelUriView
                v-if="(geneticResource.accession_name != null) || (geneticResource.accession != null)"
                label="GeneticResourceDetails.accession"
                :value="geneticResource.accession_name"
                :uri="geneticResource.accession"
                :to="{path: '/geneticResource/details/'+ encodeURIComponent(geneticResource.accession)}"
              ></opensilex-LabelUriView>
              <!-- GeneticResource Parents -->
              <opensilex-UriListView
                  label="GeneticResourceDetails.parent"
                  :list="parentList"
                  :inline="false"
                  v-if="geneticResource.has_parent_geneticResource!==null && geneticResource.has_parent_geneticResource.length>0"
              ></opensilex-UriListView>
              <!-- GeneticResource Parents F -->
              <opensilex-UriListView
                  label="GeneticResourceDetails.parentF"
                  :list="parentFList"
                  :inline="false"
                  v-if="geneticResource.has_parent_geneticResource_f!==null && geneticResource.has_parent_geneticResource_f.length>0"
              ></opensilex-UriListView>
              <!-- GeneticResource Parents M -->
              <opensilex-UriListView
                  label="GeneticResourceDetails.parentM"
                  :list="parentMList"
                  :inline="false"
                  v-if="geneticResource.has_parent_geneticResource_m!==null && geneticResource.has_parent_geneticResource_m.length>0"
              ></opensilex-UriListView>
              <!-- Metadata -->
              <opensilex-MetadataView
              v-if="geneticResource.publisher && geneticResource.publisher.uri"
                :publisher="geneticResource.publisher"
                :publicationDate="geneticResource.publication_date"
                :lastUpdatedDate="geneticResource.last_updated_date" 
              ></opensilex-MetadataView>
            </template>
          </opensilex-Card>
          <opensilex-Card label="GeneticResourceDetails.additionalInfo" icon="ik#ik-clipboard" v-if="addInfo.length != 0">
            <template v-slot:body>
              <b-table
                ref="tableAtt"
                striped
                hover
                small
                responsive
                :fields="attributeFields"
                :items="addInfo"
              >
                <template v-slot:head(attribute)="data">{{$t(data.label)}}</template>
                <template v-slot:head(value)="data">{{$t(data.label)}}</template>
              </b-table>
            </template>
          </opensilex-Card>
        </b-col>
        <b-col>
          <opensilex-AssociatedExperimentsList
            :searchMethod="loadExperiments"
            :nameFilter.sync="experimentName"
          ></opensilex-AssociatedExperimentsList>
          <opensilex-AssociatedGeneticResourceGroupsList
              :searchMethod="loadGeneticResourceGroups"
              :nameFilter.sync="geneticResourceGroupName"
          ></opensilex-AssociatedGeneticResourceGroupsList>
        </b-col>
      </b-row>

      <opensilex-DocumentTabList
        v-else-if="isDocumentTab()"
        ref="documentTabList"
        :uri="uri"        
        :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
      ></opensilex-DocumentTabList>

      <opensilex-AnnotationList
      v-else-if="isAnnotationTab()"
      ref="annotationList"
      :target="uri"
      :displayTargetColumn="false"
      :enableActions="true"
      :modificationCredentialId="credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID"
      :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
      ></opensilex-AnnotationList>

    </opensilex-PageContent>

    <opensilex-ModalForm      
      ref="geneticResourceForm"
      component="opensilex-GeneticResourceForm"
      createTitle="GeneticResourceView.add"
      editTitle="GeneticResourceView.update"
      icon="fa#seedling"
      modalSize="lg"
      @onUpdate="loadGeneticResource()"
    ></opensilex-ModalForm>    

  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
// @ts-ignore
import { GeneticResourceGetSingleDTO, GeneticResourceUpdateDTO, GeneticResourceService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import AnnotationList from "../annotations/list/AnnotationList.vue";
import DocumentTabList from "../documents/DocumentTabList.vue";
import GeneticResourceForm from "./GeneticResourceForm.vue";

import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {DocumentsService} from "opensilex-core/api/documents.service";
import {GroupDTO, SecurityService} from "opensilex-security/index";

@Component
export default class GeneticResourceDetails extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $route: any;
  $store: any;
  $router: any;
  routeArr : string = this.$route.path.split('/');
  $t: any;
  $i18n: any;
  service: GeneticResourceService;

  uri: string = null;
  addInfo = [];
  experimentName: any = "";
  geneticResourceGroupName = "";
  groupsList = [];

  $AnnotationsService: AnnotationsService
  $DocumentsService: DocumentsService

  annotations: number;
  documents: number;

  annotationsCountIsLoading: boolean = true;
  documentsCountIsLoading: boolean = true;

  @Ref("modalRef") readonly modalRef!: any;
  @Ref("annotationList") readonly annotationList!: AnnotationList;
  @Ref("documentTabList") readonly documentTabList!: DocumentTabList;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  get parentList() {
    return this.geneticResource.has_parent_geneticResource.map(parent => {
      return {
        uri: parent.uri,
        value: parent.name,
        to: {
          path: "/geneticResource/details/" + encodeURIComponent(parent.uri)
        }
      };
    });
  }
  get parentMList() {
    return this.geneticResource.has_parent_geneticResource_m.map(parent => {
      return {
        uri: parent.uri,
        value: parent.name,
        to: {
          path: "/geneticResource/details/" + encodeURIComponent(parent.uri)
        }
      };
    });
  }
  get parentFList() {
    return this.geneticResource.has_parent_geneticResource_f.map(parent => {
      return {
        uri: parent.uri,
        value: parent.name,
        to: {
          path: "/geneticResource/details/" + encodeURIComponent(parent.uri)
        }
      };
    });
  }

  isDetailsTab() {
      localStorage.setItem("tabPath", this.routeArr[2]);
      localStorage.setItem("tabPage", "1");
      return this.$route.path.startsWith("/geneticResource/details/");    
  }

  isDocumentTab() {
      return this.$route.path.startsWith("/geneticResource/documents/");
  }

  isAnnotationTab() {
      return this.$route.path.startsWith("/geneticResource/annotations/");
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.loadGeneticResource();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }    

  geneticResource: GeneticResourceGetSingleDTO = {
    uri: null,
    name: null,
    rdf_type: null,
    rdf_type_name: null,
    species: null,
    species_name: null,
    variety: null,
    variety_name: null,
    accession: null,
    accession_name: null,
    is_public : null,
    institute: null,
    code: null,
    production_year: null,
    description: null,
    metadata: null,
    website: null,
    has_parent_geneticResource: [],
    has_parent_geneticResource_m: [],
    has_parent_geneticResource_f: [],
    synonyms: [],
    groups : []
  };

  created() {
    this.service = this.$opensilex.getService<GeneticResourceService>("opensilex.GeneticResourceService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.loadGeneticResource();
    this.$AnnotationsService = this.$opensilex.getService<AnnotationsService>("opensilex.AnnotationsService");
    this.$DocumentsService = this.$opensilex.getService<DocumentsService>("opensilex.DocumentsService");
    this.searchAnnotations();
    this.searchDocuments();
  }

  loadGeneticResource() {
    this.service
      .getGeneticResource(this.uri)
      .then((http: HttpResponse<OpenSilexResponse<GeneticResourceGetSingleDTO>>) => {
        this.geneticResource = http.response.result;
        this.loadExperiments;
        this.loadGroups();
        this.getAddInfo();
      })
      .catch(this.$opensilex.errorHandler);
  }

  expFields = [
    {
      key: "uri",
      label: "component.experiment.uri",
      sortable: true,
    },
    {
      key: "label",
      label: "component.experiment.label",
      sortable: true,
    },
  ];

  experiments = [];

  @Ref("table") readonly table!: any;

  loadExperiments(options) {
    return this.service.getGeneticResourceExperiments(
      this.uri,
      this.experimentName,
      options.orderBy,
      options.currentPage,
      options.pageSize
    );
  }

  loadGroups() {
    let service: SecurityService = this.$opensilex.getService(
      "opensilex.SecurityService"
    );
    this.groupsList = [];
    if (this.geneticResource.groups && this.geneticResource.groups.length > 0) {
      service
        .getGroupsByURI(this.geneticResource.groups)
        .then((http: HttpResponse<OpenSilexResponse<GroupDTO[]>>) => {
          this.groupsList = http.response.result.map((group) => {
            return {
              uri: group.uri,
              value: group.name,
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  loadGeneticResourceGroups(options) {
    return this.service.searchGeneticResourceGroups(
        undefined,
        [this.uri],
        options.orderBy,
        options.currentPage,
        options.pageSize
    );
  }

  attributeFields = [
    {
      key: "attribute",
      label: "GeneticResourceDetails.attribute",
    },
    {
      key: "value",
      label: "GeneticResourceDetails.value",
    },
  ];

  @Ref("tableAtt") readonly tableAtt!: any;

  getAddInfo() {
    this.addInfo = []
    for (const property in this.geneticResource.metadata) {
      let tableData = {
        attribute: property,
        value: this.geneticResource.metadata[property],
      };
      this.addInfo.push(tableData);
    }
  }

  @Ref("geneticResourceForm") readonly geneticResourceForm!: any;
  updateGeneticResource() {
    let form: GeneticResourceForm = this.geneticResourceForm.getFormRef();
    form.readAttributes(this.geneticResource.metadata);
    let updateDTO : GeneticResourceUpdateDTO = GeneticResourceForm.readDuplicatableRelations(this.geneticResource);
    this.geneticResourceForm.showEditForm(updateDTO);
  }

    toggleUpdatePublicStatus() {
        let updateDTO : GeneticResourceUpdateDTO = GeneticResourceForm.readDuplicatableRelations(this.geneticResource);
        this.service
          .updateGeneticResource(updateDTO)
          .catch(this.$opensilex.errorHandler);

    }

  deleteGeneticResource() {
    this.service
      .deleteGeneticResource(this.geneticResource.uri)
      .then(() => {
        let message =
          this.$i18n.t("GeneticResourceView.title") +
          " " +
          this.geneticResource.uri +
          " " +
          this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
        this.$router.push({
            path: "/geneticResource"
          });
      })
      .catch(this.$opensilex.errorHandler);
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
    }).catch(this.$opensilex.errorHandler);
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
</style>

<i18n>

en:
  GeneticResourceDetails:
    title: Genetic Resource
    description: Detailed Information
    info: Genetic Resource Information
    experiment: Related experiments
    document: Associated documents
    uri: URI
    name: Name
    rdfType: Type
    species: Species
    variety: Variety
    accession: Accession
    institute: Institute
    year: Year
    comment: Description
    backToList: Go back to Genetic Resource list
    code: Code
    synonyms: Synonyms
    additionalInfo: Additional information
    attribute: Attribute
    value: Value
    subtaxa: Subtaxa
    website: Web site
    parent: Parent Genetic Resources
    parentM: Male parents
    parentF: Female parents
    is_public: Public
    groups: Groups

fr:
  GeneticResourceDetails:
    title: Ressource génétique
    description: Information détaillées
    info: Informations générales
    experiment: Expérimentations connexes
    document: Documents associées
    uri: URI
    name: Nom
    rdfType: Type
    species: Espèce
    variety: Variété
    accession: Accession
    institute: Institut
    year: Année
    comment: Description
    backToList: Retourner à la liste des ressources génétiques
    code: Code
    synonyms: Synonymes
    additionalInfo: Informations supplémentaires
    attribute: Attribut
    value: Valeur
    subtaxa: Subtaxa
    website: Site web
    parent: Parents
    parentM: Parents mâle
    parentF: Parents femelles
    is_public: Public
    groups: Groupes

</i18n>
