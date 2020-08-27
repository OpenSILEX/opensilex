<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="fa#sun"
      title="GermplasmDetails.title"
      :description="germplasm.label"
    ></opensilex-PageHeader>

    <opensilex-PageActions :returnButton="true" >   
        <b-button
          v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"           
          @click="update"
          label="update"
          variant="primary"
        >update</b-button>      
    </opensilex-PageActions>

    <opensilex-PageContent>
      <b-row>
        <b-col>
          <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">
            <template v-slot:body>
              <opensilex-UriView
                v-if="germplasm.uri.startsWith('http')"
                :uri="germplasm.uri"
                :url="germplasm.uri"
              ></opensilex-UriView>
              <opensilex-StringView class="test" v-else label="GermplasmDetails.uri" :value="germplasm.uri"></opensilex-StringView>
              <opensilex-StringView label="GermplasmDetails.rdfType" :value="germplasm.typeLabel"></opensilex-StringView>
              <opensilex-StringView label="GermplasmDetails.label" :value="germplasm.label"></opensilex-StringView>
              <opensilex-StringView
                v-if="germplasm.synonyms.length>0"
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
              <opensilex-StringView
                v-if="germplasm.productionYear != null"
                label="GermplasmDetails.year"
                :value="germplasm.productionYear"
              ></opensilex-StringView>
              <opensilex-StringView
                v-if="germplasm.comment != null"
                label="GermplasmDetails.comment"
                :value="germplasm.comment"
              ></opensilex-StringView>
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
                v-if="(germplasm.accessionLabel != null) || (germplasm.fromAccession != null)"
                label="GermplasmDetails.accession"
                :value="germplasm.accessionLabel"
                :uri="germplasm.fromAccession"
              ></opensilex-LabelUriView>
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
    </opensilex-PageContent>
    <opensilex-ModalForm
      
      ref="germplasmForm"
      component="opensilex-GermplasmForm"
      editTitle="udpate"
      icon="ik#ik-user"
      modalSize="lg"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  GermplasmGetSingleDTO,
  GermplasmService,
  ExperimentGetListDTO,
} from "opensilex-core/index";
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
  addInfo = [];

  @Ref("modalRef") readonly modalRef!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }


  germplasm: GermplasmGetSingleDTO = {
    uri: null,
    label: null,
    rdfType: null,
    typeLabel: null,
    fromSpecies: null,
    speciesLabel: null,
    fromVariety: null,
    varietyLabel: null,
    fromAccession: null,
    accessionLabel: null,
    institute: null,
    code: null,
    productionYear: null,
    comment: null,
    attributes: null,
  };

  created() {
    this.service = this.$opensilex.getService("opensilex.GermplasmService");

    this.uri = this.$route.params.uri;
    this.loadGermplasm(this.uri);
  }

  loadGermplasm(uri: string) {
    this.service
      .getGermplasm(uri)
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
    for (const property in this.germplasm.attributes) {
      let tableData = {
        attribute: property,
        value: this.germplasm.attributes[property],
      };
      this.addInfo.push(tableData);
    }
  }

  @Ref("germplasmForm") readonly germplasmForm!: any;
  update() {
    this.germplasmForm.showEditForm(this.germplasm);
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
    label: Name
    rdfType: Type
    species: Species
    variety: Variety
    accession: Accession
    institute: Institute
    year: Year
    comment: Comment
    backToList: Go back to Germplasm list
    code: Code
    synonyms: Synonyms
    additionalInfo: Additional information
    attribute: Attribute
    value: Value

fr:
  GermplasmDetails:
    title: Ressource génétique
    description: Information détaillées
    info: Informations générales
    experiment: Expérimentations associées
    document: Documents associées
    uri: URI 
    label: Nom 
    rdfType: Type 
    species: Espèce 
    variety: Variété 
    accession: Accession 
    institute: Institut 
    year: Année 
    comment: Commentaire 
    backToList: Retourner à la liste des germplasm
    code: Code 
    synonyms: Synonymes
    additionalInfo: Informations supplémentaires
    attribute: Attribut
    value: Valeur
</i18n>
