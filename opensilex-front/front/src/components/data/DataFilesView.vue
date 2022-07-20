<template>
  <div class="container-fluid">
    <opensilex-PageContent
    class="pagecontent">
      <template v-slot>

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
          @search="refresh()"
          @clear="reset()"
          label="DataView.filter.label"
          :showTitle="false"
          class="searchFilterField"
        >
          <template v-slot:filters>
            <!-- Type -->
            <div>
              <opensilex-FilterField>
                <opensilex-TypeForm
                  v-if="filter.imagesView"
                  :type.sync="filter.rdf_type"
                  :baseType="$opensilex.Oeso.IMAGE_TYPE_URI"
                  :ignoreRoot="false"
                  placeholder="ScientificObjectDataFiles.rdfType-placeholder"
                  class="searchFilter"
                  key="imageTypeForm"
                ></opensilex-TypeForm>

                <opensilex-TypeForm
                  v-else
                  :type.sync="filter.rdf_type"
                  :baseType="$opensilex.Oeso.DATAFILE_TYPE_URI"
                  :ignoreRoot="false"
                  placeholder="ScientificObjectDataFiles.rdfType-placeholder"
                  key="datafileTypeForm"
                  class="searchFilter"
                ></opensilex-TypeForm>
              </opensilex-FilterField>
            </div>

            <!-- Experiments -->
            <div>
              <opensilex-FilterField>
                <opensilex-ExperimentSelector
                  label="DataView.filter.experiments"
                  :experiments.sync="filter.experiments"
                  :multiple="true"
                  @select="updateSOFilter"
                  @clear="updateSOFilter"
                  class="searchFilter"
                ></opensilex-ExperimentSelector>
              </opensilex-FilterField> 
            </div>

            <!-- Scientific objects -->
            <div>
            <opensilex-FilterField>
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
                class="searchFilter"
              ></opensilex-SelectForm>
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
                  :experiments="filter.experiments"
                  :multiple="false"
                  :viewHandler="showProvenanceDetails"
                  :viewHandlerDetailsVisible="visibleDetails"
                  :showURI="false"
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

            <opensilex-FilterField>
              <b-form-checkbox v-model="filter.imagesView" name="check-button" switch>
              <b>Images view</b>
              </b-form-checkbox>
             </opensilex-FilterField>

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
          class="datafilesList">
        </opensilex-DataFilesList>


      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO } from "../../../../../opensilex-core/front/src/lib";
import HttpResponse from "../../lib/HttpResponse";
import { OpenSilexResponse } from "../../../../../opensilex-core/front/src/lib/HttpResponse";
import DataFilesList from "./DataFilesList.vue";
import DataFilesImagesList from "./DataFilesImagesList.vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {DataService} from "opensilex-core/api/data.service";

@Component
export default class DataFilesView extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;
  service: any;
  disabled = false;
  $i18n: any;

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
  @Ref("datafilesList") readonly datafilesList!: DataFilesList;
  @Ref("datafilesImagesList") readonly datafilesImagesList!: DataFilesImagesList;
  @Ref("searchField") readonly searchField!: any;
  @Ref("provSelector") readonly provSelector!: any;
  @Ref("resultModal") readonly resultModal!: any;
  @Ref("soSelector") readonly soSelector!: any;

  filter = {
    start_date: undefined,
    end_date: undefined,
    rdf_type: undefined,
    provenance: undefined,
    experiments: [],
    scientificObjects: [],
    imagesView: false
  };

  resetFilter() {
    this.filter = {
      start_date: undefined,
      end_date: undefined,
      rdf_type: undefined,
      provenance: undefined,
      experiments: [],
      scientificObjects: [],
      imagesView: true
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

  data(){
    return {
      SearchFiltersToggle : false,
    }
  }

  refreshSoSelector() {
    this.refreshProvComponent();
    this.soSelector.refreshModalSearch();
  }

  refreshProvComponent() {
    this.refreshKey += 1;
  }

  updateSOFilter() {
    this.soFilter.experiment = this.filter.experiments[0];
    this.refreshProvComponent();
    this.soSelector.refreshModalSearch();
  }

  refresh() {
    if(this.filter.imagesView) {
      this.datafilesImagesList.refresh();

    } else {
      this.datafilesList.refresh();
    }
   
  }

  reset() {
    this.resetFilter();
    this.refresh();
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
      let dataService : DataService = this.$opensilex.getService("opensilex.DataService");
      return dataService.getProvenance(uri)
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
  searchFiltersPannel() {
    return  this.$i18n.t("searchfilter.label")
  }
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
