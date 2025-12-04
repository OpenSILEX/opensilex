<template>
  <div ref="page">
    <div>
      <div>
        <opensilex-PageActions>
            <opensilex-CreateButton
            @click="datafileForm.showCreateForm()"
            label="DataFilesList.add"
            class="createButton"
            ></opensilex-CreateButton>
        </opensilex-PageActions>

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
              <opensilex-FilterField>
                <opensilex-TypeForm
                  :type.sync="filter.rdf_type"
                  :baseType="$opensilex.Oeso.DATAFILE_TYPE_URI"
                  :ignoreRoot="false"
                  placeholder="ScientificObjectDataFiles.rdfType-placeholder"
                  class="searchFilter"
                ></opensilex-TypeForm>
              </opensilex-FilterField>
              </div>

              <!-- Scientific objects -->
              <div>
              <opensilex-FilterField halfWidth="true">
                <opensilex-ModalFormSelector
                  ref="soSelector"
                  label="DataView.filter.scientificObjects"
                  placeholder="DataView.filter.scientificObjects-placeholder"
                  :selected.sync="filter.scientificObjects"
                  modalComponent="opensilex-ScientificObjectModalList"
                  :filter.sync="soFilter"
                  :clearable="true"
                  :multiple="true"
                  @clear="refreshSoSelector"
                  @onValidate="refreshProvComponent"
                  @onClose="refreshProvComponent"
                  :limit="1"
                  class="searchFilter"
                ></opensilex-ModalFormSelector>
              </opensilex-FilterField>
              </div>

              <!-- Start Date -->
              <div>
              <opensilex-FilterField>                
                <opensilex-DateTimeForm
                  :value.sync="filter.start_date"
                  label="component.common.begin"
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
                  :min-date="filter.start_date ? filter.start_date : undefined"
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
                  :targets="filter.scientificObjects"
                  :experiments="[uri]"
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

              <!-- Images -->
              <opensilex-FilterField>
                <b-form-checkbox v-model="filter.imagesView" name="check-button" switch>
                <b> {{ $t("DataFilesView.imagesView") }}</b>
                </b-form-checkbox>
              </opensilex-FilterField>
            </div>

            </template>            
            
          </opensilex-SearchFilterField>
        </div>
      </Transition>

      <opensilex-DataFilesImagesList
          v-if="filter.imagesView"
          ref="datafilesImagesList"
          :filter="filter"
          class="imagesList">
      </opensilex-DataFilesImagesList>

      <opensilex-DataFilesList 
          v-else
          ref="datafilesList"
          :filter="filter"
          class=datafilesList
      >
      </opensilex-DataFilesList>


    </opensilex-PageContent>

      <opensilex-ModalForm
        ref="datafileForm"
        :initForm="initForm"
        :data="{ tabExperimentMode: true }"
        component="opensilex-DataFileForm"
        editTitle="update"
        createTitle="DataFilesList.add"
        icon="ik#ik-file-text"
        modalSize="lg"
        @onCreate="refresh()"
      >
      </opensilex-ModalForm>
    </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO, ResourceTreeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import Oeso from "../../../ontologies/Oeso";
import DataFilesImagesList from "../../data/DataFilesImagesList.vue";
import { Route } from "vue-router";
import VueI18n from "vue-i18n";

@Component
export default class ExperimentDataFiles extends Vue {
  $opensilex: any;
  $t: typeof VueI18n.prototype.t;
  $route: Route; 
  refreshKey = 0; 
  uri = decodeURIComponent(this.$route.params.uri);
  
  visibleDetails: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;
  filterProvenanceLabel: string = null;
  SearchFiltersToggle: boolean = false;

  filter = {
    start_date: undefined,
    end_date: undefined,
    rdf_type: undefined,
    provenance: undefined,
    experiments: [],
    scientificObjects: [],
    devices: [],
    imagesView: false
  };

  soFilter = {
      name: undefined,
      experiment: undefined,
      geneticResource: undefined,
      factorLevels: [],
      types: [],
      existenceDate: undefined,
      creationDate: undefined,
    };


  @Ref("datafilesList") readonly datafilesList!: any;
  @Ref("searchField") readonly searchField!: any;
  @Ref("provSelector") readonly provSelector!: any;
  @Ref("soSelector") readonly soSelector!: any;
  @Ref("datafileForm") readonly datafileForm!: any;
  @Ref("datafilesImagesList") readonly datafilesImagesList!: DataFilesImagesList;

  created() {
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
      start_date: undefined,
      end_date: undefined,
      rdf_type: undefined,
      provenance: undefined,
      experiments: [this.uri],
      scientificObjects: [],
      devices: [],
      imagesView: false
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
    if(this.filter.imagesView) {
      this.datafilesImagesList.refresh();
    } else {
      this.datafilesList.refresh();
    }
  }

  images_rdf_types = [];
  rdf_types = {};
  loadTypes() {
    // Call to the ontology service to retrieve the subclasses of the “DataFile” type
    this.$opensilex.getService("opensilex.OntologyService")
    .getSubClassesOf(Oeso.DATAFILE_TYPE_URI, false)
    .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
      let parentType = http.response.result[0];
      let key = parentType.uri;
      this.rdf_types[key] = parentType.name;

      // Browse child types of the parent type
      for (let i = 0; i < parentType.children.length; i++) {   
        let key = parentType.children[i].uri;
        this.rdf_types[key] = parentType.children[i].name;

        // If the type matches IMAGE_TYPE_URI, we process the image subtypes.
        if (Oeso.checkURIs(key, Oeso.IMAGE_TYPE_URI)) {
          let imageType = parentType.children[i];
          this.images_rdf_types.push(imageType.uri);
          for (let i = 0; i < imageType.children.length; i++) {
            let key = imageType.children[i].uri;
            this.rdf_types[key] = imageType.children[i].name;
            this.images_rdf_types.push(key);
          }
        } else {
        // Otherwise, we add the generic subtypes.
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

   initForm() {
    if(this.uri){
      return {
        rdf_type: null,
        provenance: null,
        experiments: decodeURIComponent(this.uri),
        file: null
    }
  }}
}
</script>

<style scoped lang="scss">
.imagesList {
  min-width: 70%;
  width: 100%;
}

.datafilesList {
  width: 100%
}

.card-body {
  margin-bottom: -15px;
}

.createButton {
  margin-bottom: 10px;
  margin-top: 10px
}
</style>