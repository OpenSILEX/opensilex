<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="fa#file-medical-alt"
      title="component.menu.data.datafiles"
      description="DataFilesView.description"
    ></opensilex-PageHeader>

    <opensilex-PageContent>
      <template v-slot>

        <opensilex-SearchFilterField
          @search="refresh()"
          @clear="reset()"
          label="DataView.filter.label"
          :showTitle="false"
        >
          <template v-slot:filters>

            <!-- Type -->
            <opensilex-FilterField quarterWidth="true">
              <opensilex-TypeForm
                :type.sync="filter.rdf_type"
                :baseType="$opensilex.Oeso.DATAFILE_TYPE_URI"
                :ignoreRoot="false"
                placeholder="ScientificObjectDataFiles.rdfType-placeholder"
              ></opensilex-TypeForm>
            </opensilex-FilterField>

            <!-- Experiments -->
            <opensilex-FilterField quarterWidth="true">
              <opensilex-ExperimentSelector
                label="DataView.filter.experiments"
                :experiments.sync="filter.experiments"
                :multiple="true"
                @select="updateSOFilter"
                @clear="updateSOFilter"
              ></opensilex-ExperimentSelector>
            </opensilex-FilterField> 


              <!-- Start Date -->
              <opensilex-FilterField quarterWidth="true">
                <opensilex-DateTimeForm
                    :value.sync="filter.start_date"
                    label="component.common.begin"
                    name="startDate"
                    :max-date="filter.end_date ? filter.end_date : undefined" 
                ></opensilex-DateTimeForm>
            </opensilex-FilterField>

            <!-- End Date -->
            <opensilex-FilterField quarterWidth="true">
              <opensilex-DateTimeForm
                  :value.sync="filter.end_date"
                  label="component.common.end"
                  name="endDate"
                  :min-date="filter.start_date ? filter.start_date : undefined"
              ></opensilex-DateTimeForm>
            </opensilex-FilterField>

            <!-- Scientific objects -->
            <opensilex-FilterField halfWidth="true">
              <opensilex-SelectForm
                ref="soSelector"
                label="DataView.filter.scientificObjects"
                placeholder="DataView.filter.scientificObjects-placeholder"
                :selected.sync="filter.scientificObjects"
                modalComponent="opensilex-ScientificObjectModalList"
                :filter.sync="soFilter"
                :isModalSearch="true"
                :clearable="true"
                :multiple="true"
                @onValidate="refreshProvComponent"
                @onClose="refreshProvComponent"
                @clear="refreshSoSelector"
                :limit="1"
              ></opensilex-SelectForm>
            </opensilex-FilterField>


          

            <!-- Provenance -->
            <opensilex-FilterField halfWidth="true">
               
              <opensilex-DatafileProvenanceSelector
                ref="provSelector"
                :provenances.sync="filter.provenance"
                label="ExperimentData.provenance"
                @select="loadProvenance"
                :targets="filter.scientificObjects"
                :experiments="filter.experiments"
                :multiple="false"
                :viewHandler="showProvenanceDetails"
                :viewHandlerDetailsVisible="visibleDetails"
                :key="refreshKey"
            ></opensilex-DatafileProvenanceSelector>
            

              <b-collapse
                v-if="selectedProvenance"
                id="collapse-4"
                v-model="visibleDetails"
                class="mt-2"
              >
                <opensilex-ProvenanceDetails
                  :provenance="getSelectedProv"
                ></opensilex-ProvenanceDetails>
              </b-collapse>
            </opensilex-FilterField>

          </template>
        </opensilex-SearchFilterField>

        <opensilex-DataFilesList 
          ref="datafilesList"
          :filter="filter">
        </opensilex-DataFilesList>
      </template>
    </opensilex-PageContent>

  </div>
</template>

<script lang="ts">
import { Prop, Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO, ResourceTreeDTO } from "opensilex-core/index";
import {ScientificObjectNodeDTO} from "opensilex-core/model/scientificObjectNodeDTO";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import Oeso from "../../ontologies/Oeso";

@Component
export default class DataFilesView extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  disabled = false;

  refreshKey = 0;
  visibleDetails: boolean = false;
  selectedProvenance: any = null;
  filterProvenanceLabel: string = null;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("templateForm") readonly templateForm!: any;
  @Ref("datafilesList") readonly datafilesList!: any;
  @Ref("searchField") readonly searchField!: any;
  @Ref("provSelector") readonly provSelector!: any;
  @Ref("resultModal") readonly resultModal!: any;
  @Ref("soSelector") readonly soSelector!: any;

  filter = {
    start_date: null,
    end_date: null,
    rdf_type: null,
    provenance: null,
    experiments: [],
    scientificObjects: []
  };

  resetFilter() {
    this.filter = {
      start_date: null,
      end_date: null,
      rdf_type: null,
      provenance: null,
      experiments: [],
      scientificObjects: []
    };
  }

  soFilter = {
      name: undefined,
      experiment: undefined,
      germplasm: undefined,
      factorLevels: [],
      types: [],
      existenceDate: undefined,
      creationDate: undefined,
    };
    

  refreshSoSelector() {
    this.refreshProvComponent();
    this.soSelector.refreshModalSearch();
  }


  refreshProvComponent(){
    this.refreshKey += 1
  }


  updateSOFilter() {
    this.soFilter.experiment = this.filter.experiments[0];
    this.refreshProvComponent();
    this.soSelector.refreshModalSearch();
  }

  refresh() {
    this.datafilesList.refresh();
  }

  reset() {
    this.resetFilter();
    this.refresh();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.DataService");
    this.loadTypes();
  }

  get getSelectedProv() {
    return this.selectedProvenance;
  }

  showProvenanceDetails() {
    if (this.selectedProvenance != null) {
      this.visibleDetails = !this.visibleDetails;
    }
  }

  getProvenance(uri) {
    if (uri != undefined && uri != null) {
      return this.$opensilex
        .getService("opensilex.DataService")
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
    }
  }

  loadProvenance(selectedValue) {
    if (selectedValue != undefined && selectedValue != null) {
      this.getProvenance(selectedValue.id).then((prov) => {
        this.selectedProvenance = prov;
      });
    }
  }

  images_rdf_types = [];
  rdf_types = {};
  loadTypes() {
    this.$opensilex.getService("opensilex.OntologyService")
    .getSubClassesOf(Oeso.DATAFILE_TYPE_URI, false)
    .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
      let parentType = http.response.result[0];
      let key = parentType.uri;
      this.rdf_types[key] = parentType.name;
      for (let i = 0; i < parentType.children.length; i++) {   
        let key = parentType.children[i].uri;
        this.rdf_types[key] = parentType.children[i].name;
        if (Oeso.checkURIs(key, Oeso.IMAGE_TYPE_URI)) {
          let imageType = parentType.children[i];
          this.images_rdf_types.push(imageType.uri);
          for (let i = 0; i < imageType.children.length; i++) {
            let key = imageType.children[i].uri;
            this.rdf_types[key] = imageType.children[i].name;
            this.images_rdf_types.push(key);
          }
        } else {
          let subType = parentType.children[i];
          for (let i = 0; i < subType.children.length; i++) {
            let key = subType.children[i].uri;
            this.rdf_types[key] = subType.children[i].name;
          }
        }   
      }
    })
    .catch(this.$opensilex.errorHandler);
  }

}
</script>


<style scoped lang="scss">
</style>

<i18n>
en:
  DataFilesView:
    description: View datafiles
    details: view datafile metadata

fr:
  DataFilesView:
    description: Voir les fichiers de données
    details: Voir les métadonnées du fichier
</i18n>
