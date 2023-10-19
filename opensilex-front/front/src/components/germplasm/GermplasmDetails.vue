<template>
  <div class="container-fluid" v-if="germplasm.uri">
    <opensilex-PageHeader
      icon="fa#seedling"
      :title="germplasm.name"
      description="GermplasmDetails.title"
      class="detail-element-header"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs=true :returnButton="true">
      <b-nav-item
      :active="isDetailsTab()"
      :to="{path: '/germplasm/details/' + encodeURIComponent(uri)}"
      >{{ $t('component.common.details-label') }}
      </b-nav-item>

      <b-nav-item
      :active="isAnnotationTab()"
      :to="{ path: '/germplasm/annotations/' + encodeURIComponent(uri) }"
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
      :to="{path: '/germplasm/documents/' + encodeURIComponent(uri)}"
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
                  v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
                  @click="updateGermplasm"
                ></opensilex-EditButton>
                <opensilex-DeleteButton
                  v-if="!germplasm.rdf_type.endsWith('Species') && user.hasCredential(credentials.CREDENTIAL_GERMPLASM_DELETE_ID)"
                  @click="deleteGermplasm"
                ></opensilex-DeleteButton>
            </template>
            <template v-slot:body>
              <opensilex-UriView
                v-if="germplasm.uri.startsWith('http')"
                :uri="germplasm.uri"
                :url="germplasm.uri"
              ></opensilex-UriView>
              <opensilex-UriView v-else :uri="germplasm.uri" ></opensilex-UriView>
              <opensilex-StringView label="GermplasmDetails.rdfType" :value="germplasm.rdf_type_name"></opensilex-StringView>
              <opensilex-StringView label="GermplasmDetails.name" :value="germplasm.name"></opensilex-StringView>
              <opensilex-StringView
                v-if="germplasm.synonyms.length>0 && (germplasm.rdf_type.endsWith('Accession') || germplasm.rdf_type.endsWith('Variety'))"
                label="GermplasmDetails.subtaxa"
                :value="germplasm.synonyms.toString()"
              ></opensilex-StringView>
              <opensilex-StringView
                v-if="germplasm.synonyms.length>0 && !germplasm.rdf_type.endsWith('Accession') && !germplasm.rdf_type.endsWith('Variety')"
                label="GermplasmDetails.synonyms"
                :value="germplasm.synonyms.toString()"
              ></opensilex-StringView>
              <opensilex-StringView
                v-if="germplasm.code != null"
                label="GermplasmDetails.code"
                :value="germplasm.code"
              ></opensilex-StringView>
              <opensilex-StringView
                v-if="germplasm.institute != null"
                label="GermplasmDetails.institute"
                :value="germplasm.institute"
              ></opensilex-StringView>
              <opensilex-UriView
                v-if="germplasm.website != null"
                title="GermplasmDetails.website"
                :value="germplasm.website"
                :uri="germplasm.website"
              ></opensilex-UriView>
              <opensilex-StringView
                v-if="germplasm.production_year != null"
                label="GermplasmDetails.year"
                :value="germplasm.production_year"
              ></opensilex-StringView>
              <opensilex-StringView
                v-if="germplasm.description != null"
                label="GermplasmDetails.comment"
                :value="germplasm.description"
              ></opensilex-StringView>
              <opensilex-LabelUriView
                v-if="(germplasm.species_name != null) || (germplasm.species != null)"
                label="GermplasmDetails.species"
                :value="germplasm.species_name"
                :uri="germplasm.species"
                :to="{path: '/germplasm/details/'+ encodeURIComponent(germplasm.species)}"
              ></opensilex-LabelUriView>
              <opensilex-LabelUriView
                v-if="(germplasm.variety_name != null) || (germplasm.variety != null)"
                label="GermplasmDetails.variety"
                :value="germplasm.variety_name"
                :uri="germplasm.variety"
                :to="{path: '/germplasm/details/'+ encodeURIComponent(germplasm.variety)}"
              ></opensilex-LabelUriView>
              <opensilex-LabelUriView
                v-if="(germplasm.accession_name != null) || (germplasm.accession != null)"
                label="GermplasmDetails.accession"
                :value="germplasm.accession_name"
                :uri="germplasm.accession"
                :to="{path: '/germplasm/details/'+ encodeURIComponent(germplasm.accession)}"
              ></opensilex-LabelUriView>
              <opensilex-MetadataView
              v-if="germplasm.publisher && germplasm.publisher.uri"
                :publisher="germplasm.publisher"
                :publicationDate="germplasm.publication_date"
                :lastUpdatedDate="germplasm.last_updated_date" 
              ></opensilex-MetadataView>
            </template>
          </opensilex-Card>
          <opensilex-Card label="GermplasmDetails.additionalInfo" icon="ik#ik-clipboard" v-if="addInfo.length != 0">
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
          <opensilex-AssociatedGermplasmGroupsList
              :searchMethod="loadGermplasmGroups"
              :nameFilter.sync="germplasmGroupName"
          ></opensilex-AssociatedGermplasmGroupsList>
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
      ref="germplasmForm"
      component="opensilex-GermplasmForm"
      createTitle="GermplasmView.add"
      editTitle="GermplasmView.update"
      icon="fa#seedling"
      modalSize="lg"
      @onUpdate="loadGermplasm()"
    ></opensilex-ModalForm>    

  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
