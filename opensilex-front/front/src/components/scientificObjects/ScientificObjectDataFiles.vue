<template>
  <div>

        <opensilex-PageContent class="pagecontent">
      <!-- Toggle Sidebar--> 
      <div class="searchMenuContainer"
      v-on:click="SearchFiltersToggle = !SearchFiltersToggle"
      :title="searchFiltersPannel()">
        <div class="searchMenuIcon">
          <i class="icon ik ik-search"></i>
        </div>
      </div>

      <!-- FILTERS -->
      <Transition>
        <div v-show="SearchFiltersToggle">

        <opensilex-SearchFilterField
          ref="searchField"
          :withButton="true"
          @search="refresh()"
          @clear="clear()"
          :showAdvancedSearch="false"
          class="searchFilterField"
        >
          <template v-slot:filters>

            <!-- Type -->
            <div>
              <opensilex-FilterField halfWidth="true">
                <opensilex-TypeForm
                  :type.sync="filter.rdf_type"
                  :baseType="$opensilex.Oeso.DATAFILE_TYPE_URI"
                  :ignoreRoot="false"
                  placeholder="ScientificObjectDataFiles.rdfType-placeholder"
                  class="searchFilter"
                ></opensilex-TypeForm>
              </opensilex-FilterField>
            </div>

            <!-- Experiments -->
            <div>
              <opensilex-FilterField halfWidth="true">
                <opensilex-ExperimentSelector
                  label="DataView.filter.experiments"
                  :experiments.sync="filter.experiments"
                  :multiple="true"
                  @select="refreshComponents"
                  @clear="refreshComponents"
                  class="searchFilter"
                ></opensilex-ExperimentSelector>
              </opensilex-FilterField>
            </div>

            <!-- Start Date -->
            <div>
              <opensilex-FilterField>
                <opensilex-DateTimeForm
                    :value.sync="filter.start_date"
                    label="component.common.begin"
                    name="startDate"
                    :max-date="filter.end_date ? filter.end_date : undefined" 
                    class="searchFilter"
                ></opensilex-DateTimeForm>
              </opensilex-FilterField>
            </div>

            <!-- End Date -->
            <div>
              <opensilex-FilterField>
                <opensilex-DateTimeForm
                    :value.sync="filter.end_date"
                    label="component.common.end"
                    name="endDate"
                    :min-date="filter.start_date ? filter.start_date : undefined"
                    :minDate="filter.start_date"
                    :maxDate="filter.end_date"
                    class="searchFilter"
                ></opensilex-DateTimeForm>
              </opensilex-FilterField>
            </div>

            <!-- Provenance -->
            <div>
              <opensilex-FilterField>
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
                  class="searchFilter"
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
            </div>

          </template>            
        </opensilex-SearchFilterField>
        </div>
      </Transition>
        
      <div class="card">
        <div class="card-body">
          <opensilex-DataFilesList 
            ref="datafilesList"
            :filter="filter"
            class="datafilesList"
            @redirectToDetail="redirectToDetail"
            :hideTarget= "true">
          </opensilex-DataFilesList>
        </div>
      </div>
    </opensilex-PageContent>
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

  data(){
    return {
      SearchFiltersToggle : false,
    }
  }

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
  
  searchFiltersPannel() {
    return  this.$t("searchfilter.label")
  }

  redirectToDetail(){
    this.$emit("redirectToDetail")
  }
}
</script>

<style scoped lang="scss">
.card-body {
  margin-bottom: -15px;
}

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