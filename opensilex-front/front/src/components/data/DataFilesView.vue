<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="ik#ik-bar-chart-line"
      title="component.menu.datafiles.label"
      description="DatafilesView.description"
    ></opensilex-PageHeader>

    <opensilex-SearchFilterField
      @search="refresh()"
      @clear="reset()"
      label="DataView.filter.label"
      :showTitle="false"
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

        <opensilex-FilterField>
          <!-- Start Date -->
          <opensilex-DateTimeForm
              :value.sync="filter.start_date"
              label="component.common.begin"
              name="startDate"
          ></opensilex-DateTimeForm>
        </opensilex-FilterField>

        <opensilex-FilterField>
          <!-- End Date -->
          <opensilex-DateTimeForm
              :isTime="true"
              :value.sync="filter.end_date"
              label="component.common.end"
              name="endDate"
          ></opensilex-DateTimeForm>
        </opensilex-FilterField>

        <!-- Experiments -->
        <opensilex-FilterField>
          <opensilex-ExperimentSelector
            label="DataView.filter.experiments"
            :experiments.sync="filter.experiments"
            :multiple="true"
          ></opensilex-ExperimentSelector>
        </opensilex-FilterField> 

        <!-- Scientific objects -->
        <opensilex-FilterField>
          <opensilex-SelectForm
            label="DataView.filter.scientificObjects"
            placeholder="DataView.filter.scientificObjects-placeholder"
            :selected.sync="filter.scientificObjects"
            :conversionMethod="soGetDTOToSelectNode"
            modalComponent="opensilex-ScientificObjectModalList"
            :isModalSearch="true"
            :clearable="false"
            :maximumSelectedItems="1"
          ></opensilex-SelectForm>
        </opensilex-FilterField>

        <!-- Provenance -->
        <opensilex-FilterField>
          <opensilex-ProvenanceSelector
            ref="provSelector"
            :provenances.sync="filter.provenance"
            :filterLabel="filterProvenanceLabel"
            label="ExperimentData.provenance"
            @select="loadProvenance"
            @clear="filterLabel = null"
            :experiment="uri"
            :multiple="false"
            :viewHandler="showProvenanceDetails"
            :viewHandlerDetailsVisible="visibleDetails"
            :showURI="false"
          ></opensilex-ProvenanceSelector>
        </opensilex-FilterField>

      </template>
    </opensilex-SearchFilterField>

    <opensilex-PageContent>
      <template v-slot></template>
    </opensilex-PageContent>

    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchDatafiles"
      :fields="fields"
      defaultSortBy="name"
    >

      <template v-slot:export>
        <b-dropdown
          dropup
          :small="false"
          :text="$t('ExperimentData.export')"
        >
          <b-dropdown-item-button @click="exportData('long')">
            {{ $t("ExperimentData.export-long") }}
            <opensilex-FormInputLabelHelper
              :helpMessage="$t('ExperimentData.export-long-help')"
            >
            </opensilex-FormInputLabelHelper>
          </b-dropdown-item-button>
          <b-dropdown-item-button @click="exportData('wide')"
            >{{ $t("ExperimentData.export-wide") }}
            <opensilex-FormInputLabelHelper
              :helpMessage="$t('ExperimentData.export-wide-help')"
            >
            </opensilex-FormInputLabelHelper
          ></b-dropdown-item-button>
        </b-dropdown>
      </template>

      <template v-slot:cell(scientific_object)="{ data }">
          <opensilex-UriLink
            :uri="data.item.scientific_object"
            :value="objects[data.item.scientific_object]"
            :to="{
              path:
                '/scientific-objects/details/' +
                encodeURIComponent(data.item.scientific_object),
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


    <opensilex-DataForm
      v-if="user.hasCredential(credentials.CREDENTIAL_DATA_MODIFICATION_ID)"
      ref="dataForm"
      @onCreate="afterCreateData"
    ></opensilex-DataForm>

    <opensilex-ResultModalView ref="resultModal" @onHide="refreshDataAfterImportation()"> </opensilex-ResultModalView>

    <opensilex-DataProvenanceModalView 
      ref="dataProvenanceModalView"        
    ></opensilex-DataProvenanceModalView>

    <opensilex-ImageModal      
      ref="imageModal"
      :fileUrl.sync="imageUrl"
    ></opensilex-ImageModal>
  </div>
</template>