// @ts-ignore
import { GermplasmGetSingleDTO, GermplasmUpdateDTO, GermplasmService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import AnnotationList from "../annotations/list/AnnotationList.vue";
import DocumentTabList from "../documents/DocumentTabList.vue";
import GermplasmForm from "./GermplasmForm.vue";

import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {DocumentsService} from "opensilex-core/api/documents.service";

@Component
export default class GermplasmDetails extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $route: any;
  $store: any;
  $router: any;
  routeArr : string = this.$route.path.split('/');
  $t: any;
  $i18n: any;
  service: GermplasmService;

  uri: string = null;
  addInfo = [];
  experimentName: any = "";
  germplasmGroupName = "";

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

  isDetailsTab() {
      localStorage.setItem("tabPath", this.routeArr[2]);
      localStorage.setItem("tabPage", "1");
      return this.$route.path.startsWith("/germplasm/details/");    
  }

  isDocumentTab() {
      return this.$route.path.startsWith("/germplasm/documents/");
  }

  isAnnotationTab() {
      return this.$route.path.startsWith("/germplasm/annotations/");
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.loadGermplasm();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }    

  germplasm: GermplasmGetSingleDTO = {
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
    institute: null,
    code: null,
    production_year: null,
    description: null,
    metadata: null,
    website: null,
    synonyms: []
  };

  created() {
    this.service = this.$opensilex.getService<GermplasmService>("opensilex.GermplasmService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.loadGermplasm();
    this.$AnnotationsService = this.$opensilex.getService<AnnotationsService>("opensilex.AnnotationsService");
    this.$DocumentsService = this.$opensilex.getService<DocumentsService>("opensilex.DocumentsService");
    this.searchAnnotations();
    this.searchDocuments();
  }

  loadGermplasm() {
    this.service
      .getGermplasm(this.uri)
      .then((http: HttpResponse<OpenSilexResponse<GermplasmGetSingleDTO>>) => {
        this.germplasm = http.response.result;
        this.loadExperiments;
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
    return this.service.getGermplasmExperiments(
      this.uri,
      this.experimentName,
      options.orderBy,
      options.currentPage,
      options.pageSize
    );
  }

  loadGermplasmGroups(options) {
    return this.service.searchGermplasmGroups(
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
      label: "GermplasmDetails.attribute",
    },
    {
      key: "value",
      label: "GermplasmDetails.value",
    },
  ];

  @Ref("tableAtt") readonly tableAtt!: any;

  getAddInfo() {
    this.addInfo = []
    for (const property in this.germplasm.metadata) {
      let tableData = {
        attribute: property,
        value: this.germplasm.metadata[property],
      };
      this.addInfo.push(tableData);
    }
  }

  @Ref("germplasmForm") readonly germplasmForm!: any;
  updateGermplasm() {

    let form: GermplasmForm = this.germplasmForm.getFormRef();
    form.readAttributes(this.germplasm.metadata);

    let updateDTO: GermplasmUpdateDTO = {
      uri: this.germplasm.uri,
      name: this.germplasm.name,
      rdf_type: this.germplasm.rdf_type,
      species: this.germplasm.species,
      variety: this.germplasm.variety,
      accession: this.germplasm.accession,
      institute: this.germplasm.institute,
      code: this.germplasm.code,
      production_year: this.germplasm.production_year,
      description: this.germplasm.description,
      metadata: this.germplasm.metadata,
      website: this.germplasm.website,
      synonyms: this.germplasm.synonyms
    }
    //let germplasmDtoCopy = JSON.parse(JSON.stringify(this.germplasm));
    this.germplasmForm.showEditForm(updateDTO);
  }

  deleteGermplasm() {
    this.service
      .deleteGermplasm(this.germplasm.uri)
      .then(() => {
        let message =
          this.$i18n.t("GermplasmView.title") +
          " " +
          this.germplasm.uri +
          " " +
          this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
        this.$router.push({
            path: "/germplasm"
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
  GermplasmDetails:
    title: Germplasm
    description: Detailed Information
    info: Germplasm Information
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
    backToList: Go back to Germplasm list
    code: Code
    synonyms: Synonyms
    additionalInfo: Additional information
    attribute: Attribute
    value: Value
    subtaxa: Subtaxa
    website: Web site

fr:
  GermplasmDetails:
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
    backToList: Retourner à la liste des germplasm
    code: Code 
    synonyms: Synonymes
    additionalInfo: Informations supplémentaires
    attribute: Attribut
    value: Valeur
    subtaxa: Subtaxa
    website: Site web    

</i18n>
