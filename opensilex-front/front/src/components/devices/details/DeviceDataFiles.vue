<template>
  <div ref="page">
    <div class="card">
      <div class="card-body">
          <opensilex-SearchFilterField
            ref="searchField"
            :withButton="true"
            :showTitle="true"
            @search="refresh()"
            @clear="clear()"
            :showAdvancedSearch="true"
          >
            <template v-slot:filters>

              <!-- Start Date -->
              <opensilex-FilterField>                
                <opensilex-DateTimeForm
                  :value.sync="filter.start_date"
                  label="component.common.begin"
                ></opensilex-DateTimeForm>
              </opensilex-FilterField>

              <!-- End Date -->
              <opensilex-FilterField>                
                <opensilex-DateTimeForm
                  :value.sync="filter.end_date"
                  label="component.common.end"
                ></opensilex-DateTimeForm>
              </opensilex-FilterField>

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
                  label="GermplasmList.filter.experiment"
                  :multiple="false"
                  :experiments.sync="filter.experiment"
                ></opensilex-ExperimentSelector>
              </opensilex-FilterField>

              <!-- TODO ScientificObjects -->

            </template>            

            <template v-slot:advancedSearch>

              <opensilex-FilterField :halfWidth="true">
                <opensilex-DatafileProvenanceSelector
                  ref="provSelector"
                  :provenances.sync="filter.provenance"
                  label="Provenance"
                  @select="loadProvenance"
                  :multiple="false"
                  :device="uri"
                  :viewHandler="showProvenanceDetails"
                  :viewHandlerDetailsVisible="visibleDetails"
                  :showURI="false"
                ></opensilex-DatafileProvenanceSelector>
              </opensilex-FilterField>

              <!-- <opensilex-FilterField :halfWidth="true">
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
              </opensilex-FilterField> -->

              <opensilex-FilterField>
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

          <opensilex-TableAsyncView
            ref="dataRef"
            :searchMethod="searchData"
            :fields="fields"
          >
            <!-- <template v-slot:export>
              <b-button class="mb-2 mr-2" @click="exportData('long')">{{
                $t("ExperimentData.export-long")
              }}</b-button>
              <b-button class="mb-2 mr-2" @click="exportData('wide')">{{
                $t("ExperimentData.export-wide")
              }}</b-button>
            </template> -->
            <template v-slot:cell(scientific_objects)="{ data }">
                <opensilex-UriLink v-for="scientific_object in data.item.scientific_objects" :key="scientific_object"
                  :uri="scientific_object"
                  :value="objects[scientific_object]"
                  :to="{
                    path:
                      '/scientific-objects/details/' +
                      encodeURIComponent(scientific_object),
                  }"
                ></opensilex-UriLink>
            </template>

            <template v-slot:cell(provenance)="{ data }">
              <opensilex-UriLink
                :uri="data.item.provenance.uri"
                :value="provenances[data.item.provenance.uri]"
                :noExternalLink="true"
                @click="showProvenanceDetailsModal(data.item)"
              ></opensilex-UriLink>
            </template>
          
            <template v-slot:cell(type)="{ data }">
              <div>{{ rdf_types[data.item.rdf_type] }}</div>
            </template>

            <template v-slot:cell(actions)="{data}">
              <b-button-group size="sm">
                <opensilex-Button
                  :disabled="!images_rdf_types.includes(data.item.rdf_type)"
                  component="opensilex-DocumentDetails"
                  @click="showImage(data.item)"
                  label="ScientificObjectDataFiles.displayImage"
                  :small="true"
                  icon= "ik#ik-eye"
                  variant="outline-info"
                ></opensilex-Button>
              </b-button-group>
            </template>

          </opensilex-TableAsyncView>
          <opensilex-DataProvenanceModalView 
            ref="dataProvenanceModalView"
            :datafile="true"          
          ></opensilex-DataProvenanceModalView>
      </div>
    </div>

    <opensilex-ImageModal      
      ref="imageModal"
      :fileUrl.sync="imageUrl"
    ></opensilex-ImageModal>

  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO, ResourceTreeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import Oeso from "../../../ontologies/Oeso";

@Component
export default class DeviceDataFiles extends Vue {
  $opensilex: any;
  $t: any;
  $route: any;  
  
  visibleDetails: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;

  filterProvenanceLabel: string = null;

  filter = {
    start_date: undefined,
    end_date: undefined,
    rdf_type: undefined,
    experiment: undefined,
    provenance: undefined,
  };

  imageUrl = null;

  @Prop()
  uri;

  @Ref("dataRef") readonly dataRef!: any;