<script lang="ts">
import { Prop, Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO, ResourceTreeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import Oeso from "../../ontologies/Oeso";

@Component
export default class DataFilesView extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  disabled = false;

  visibleDetails: boolean = false;
  usedVariables: any[] = [];
  selectedProvenance: any = null;
  imageUrl = null;

  @Ref("templateForm") readonly templateForm!: any;

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("tableRef") readonly tableRef!: any;
  @Ref("searchField") readonly searchField!: any;
  @Ref("provSelector") readonly provSelector!: any;
  @Ref("resultModal") readonly resultModal!: any;
  @Ref("dataProvenanceModalView") readonly dataProvenanceModalView!: any;
  @Ref("imageModal") readonly imageModal!: any;

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


  get user() {
    return this.$store.state.user;
  }

  get fields() {
    let tableFields: any = [
      {
        key: "scientific_object",
        label: "ExperimentData.object",
      },
      {
        key: "date",
        label: "DataView.list.date",
        sortable: true,
      },
      {
        key: "type",
        label: "ScientificObjectDataFiles.rdfType",
        sortable: true,
      },
      {
        key: "provenance",
        label: "DataView.list.provenance",
        sortable: false
      },
      {
        key: "actions",
        label: "component.common.actions"
      }
    ];

    return tableFields;
  }

  refresh() {
    this.tableRef.refresh();
  }

  reset() {
    this.resetFilter();
    this.refresh();
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.DataService");
    let query: any = this.$route.query;

    this.loadTypes();

    this.reset();
    for (let [key, value] of Object.entries(this.filter)) {
      if (query[key]) {
        this.filter[key] = decodeURIComponent(query[key]);
      }
    }
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

  objects = {};
  provenances = {};

  searchDatafiles(options) {
    let provUris = this.$opensilex.prepareGetParameter(this.filter.provenance);
    if (provUris != undefined) {
      provUris = [provUris];
    }

    let scientificObjects = [];
    if (this.filter.scientificObjects) {
      for (let i in this.filter.scientificObjects) {
        scientificObjects.push(this.filter.scientificObjects[i].id);
      }
    }

    return new Promise((resolve, reject) => {
      this.service.getDataFileDescriptionsBySearch(
        this.$opensilex.prepareGetParameter(this.filter.rdf_type),
        this.$opensilex.prepareGetParameter(this.filter.start_date), // start_date
        this.$opensilex.prepareGetParameter(this.filter.end_date), // end_date
        undefined, // timezone,
        this.filter.experiments, // experiments
        scientificObjects, // scientific_object
        provUris, // provenance
        undefined, // metadata
        undefined, // order_by
        options.currentPage,
        options.pageSize
      )
      .then((http) => {
        let promiseArray = [];
        let objectsToLoad = [];
        let provenancesToLoad = [];

        if (http.response.result.length > 0) {
          for (let i in http.response.result) {

            let objectURI = http.response.result[i].scientific_object;
            if (objectURI != null && !objectsToLoad.includes(objectURI)) {
              objectsToLoad.push(objectURI);
            }

            let provenanceURI = http.response.result[i].provenance.uri;
            if (!provenancesToLoad.includes(provenanceURI)) {
              provenancesToLoad.push(provenanceURI);
            }
          }        
          

            if (objectsToLoad.length > 0) {
              let promiseObject = this.$opensilex
                .getService("opensilex.ScientificObjectsService")
                .getScientificObjectsListByUris(undefined, objectsToLoad)
                .then((httpObj) => {
                  for (let j in httpObj.response.result) {
                    let obj = httpObj.response.result[j];
                    this.objects[obj.uri] =
                      obj.name + " (" + obj.rdf_type_name + ")";
                  }
                })
                .catch(reject);
              promiseArray.push(promiseObject);
            }

            if (provenancesToLoad.length > 0) {
              let promiseProvenance = this.$opensilex
                .getService("opensilex.DataService")
                .getProvenancesByURIs(provenancesToLoad)
                .then((httpObj) => {
                  for (let j in httpObj.response.result) {
                    let prov = httpObj.response.result[j];
                    this.provenances[prov.uri] = prov.name;
                  }
                })
                .catch(reject);
              promiseArray.push(promiseProvenance);
            }

            Promise.all(promiseArray).then((values) => {
              resolve(http);
            });

        } else {
          resolve(http);
        }
      })
      .catch(reject);
    });
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

  showImage(item: any) {
    let path = "/core/datafiles/" + encodeURIComponent(item.uri) + "/thumbnail?scaled_width=600&scaled_height=600";
      let promise = this.$opensilex.viewImageFromGetService(path);
      promise.then((result) => {
        this.imageUrl = result;
        this.imageModal.show();
      })    
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
  DataView:
    buttons:
      create-data : Add data
      generate-template : Generate template
    description: View and export data
    list:
      date: Date
      variable: Variable
      value: Value
      object: Object
      provenance: provenance
    filter:
      label: Search data
      experiments:  Experiment(s)
      variables: Variable(s)
      scientificObjects: scientific object(s)
      provenance: Provenance

fr:
  DataView:
    buttons:
      create-data : Ajouter un jeu de données
      generate-template : Générer un gabarit
    description: Visualiser et exporter des données
    list:
      date: Date
      variable: Variable
      value: Valeur
      object: objet
      provenance: provenance
    filter:
      label: Rechercher des données
      experiments:  Expérimentation(s)
      variables: Variable(s)
      scientificObject: Objet(s) scientifique(s)
      provenance: Provenance

      
  
</i18n>
