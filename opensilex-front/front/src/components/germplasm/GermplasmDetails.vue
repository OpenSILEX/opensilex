<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="fa#sun"
      :title="germplasm.label"
      description="GermplasmDetails.description"
    ></opensilex-PageHeader>
    <opensilex-NavBar returnTo="/germplasm" returnToTitle="GermplasmDetails.backToList">
    </opensilex-NavBar>
  
  <div class="container-fluid">
      <b-row>
        <b-col>
          <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">
            <template v-slot:body>
              <opensilex-UriView v-if="germplasm.uri.startsWith('http')" :uri="germplasm.uri" :url="germplasm.uri"></opensilex-UriView>
              <opensilex-StringView v-else label="GermplasmDetails.uri" :value="germplasm.uri"></opensilex-StringView>
              <opensilex-StringView label="GermplasmDetails.rdfType" :value="germplasm.typeLabel"></opensilex-StringView>
              <opensilex-StringView label="GermplasmDetails.label" :value="germplasm.label"></opensilex-StringView>
              <opensilex-StringView v-if="germplasm.institute != null" label="GermplasmDetails.institute" :value="germplasm.institute"></opensilex-StringView>
              <opensilex-StringView v-if="germplasm.year != null" label="GermplasmDetails.year" :value="germplasm.year"></opensilex-StringView>
              <opensilex-StringView v-if="germplasm.comment != null" label="GermplasmDetails.comment" :value="germplasm.comment"></opensilex-StringView>
              <opensilex-LabelUriView 
              v-if="(germplasm.speciesLabel != null) || (germplasm.fromSpecies != null)" 
              label="GermplasmDetails.species" 
              :value="germplasm.speciesLabel" 
              :uri="germplasm.fromSpecies" 
              ></opensilex-LabelUriView>
              <opensilex-LabelUriView 
              v-if="(germplasm.varietyLabel != null) || (germplasm.fromVariety != null)" 
              label="GermplasmDetails.variety" 
              :value="germplasm.varietyLabel" 
              :uri="germplasm.fromVariety" 
              ></opensilex-LabelUriView>
              <opensilex-LabelUriView 
              v-if="(germplasm.fromAccession != null) || (germplasm.fromAccession != null)" 
              label="GermplasmDetails.accession" 
              :value="germplasm.accessionLabel" 
              :uri="germplasm.fromAccession" 
              ></opensilex-LabelUriView>
            </template>
          </opensilex-Card>
        </b-col>
        <b-col>
          <opensilex-Card label="GermplasmDetails.experiment" icon="ik#ik-clipboard">
            <template v-slot:body>
               <opensilex-TableAsyncView
                ref="tableRef"
                :searchMethod="loadExperiments"
                :fields="expFields"
                defaultSortBy="label"
              >
              <template v-slot:cell(uri)="{data}">
                <opensilex-UriLink
                  :uri="data.item.uri"
                  :to="{path: '/experiment/'+ encodeURIComponent(data.item.uri)}"
                ></opensilex-UriLink>
              </template>
              </opensilex-TableAsyncView>
            </template>
          </opensilex-Card>
        </b-col>
      </b-row>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import { GermplasmGetDTO, GermplasmService, ExperimentGetListDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class GermplasmDetails extends Vue {
  $opensilex: any;
  $store: any;
  $router: any;
  $t: any;
  $i18n: any;
  service: GermplasmService;

  uri: string = null;

  @Ref("modalRef") readonly modalRef!: any;

  get user() {
    return this.$store.state.user;
  }

  germplasm: GermplasmGetDTO = {
    uri: null,
    label: null,
    rdfType: null,
    fromSpecies: null,
    speciesLabel: null,
    fromVariety: null,
    varietyLabel: null,
    fromAccession: null,
    accessionLabel: null,
    institute: null,
    productionYear: null,
    comment: null
  };

  created() {
    this.service = this.$opensilex.getService("opensilex.GermplasmService");

    this.uri = this.$route.params.uri;
    this.loadGermplasm(this.uri);
  }

  loadGermplasm(uri: string) {
    this.service
      .getGermplasm(uri)
      .then((http: HttpResponse<OpenSilexResponse<GermplasmGetDTO>>) => {
        this.germplasm = http.response.result;
        this.loadExperiments
      })
      .catch(this.$opensilex.errorHandler);
  }

  expFields = [
    {
      key: "uri",
      label: "component.experiment.uri",
      sortable: true
    },
    {
      key: "label",
      label: "component.experiment.label",
      sortable: true
    },
  ]

  experiments = [];

  @Ref("table") readonly table!: any;

  loadExperiments(options) {
    
    return  this.service.getGermplasmExperiments(
      this.uri,
      options.orderBy,
      options.currentPage,
      options.pageSize)
  }
}
</script>

<style scoped lang="scss">
.uri-info {
  text-overflow: ellipsis;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  max-width: 300px;
}
</style>

<i18n>

en:
  GermplasmDetails:
    title: Germplasm Details
    description: Detailed Information
    info: Germplasm Information
    experiment: Associated experiments
    document: Associated documents
    uri: URI
    label: Name
    rdfType: Type
    species: Species
    variety: Variety
    accession: Accession
    institute: Institute
    year: Production Year
    comment: Comment
    backToList: Go back to Germplasm list

fr:
  GermplasmDetails:
    title: Détails du Germplasm
    description: Information détaillées
    info: Informations générales
    experiment: Experimentations associées
    document: Documents associées
    uri: URI
    label: Nom
    rdfType: Type
    species: Espèce
    variety: Variété
    accession: Accession
    institute: Institut
    year: Année de production
    comment: Commentaire
    backToList: Retourner à la liste des germplasm
</i18n>
