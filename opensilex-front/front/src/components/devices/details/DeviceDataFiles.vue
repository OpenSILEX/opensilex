<template>
  <div ref="page">
    <div class="card">
      <div class="card-body">
          <opensilex-SearchFilterField
            ref="searchField"
            :withButton="true"
            @search="refresh()"
            @clear="clear()"
            :showAdvancedSearch="false"
          >
            <template v-slot:filters>

              <!-- Type -->
              <opensilex-FilterField>
                <opensilex-TypeForm
                  :type.sync="filter.rdf_type"
                  :baseType="$opensilex.Oeso.DATAFILE_TYPE_URI"
                  :ignoreRoot="false"
                  placeholder="ScientificObjectDataFiles.rdfType-placeholder"
                ></opensilex-TypeForm>
              </opensilex-FilterField>

              <!-- Experiments -->
              <opensilex-FilterField>
                <opensilex-ExperimentSelector
                  label="DataView.filter.experiments"
                  :experiments.sync="filter.experiments"
                  :multiple="true"
                  @select="updateSOFilter"
                  @clear="updateSOFilter"
                ></opensilex-ExperimentSelector>
              </opensilex-FilterField>

              <!-- Scientific objects -->
              <opensilex-FilterField halfWidth="true">
                <opensilex-SelectForm
                  ref="soSelector"
                  label="DataView.filter.scientificObjects"
                  placeholder="DataView.filter.scientificObjects-placeholder"
                  :selected.sync="filter.scientificObjects"
                  :conversionMethod="soGetDTOToSelectNode"
                  modalComponent="opensilex-ScientificObjectModalList"
                  :itemLoadingMethod="loadSO"
                  :filter.sync="soFilter"
                  :isModalSearch="true"
                  :clearable="true"
                  :multiple="true"
                  @clear="refreshSoSelector"
                  @validate="refreshProvComponent"
                  :limit="1"
                ></opensilex-SelectForm>
              </opensilex-FilterField>

              <!-- Start Date -->
              <opensilex-FilterField>                
                <opensilex-DateTimeForm
                  :value.sync="filter.start_date"
                  label="component.common.begin"
                  :max-date="filter.end_date ? filter.end_date : undefined" 
                ></opensilex-DateTimeForm>
              </opensilex-FilterField>

              <!-- End Date -->
              <opensilex-FilterField>                
                <opensilex-DateTimeForm
                  :value.sync="filter.end_date"
                  label="component.common.end"
                  :min-date="filter.start_date ? filter.start_date : undefined"
                ></opensilex-DateTimeForm>
              </opensilex-FilterField>

              <!-- Provenance -->
              <opensilex-FilterField halfWidth="true">
                <opensilex-DatafileProvenanceSelector
                  ref="provSelector"
                  :provenances.sync="filter.provenance"
                  label="ExperimentData.provenance"
                  @select="loadProvenance"
                  :multiple="false"
                  :devices="[uri]"
                  :targets="filter.scientificObjects"
                  :experiments="filter.experiments"
                  :viewHandler="showProvenanceDetails"
                  :viewHandlerDetailsVisible="visibleDetails"
                  :showURI="false"
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

            <!-- <template v-slot:advancedSearch>
              <opensilex-FilterField :halfWidth="true">
                <div class="row">
                  <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                    <label for="metadataKey">{{ $t("DataVisuForm.search.metadataKey") }}</label>
                    <opensilex-StringFilter id="metadataKey" :filter.sync="filter.metadataKey" @update="onUpdate"></opensilex-StringFilter>
                  </div>
                  <div class="col col-xl-6 col-md-6 col-sm-6 col-12">
                    <label for="metadataValue">{{ $t("DataVisuForm.search.metadataValue") }}</label>
                    <opensilex-StringFilter id="metadataValue" :filter.sync="filter.metadataValue"  @update="onUpdate"></opensilex-StringFilter>
                  </div>
                </div>
              </opensilex-FilterField>
            </template> -->
            
          </opensilex-SearchFilterField>

          <opensilex-DataFilesList 
          ref="datafilesList"
          :filter="filter"
          :device="uri">
        </opensilex-DataFilesList>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO, ResourceTreeDTO, ScientificObjectNodeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import Oeso from "../../../ontologies/Oeso";

@Component
export default class DeviceDataFiles extends Vue {
  $opensilex: any;
  $t: any;
  $route: any; 
  refreshKey = 0; 
  
  visibleDetails: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;

  filterProvenanceLabel: string = null;

  filter = {
    start_date: undefined,
    end_date: undefined,
    rdf_type: undefined,
    provenance: undefined,
    experiments: [],
    scientificObjects: [],
    devices: []
  };

  soFilter = {
      name: undefined,
      experiment: undefined,
      germplasm: undefined,
      factorLevels: [],
      types: [],
      existenceDate: undefined,
      creationDate: undefined,
    };

  @Prop()
  uri;

  @Ref("datafilesList") readonly datafilesList!: any;
  @Ref("searchField") readonly searchField!: any;
  @Ref("provSelector") readonly provSelector!: any;
  @Ref("soSelector") readonly soSelector!: any;

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    let query: any = this.$route.query;

    this.loadTypes();

    this.resetFilters();
    for (let [key, value] of Object.entries(this.filter)) {
      if (query[key]) {
        this.filter[key] = decodeURIComponent(query[key]);
      }
    }
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.loadTypes();
        this.refresh();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }  


  refreshProvComponent(){
    this.refreshKey += 1
  }

  resetFilters() {
    this.filter = {
      start_date: null,
      end_date: null,
      rdf_type: null,
      provenance: null,
      experiments: [],
      scientificObjects: [],
      devices: [this.uri]
    };    
  }

  refreshSoSelector() {
    this.refreshProvComponent();
    this.soSelector.refreshModalSearch();
  }

  updateSOFilter() {
    this.refreshProvComponent();
    this.soFilter.experiment = this.filter.experiments[0];
    this.soSelector.refreshModalSearch();
  }

  successMessage(form) {
    return this.$t("ResultModalView.data-imported");
  }

  get getSelectedProv() {
    return this.selectedProvenance;
  }

  showProvenanceDetails() {
    if (this.selectedProvenance != null) {
      this.visibleDetails = !this.visibleDetails;
    }
  }

  clear() {
    this.selectedProvenance = null;
    this.resetFilters()
    this.filterProvenanceLabel = null;
    this.refresh();
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

  refresh() {
    this.datafilesList.refresh();
  }

  images_rdf_types = [];
  rdf_types = {};
  loadTypes() {
    this.$opensilex.getService("opensilex.OntologyService")
    .getSubClassesOf(Oeso.DATAFILE_TYPE_URI, false)
    .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
      console.log(http.response.result);
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

  soGetDTOToSelectNode(dto) {
    if (dto) {
      return {
        id: dto.uri,
        label: dto.name
      };
    }
    return null;
  }

  loadSO(scientificObjectsURIs) {
    return this.$opensilex.getService("opensilex.ScientificObjectsService")
      .getScientificObjectsListByUris(undefined,scientificObjectsURIs)
      .then((http: HttpResponse<OpenSilexResponse<Array<ScientificObjectNodeDTO>>>) => {
          return (http && http.response) ? http.response.result : undefined
    }).catch(this.$opensilex.errorHandler);
  }
  
}
</script>

<style scoped lang="scss">
</style>