  @Ref("imageModal") readonly imageModal!: any;

  @Ref("searchField") readonly searchField!: any;

  @Ref("provSelector") readonly provSelector!: any;

  @Ref("dataProvenanceModalView") readonly dataProvenanceModalView!: any;

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

  resetFilters() {
    this.filter = {
      start_date: undefined,
      end_date: undefined,
      rdf_type: undefined,
      experiment: undefined,
      provenance: undefined,
    };    
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

  fields = [
    {
      key: "date",
      label: "ExperimentData.date",
      sortable: true
    },
    {
      key: "type",
      label: "ScientificObjectDataFiles.rdfType",
      sortable: false
    },
    {
      key: "provenance",
      label: "ExperimentData.provenance",
      sortable: false
    },
    {
      key: "scientific_objects",
      label: "DeviceDataFiles.objects",
      sortable: false
    },
    {
      key: "actions",
      label: "component.common.actions"
    }
  ];

  objects = {};
  provenances = {};
  images_rdf_types = [];
  rdf_types = {};

  searchData(options) {
    let provUris = undefined;
    if (this.filter.provenance != null) {
      provUris = [this.filter.provenance];
    }

    let expUris = undefined;
    if (this.filter.experiment != null) {
      expUris = [this.filter.experiment];
    }

    // let objUris = undefined;
    // if (this.filter.scientific_object != null) {
    //   expUris = [this.filter.experiment];
    // }

    let start_date = this.$opensilex.prepareGetParameter(
      this.filter.start_date
    );
    let end_date = this.$opensilex.prepareGetParameter(this.filter.end_date);

    return new Promise((resolve, reject) => {
      this.$opensilex
        .getService("opensilex.DevicesService")
        .searchDeviceDatafiles(
          this.uri,
          this.filter.rdf_type, //rdf_type
          start_date, // start_date
          end_date, // end_date
          undefined, // timezone,
          expUris, //experiments
          undefined, // objectUris
          provUris, // provenance_uri
          undefined, // metadata
          options.orderBy, // order_by
          options.currentPage,
          options.pageSize,
        )
        .then((http) => {
          let objectToLoad = [];
          let provenancesToLoad = [];
          if (http.response.result.length > 0) {
            for (let i in http.response.result) {
              let provenanceURI = http.response.result[i].provenance.uri;
              if (!provenancesToLoad.includes(provenanceURI)) {
                provenancesToLoad.push(provenanceURI);
              }

              let objectsURIs = http.response.result[i].scientific_objets;
              for (let k in objectsURIs) {
                if (!objectsURIs[i] &&  !objectToLoad.includes(objectsURIs[i])) {
                  objectToLoad.push(objectsURIs[i]);
                }
              }
            }           

            if (provenancesToLoad.length > 0) {
              this.$opensilex
                .getService("opensilex.DataService")
                .getProvenancesByURIs(provenancesToLoad)
                .then((httpObj) => {
                  for (let j in httpObj.response.result) {
                    let prov = httpObj.response.result[j];
                    this.provenances[prov.uri] = prov.name;
                  }
                  resolve(http);
                })
                .catch(reject);
            }

            if (objectToLoad.length > 0) {
              this.$opensilex
                .getService("opensilex.ScientificObjectsService")
                .getScientificObjectsListByUris(this.uri, objectToLoad)
                .then((httpObj) => {
                  for (let j in httpObj.response.result) {
                    let obj = httpObj.response.result[j];
                    this.objects[obj.uri] =
                      obj.name + " (" + obj.rdf_type_name + ")";
                  }
                  resolve(http);
                })
                .catch(reject);
            }

          } else {
            resolve(http);
          }
        })
        .catch(reject);
    });
  }

  refresh() {
    this.dataRef.refresh();
  }

  showImage(item: any) {
    let path = "/core/datafiles/" + encodeURIComponent(item.uri) + "/thumbnail?scaled_width=600&scaled_height=600";
      let promise = this.$opensilex.viewImageFromGetService(path);
      promise.then((result) => {
        this.imageUrl = result;
        this.imageModal.show();
      })    
  }

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

  showProvenanceDetailsModal(item) {
    this.$opensilex.enableLoader();
    this.getProvenance(item.provenance.uri)
    .then(result => {
      let value = {
        provenance: result,
        data: item
      }
      this.dataProvenanceModalView.setProvenance(value);
      this.dataProvenanceModalView.show();
    });    
  }
  
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    DeviceDataFiles:
      objects: Scientific Objects
      
fr:
    DeviceDataFiles:
      objects: Objets scientifiques

</i18n>