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
          <opensilex-FilterField halfWidth="true">
            <opensilex-TypeForm
              :type.sync="filter.rdf_type"
              :baseType="$opensilex.Oeso.DATAFILE_TYPE_URI"
              :ignoreRoot="false"
              placeholder="ScientificObjectDataFiles.rdfType-placeholder"
            ></opensilex-TypeForm>
          </opensilex-FilterField>

          <!-- Experiments -->
          <opensilex-FilterField halfWidth="true">
            <opensilex-ExperimentSelector
              label="DataView.filter.experiments"
              :experiments.sync="filter.experiments"
              :multiple="true"
              @select="refreshComponents"
              @clear="refreshComponents"
            ></opensilex-ExperimentSelector>
          </opensilex-FilterField>

          <opensilex-FilterField>
            <!-- Start Date -->
            <opensilex-DateTimeForm
                :value.sync="filter.start_date"
                label="component.common.begin"
                name="startDate"
                :max-date="filter.end_date ? filter.end_date : undefined" 
            ></opensilex-DateTimeForm>
          </opensilex-FilterField>

          <opensilex-FilterField>
            <!-- End Date -->
            <opensilex-DateTimeForm
                :value.sync="filter.end_date"
                label="component.common.end"
                name="endDate"
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
              :targets="[uri]"
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
          :filter="filter">
        </opensilex-DataFilesList>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { ProvenanceGetDTO, ResourceTreeDTO, ScientificObjectNodeDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import Oeso from "../../ontologies/Oeso";

@Component
export default class ScientificObjectDataFiles extends Vue {
  $opensilex: any;
  $t: any;
  $route: any;  
  
  visibleDetails: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;

  filterProvenanceLabel: string = null;

  filter = {
    start_date: null,
    end_date: null,
    rdf_type: null,
    provenance: null,
    experiments: [],
    scientificObjects: []
  };

  refreshKey = 0;

  @Prop()
  uri;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("datafilesList") readonly datafilesList!: any;
  @Ref("searchField") readonly searchField!: any;
  @Ref("provSelector") readonly provSelector!: any;

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

  resetFilters() {
    this.filter = {
      start_date: undefined,
      end_date: undefined,
      rdf_type: undefined,
      provenance: undefined,
      experiments: [],
      scientificObjects: [this.uri]
    };    
  }

  refreshComponents(){
    this.refreshKey += 1;
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


}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    ScientificObjectDataFiles:
      datafiles: Datafiles
      rdfType: Type
      displayImage: Display image
      rdfType-placeholder: Select the datafile type
      
fr:
    ScientificObjectDataFiles:
      datafiles: Fichiers de données
      rdfType : Type
      displayImage: Afficher l'image
      rdfType-placeholder: Sélectionner le type de fichier

</i18n>