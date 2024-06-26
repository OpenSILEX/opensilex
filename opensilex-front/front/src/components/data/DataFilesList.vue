<template>
  <div>
    <opensilex-TableAsyncView
      ref="tableRef"
      :searchMethod="searchDatafiles"
      :fields="fields"
      defaultSortBy="name"
    >
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
  </div>
</template>

<script lang="ts">
import { Prop, Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import { ProvenanceGetDTO, ResourceTreeDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import {OntologyService} from "opensilex-core/api/ontology.service";

@Component
export default class DataFilesList extends Vue {
  $opensilex: any;
  $store: any;
  service: any;
  ontologyService: OntologyService;
  routeArr : string[] = this.$route.path.split('/');

  disabled = false;
  imageUrl = null;

  @Prop({
    default: () => {
      return {
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

  get fields() {
    let fields: any = [
      {
        key: "date",
        label: "DataView.list.date",
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
      this.dataProvenanceModalView.setProvenance(value);
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

  searchDatafiles(options) {
    let provUris = this.$opensilex.prepareGetParameter(this.filter.provenance);
    if (provUris != undefined) {
      provUris = [provUris];
    }

    return new Promise((resolve, reject) => {
        this.service.getDataFileDescriptionsByTargets(
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
}
</script>

<style scoped lang="scss">
</style>
