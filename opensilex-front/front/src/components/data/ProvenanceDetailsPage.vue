<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="fa#seedling"
      :title="provenance.name"
      description="ProvenanceDetails.title"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs=true :returnButton="true">
      <b-nav-item
      :active="isDetailsTab()"
      :to="{path: '/provenances/details/' + encodeURIComponent(uri)}"
      >{{ $t('component.common.details-label') }}
      </b-nav-item>

      <b-nav-item
      :active="isAnnotationTab()"
      :to="{ path: '/provenances/annotations/' + encodeURIComponent(uri) }"
      >{{ $t("Annotation.list-title") }}
      </b-nav-item>

      <b-nav-item
      :active="isDocumentTab()"
      :to="{path: '/provenances/documents/' + encodeURIComponent(uri)}"
      >{{ $t('component.project.documents') }}
      </b-nav-item>

    </opensilex-PageActions>

    <opensilex-PageContent>
      <b-row v-if="isDetailsTab()">
        <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">
          <template v-slot:body>
            <opensilex-UriView
              label="ProvenanceDetails.uri"
              :uri="provenance.uri"
            ></opensilex-UriView>
            <opensilex-StringView
              label="ProvenanceDetails.label"
              :value="provenance.name"
            ></opensilex-StringView>

            <opensilex-StringView 
              v-if="provenance.prov_activity!= null && provenance.prov_activity.length>0"
              label="ProvenanceDetails.activity"
              :value="provenance.prov_activity[0].rdf_type"
            ></opensilex-StringView>

            <opensilex-StringView
              v-if="provenance.prov_activity!= null && provenance.prov_activity.length>0"
              label="ProvenanceDetails.activity_start_date"
              :value="provenance.prov_activity[0].start_date"
            ></opensilex-StringView>

            <opensilex-UriListView
              v-if="provenance.prov_agent!= null && provenance.prov_agent.length>0"
              label="provenance.agent"
              :list="agentsList"
            ></opensilex-UriListView>
          </template>
        </opensilex-Card>
      </b-row>

      <opensilex-DocumentTabList
        v-else-if="isDocumentTab()"
        ref="documentTabList"
        :uri="uri"        
        :modificationCredentialId="credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID"
      ></opensilex-DocumentTabList>

      <opensilex-AnnotationList
      v-else-if="isAnnotationTab()"
      ref="annotationList"
      :target="uri"
      :displayTargetColumn="false"
      :enableActions="true"
      :modificationCredentialId="credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID"
      :deleteCredentialId="credentials.CREDENTIAL_GERMPLASM_DELETE_ID"
      ></opensilex-AnnotationList>

    </opensilex-PageContent>   

  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  ProvenanceGetDTO, DataService
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import AnnotationList from "../annotations/list/AnnotationList.vue";
import DocumentTabList from "../documents/DocumentTabList.vue";


@Component
export default class ProvenanceDetailsPage extends Vue {
  $opensilex: any;
  $route: any;
  $store: any;
  $router: any;
  $t: any;
  $i18n: any;
  service: DataService;

  uri: string = null;
  addInfo = [];
  experimentName: any = "";

  @Ref("annotationList") readonly annotationList!: AnnotationList;
  @Ref("documentTabList") readonly documentTabList!: DocumentTabList;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  isDetailsTab() {
      return this.$route.path.startsWith("/provenances/details/");
  }

  isDocumentTab() {
      return this.$route.path.startsWith("/provenances/documents/");
  }

  isAnnotationTab() {
      return this.$route.path.startsWith("/provenances/annotations/");
  } 

  provenance: ProvenanceGetDTO = {
        uri: null,
        name: null,
        description: null,
        prov_activity: [],
        prov_agent: [],
      };

  refresh() {
    this.loadProvenance(this.uri);
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.DataService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.refresh();
  }

  loadProvenance(uri: string) {
    this.service
      .getProvenance(uri)
      .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
        this.provenance = http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }

  get agentsList() {
    if (
      this.provenance.prov_agent != null &&
      this.provenance.prov_agent != undefined
    ) {
      return this.provenance.prov_agent.map((item) => {
        return {
          uri: item.uri,
          url: null,
          value: item.uri,
        };
      });
    } else {
      return [];
    }
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
    experiment: Associated experiments
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
    experiment: Expérimentations associées
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
