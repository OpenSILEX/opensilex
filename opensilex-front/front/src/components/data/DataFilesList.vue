<template>
  <div>
    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchDatafiles"
      :countMethod="countDatafiles"
      :fields="fields"
      defaultSortBy="name"
      :isSelectable="true"
      labelNumberOfSelectedRow="DataFilesList.selected"

    >

      <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
        <b-dropdown
        dropright
        class="mb-2 mr-2"
        :small="true"
        :text="$t('VariableList.display')">

        <b-dropdown-item-button @click="clickOnlySelected()">{{ onlySelected ? $t('DataFilesList.selected-all') : $t("component.common.selected-only")}}</b-dropdown-item-button>
        <b-dropdown-item-button @click="resetSelected()">{{$t("component.common.resetSelected")}}</b-dropdown-item-button>
        </b-dropdown>

        <b-dropdown
            dropright
            class="mb-2 mr-2"
            :small="true"
            :disabled="numberOfSelectedRows == 0"
            text="actions"
        >

        <b-dropdown-item-button @click="exportDataFileModal.show()">{{ $t('DataFilesList.export') }}</b-dropdown-item-button>
        <b-dropdown-item-button
            v-if="user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
            @click="createEvents()">{{$t('Event.add-multiple')}}
        </b-dropdown-item-button>
        </b-dropdown>
      </template>

      <!--Target -->
      <template v-slot:cell(target)="{ data }">
          <opensilex-UriLink
            :uri="data.item.target"
            :value="objects[data.item.target]"
            :to="{
              path: getTargetPath(data.item.target)
            }"
          ></opensilex-UriLink>
      </template>

      <!-- Format -->
       <template v-slot:cell(format)="{ data }">
        <div>{{ data.item.filename.split('.').pop().toUpperCase() }}</div>
      </template>

      <!-- Filename -->
       <template v-slot:cell(filename)="{ data }">
        <div>{{ data.item.filename }}</div>
      </template>

       <!-- Type -->
       <template v-slot:cell(rdfType)="{ data }">
          <div>{{ rdf_types[data.item.rdf_type] }}</div>
        </template>

      <!-- Provenance -->
      <template v-slot:cell(provenance)="{ data }">
        <opensilex-UriLink
          :uri="data.item.provenance.uri"
          :value="provenances[data.item.provenance.uri]"
          :to="{
            path:
              '/provenances/details/' + 
              encodeURIComponent(data.item.provenance.uri)
          }"
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
              icon= "fa#image"
              variant="outline-info"
            ></opensilex-Button>
            <opensilex-DetailButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
              @click="showDataProvenanceDetailsModal(data.item)"
              label="DataFilesView.details"
              :small="true"
          ></opensilex-DetailButton>

          <opensilex-Button
            @click="deleteDatafile(data.item.uri)"
            variant="outline-danger"
            :small="true"
            label="DataFilesList.delete"
            icon="fa#trash-alt"
          >
          </opensilex-Button>

          </b-button-group>
        </template>
    </opensilex-TableAsyncView>

    <opensilex-DataProvenanceModalView 
      ref="dataProvenanceModalView"
      :datafile="true"          
    ></opensilex-DataProvenanceModalView>

    <opensilex-ImageModal      
      ref="imageModal"
      :fileUrl.sync="imageUrl"
    ></opensilex-ImageModal>

    <opensilex-ExportDataFileModal      
      ref="exportDataFileModal"
      @onCreate="exportDataFiles"
    ></opensilex-ExportDataFileModal>

    <opensilex-EventCsvForm
      v-if="showEventForm && user.hasCredential(credentials.CREDENTIAL_EVENT_MODIFICATION_ID)"
      ref="eventCsvForm"
      :targets="selectedUris">
    </opensilex-EventCsvForm>

  </div>

</template>

<script lang="ts">
import { Prop, Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO, ResourceTreeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {DataService} from "opensilex-core/api/data.service";
import EventCsvForm from "../events/form/csv/EventCsvForm.vue";

@Component
export default class DataFilesList extends Vue {
  $opensilex: any;
  $store: any;
  service: DataService;
  ontologyService: OntologyService;
  routeArr : string[] = this.$route.path.split('/');
  showEventForm: boolean = false;
  selectedUris: Array<string> = [];

  disabled = false;
  imageUrl = null;

  @Prop({
    default: () => {
      return {
        name: null,
        start_date: null,
        end_date: null,
        rdf_type: null,
        provenance: null,
        experiments: [],
        scientificObjects: []
      };
    },
  })
  filter: any;

  @Prop()
  device;

  @Prop({
    default: "",
  })
  contextUri: string;


  @Prop()
  hideTarget: boolean;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("templateForm") readonly templateForm!: any;
  @Ref("tableRef") readonly tableRef!: any;
  @Ref("dataProvenanceModalView") readonly dataProvenanceModalView!: any;
  @Ref("imageModal") readonly imageModal!: any;
  @Ref("eventCsvForm") readonly eventCsvForm!: EventCsvForm;
  @Ref("exportDataFileModal") readonly exportDataFileModal!: any;

  get fields() {
    let fields: any = [
      {
        key: "date",
        label: "DataView.list.date",
        sortable: true,
      },
      {
        key: "format",
        label: "DataFilesList.format",
        sortable: true,
      },
      {
        key: "filename",
        label: "DataFilesList.filename",
        sortable: true,
      },
      {
        key: "rdfType",
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
    if(!this.hideTarget) {
      fields.unshift(
      {
        key: "target",
        label: "DataView.list.object"
      }
      )
    }
    return fields;
  }

  refresh() {
      this.$opensilex.updateURLParameters(this.filter);
      this.tableRef.changeCurrentPage(1);
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.DataService");
    this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
    this.loadTypes();
    this.$opensilex.updateFiltersFromURL(this.$route.query, this.filter); 
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

  showDataProvenanceDetailsModal(item) {
    this.$opensilex.enableLoader();
    this.getProvenance(item.provenance.uri)
    .then(result => {
      let value = {
        provenance: result,
        data: item
      }
      this.dataProvenanceModalView.setProvenanceAndBatch(value);
      this.dataProvenanceModalView.show();
    });    
  }

     /**
     * Construct paths for each target's UriLink components according to their type.
     */
    private loadObjectsPath(): Promise<unknown> {
      // ensure that at least one object has been loaded (in case where all data in the page have no target)
      let objectURIs = Object.keys(this.objects);
      if (!objectURIs || objectURIs.length == 0) {
        return Promise.resolve();
      }

      return this.ontologyService
        .getURITypes(objectURIs)
        .then((httpObj) => {
          for (let j in httpObj.response.result) {
            let obj = httpObj.response.result[j];
            this.objectsPath[obj.uri] = this.$opensilex.getPathFromUriTypes(obj.rdf_types);
          }
        }
      );
    }


    getTargetPath(uri: string) {
    let defaultOsPath: string = this.objectsPath[uri];
    if(! defaultOsPath){
        return "";
    }

    let osPath = defaultOsPath.replace(':uri', encodeURIComponent(uri))

    // pass encoded experiment inside OS path URL
    if(this.contextUri && this.contextUri.length > 0){
        return osPath.replace(':experiment', encodeURIComponent(this.contextUri));
    }else{ // no experiment passed
        return osPath.replace(':experiment', "");
    }
  }

  objects = {};
  provenances = {};
  objectsPath = {};

  countDatafiles() {
    let provUris = this.$opensilex.prepareGetParameter(this.filter.provenance);
    if (provUris != undefined) {
      provUris = [provUris];
    }

    return this.service.countDatafiles(
      this.filter.scientificObjects,
      this.filter.devices,
      this.$opensilex.prepareGetParameter(this.filter.name),
      this.$opensilex.prepareGetParameter(this.filter.rdf_type),
      this.$opensilex.prepareGetParameter(this.filter.start_date), // start_date
      this.$opensilex.prepareGetParameter(this.filter.end_date), // end_date
      undefined, // timezone,
      this.filter.experiments, // experiments
      provUris, // provenances
      undefined // metadata
    );
  }

  searchDatafiles(options) {
    let provUris = this.$opensilex.prepareGetParameter(this.filter.provenance);
    if (provUris != undefined) {
      provUris = [provUris];
    }

    return new Promise((resolve, reject) => {
        this.service.getDataFileDescriptionsByTargets(
          this.$opensilex.prepareGetParameter(this.filter.name),
          this.$opensilex.prepareGetParameter(this.filter.rdf_type),
          this.$opensilex.prepareGetParameter(this.filter.start_date), // start_date
          this.$opensilex.prepareGetParameter(this.filter.end_date), // end_date
          undefined, // timezone,
          this.filter.experiments, // experiments
          this.filter.devices, //devices
          provUris, // provenances
          undefined, // metadata
          options.orderBy, // order_by
          options.currentPage,
          options.pageSize,
          this.filter.scientificObjects, // scientific_object
        ).then((http) => {
        let promiseArray = [];
        let objectsToLoad = [];
        let provenancesToLoad = [];

        if (http.response.result.length > 0) {
          for (let i in http.response.result) {

            let objectURI = http.response.result[i].target;
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
                .getService("opensilex.OntologyService")
                .getURILabelsList(objectsToLoad, this.contextUri)
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
                this.loadObjectsPath().then((value) => {
                    resolve(http);
                })
            });

        } else {
          resolve(http);
        }
      })
      .catch(reject);
    });
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
    .getSubClassesOf(
        this.$opensilex.Oeso.DATAFILE_TYPE_URI, false)
    .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
      let parentType = http.response.result[0];
      let key = parentType.uri;
      this.rdf_types[key] = parentType.name;
      for (let i = 0; i < parentType.children.length; i++) {   
        let key = parentType.children[i].uri;
        this.rdf_types[key] = parentType.children[i].name;
        if (this.$opensilex.Oeso.checkURIs(key, this.$opensilex.Oeso.IMAGE_TYPE_URI)) {
          let imageType = parentType.children[i];
          this.images_rdf_types.push(imageType.uri);
          for (let i = 0; i < imageType.children.length; i++) {
            let key = imageType.children[i].uri;
            this.rdf_types[key] = imageType.children[i].name;
            this.images_rdf_types.push(key);
          }
        } else if (this.$opensilex.Oeso.checkURIs(key, this.$opensilex.Oeso.SPECTRA_TYPE_URI)) {
          let spectraType = parentType.children[i];
          this.images_rdf_types.push(spectraType.uri);
          for (let i = 0; i < spectraType.children.length; i++) {
            let key = spectraType.children[i].uri;
            this.rdf_types[key] = spectraType.children[i].name;
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

  redirectToDetail(uri){
    let uriArr: string[] = uri.split('/');
    if(uriArr[1] === "scientific-object"){
      this.$router.push({
        path:
          "/scientific-objects/details/" +
          encodeURIComponent(uri),
      })
    }
    if(uriArr[1] === "device"){
      this.$router.push({
        path:
          "/device/details/" +
          encodeURIComponent(uri),
      })
    }
    else {
      this.$emit("redirectToDetail")
    }
  }

  resetSelected() {
        this.tableRef.resetSelected();
    }

    clickOnlySelected() {
        this.tableRef.clickOnlySelected();
    }

    createEvents() {
        this.showEventForm = true;
        this.$nextTick(() => {
            this.updateSelectedUris();
            this.eventCsvForm.show();
        });
    }

    get onlySelected() {
        return this.tableRef.onlySelected;
    }

        updateSelectedUris() {
        this.selectedUris = [];
        for (let select of this.tableRef.getSelected()) {
            this.selectedUris.push(select.uri);
        }
    }

      get lang() {
        return this.$store.getters.language;
    }

    exportDataFiles(format, includeAverage, includeSampleDatetime) {
        let exportList = this.tableRef.getSelected().map(select => select.uri);

        if(format == "img") {
            let filenames = this.tableRef.getSelected().map(select => select.filename);
            for (let i = 0; i < filenames.length; i++) {
                let path = "/core/datafiles/" + encodeURIComponent(exportList[i]) + "/thumbnail?scaled_width=600&scaled_height=600";
                this.$opensilex.downloadFilefromService(
                    path,
                    filenames[i],
                    undefined,
                    null
                ).then((http: HttpResponse<OpenSilexResponse<any>>) => {
                  if (http && http.status === 200) {
                    this.$opensilex.showSuccessToast(this.$i18n.t("DataFilesList.upload-success-message"));
                  }
                })
                .catch((error) => {
                  if (error.status == 500) {
                    console.error("DataFile not found", error);
                    this.$opensilex.errorHandler(
                      error,
                      this.$t("DataFileForm.error.datafile-not-found")
                    );
                  } else {
                      this.$opensilex.errorHandler(error);
                  }
                });
        }} else {
          let today = new Date();
          //filename example if avg and datetime options are checked : export_datafiles_avg_WithDatetime_2024-11-27 
          let filename =
            "export_datafiles" +
            (includeAverage ? "_avg_" : "_") +
            (includeSampleDatetime ? "_WithDatetime_" : "_") +
            this.$opensilex.$dateTimeFormatter.formatISODate(today);
            let path = "/core/datafiles/export-spectra-files?format=" + format + "&includeAverage=" + includeAverage + "&includeSampleDatetime=" + includeSampleDatetime;
            return this.$opensilex.downloadFilefromPostService(
                path,
                filename,
                format,
                exportList,
                this.lang
            ).then((http: HttpResponse<OpenSilexResponse<any>>) => {
              if (http && http.status === 200) {
                this.$opensilex.showSuccessToast(this.$i18n.t("DataFilesList.upload-success-message"));
              }
            })
            .catch((error) => {
              if (error.status == 500) {
                console.error("DataFile not found", error);
                this.$opensilex.errorHandler(
                  error,
                  this.$t("DataFileForm.error.datafile-not-found")
                );
              } else {
                this.$opensilex.errorHandler(error);
              }
            });
          }
    }

    deleteDatafile(uri: string) {
      this.$bvModal
        .msgBoxConfirm(
          this.$t("component.common.delete-datafile-confirmation").toString(),
          {
            cancelTitle: this.$t("component.common.cancel").toString(),
            okTitle: this.$t("component.common.delete").toString(),
            okVariant: "danger",
            centered: true
          }
        )
        .then(confirmation => {
          if (confirmation) {
            this.$opensilex
              .getService("opensilex.DataService")
              .deleteDatafile(uri)
              .then(() => {
                this.tableRef.checkSelectedItems(uri);
                this.refresh();
                let message = uri + " " + this.$i18n.t("component.common.success.delete-success-message");
                this.$opensilex.showSuccessToast(message);
              })
              .catch(this.$opensilex.errorHandler);
          }
        });
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>
  en:
    DataFilesList:
      add: Add datafiles
      format: Format
      filename: Filename
      selected: Selected datafiles
      selected-all: All datafiles
      export: Export datafiles
      exportDX: Export DX datafiles to DX datafile
      exportTSVAVG: Export DX datafiles to TSV datafile with average
      exportTSV: Export DX datafiles to TSV datafile
      exportCSV: Export CSV datafiles to CSV datafile
      exportTSVDATE: Export DX datafiles to TSV datafile with datetime
      error:
        datafile-not-found: Datafile not found
      upload-success-message: File uploaded and processed successfully.
  
  fr:
    DataFilesList:
      add: Ajouter un fichier de données
      format: Format
      filename: Nom du fichier
      selected: Fichiers de données selectionnés
      selected-all: Tous les fichiers
      export: Exporter des fichiers de données
      exportDX: Export des fichiers de données DX 
      exportTSVAVG: Export des fichiers de données DX au format TSV avec moyenne 
      exportTSV: Export des fichiers de données DX au format TSV
      exportCSV: Export des fichiers de données CSV
      exportTSVDATE: Export des fichiers de données DX au format TSV avec la date  
      error:
        datafile-not-found: Fichiers de données non trouvés
      upload-success-message: Fichier téléchargé et traité avec succès.

  
  </i18n>