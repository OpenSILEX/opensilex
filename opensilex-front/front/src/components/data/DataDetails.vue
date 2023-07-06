<template>
  <div class="dataDetailsComponentContainer">
    <h3 v-if="datafile">{{ $t("DataDetails.datafile") }}</h3>
    <h3 v-else>{{ $t("DataDetails.data") }}</h3>

    <div class="mainPropertiesContainer"><span class="mainProperties">URI :</span><span> {{ value.data.uri ? '"' + value.data.uri + '"' : "Null"}}</span></div>

    <!-- rdfType pour datafiles -->

    <div class="mainPropertiesContainer"><span class="mainProperties">Date : </span><span>{{ value.data.date ? '"' + value.data.date + '"' : "Null" }}</span></div>

    <!-- timezone pour datafiles -->

    <!-- TARGET -->
    <div class="mainPropertiesContainer">
      <span class="mainProperties">{{ $t("DataView.list.object") }} : </span>
      <span>
        <template v-if="value.data.target">
          <opensilex-UriLink
            :uri="value.data.target"
            :value="value.data.target"
            :to="{path: '/scientific-objects/details/' + encodeURIComponent(value.data.target)}"
            class="dataDetailLinks"
          ></opensilex-UriLink>
        </template><template v-else> {{"Null"}}</template>
      </span>
    </div>

    <!-- VARIABLES  que pour data -->
    <div class="mainPropertiesContainer">
      <span class="mainProperties">Variable : </span>
      <span>
        <template v-if="value.data.variable">
          <opensilex-UriLink
            :uri="value.data.variable"
            :value="value.data.variable"
            :to="{path: '/variable/details/' + encodeURIComponent(value.data.variable)}"
            class="dataDetailLinks"
          ></opensilex-UriLink>
        </template><template v-else> {{"Null"}}</template>
      </span>
    </div>

    <div class="mainPropertiesContainer"><span class="mainProperties">{{ $t("DataView.list.value") }} : </span><span>{{ value.data.value ? value.data.value : "Null" }}</span></div>
    <div class="mainPropertiesContainer"><span class="mainProperties">{{ $t("DataDetails.confidence") }} : </span><span> {{ value.data.confidence ? '"' + value.data.confidence + '"' : "Null" }}</span></div>

    <!-- PROVENANCE  -->
    <div class="mainPropertiesContainer">
      <div class="mainProperties">Provenance : </div>
      <div class="provenancePropertiesContainter">
        <div>
          <span class="provenanceProperties">URI : </span>
          <span>
            <template v-if="value.data.provenance.uri">
              <opensilex-UriLink :uri="value.data.provenance.uri" :value="value.data.provenance.uri" :to="{path: '/provenances/details/' + encodeURIComponent(value.data.provenance.uri)}" class="dataDetailLinks"></opensilex-UriLink>
            </template><template v-else> {{"Null"}}</template>
          </span>
        </div>


          <!-- OK -- 1/ binding de class pour toutes les propriétés qui vont a la ligne
          si valeur nulle on veut une autre classe pour la div "provenanceProperties" qui place le null en inline : 
          prov_used / prov_was_asso / settings / expe / metadata -->

          <!-- 2/ boucle sur les images de la prov_used (voir aussi modif au niveau de l'appel au service, du type de modalImage et du template) -->

          <!-- 3/ Gérer les propriétées exclusives : Template DATA / DATAFILE  ex propriété de fuseau horaire  -->

          <!-- 4/ continuer les clés de traductions  (prov_used et prov_was_asso_with ?) + les champs spécifiques aux datafiles (voir point ci-dessus)-->

          <!-- OK -- 5/ faire la redirection du prov_was_asso s'il s'agit d'un device vers sa page de détail. cela peut être une personne mais on ne le gère pas
          pour l'instant puisqu'une personne n'a pas de page de détail vers laquelle renvoyer -->

        <!-- PROV_USED -->
        <div
          v-bind:class="{hasNoProvUsed:!hasProvUsed}"
        >
          <div class="provenanceProperties">Prov_used : </div>
          <div class="dataModalProvUsed">
            <template v-if="value.data.provenance.prov_used && value.data.provenance.prov_used.length > 0">
              <div v-for="(prov, index) in value.data.provenance.prov_used" :key="index" class="provUsedElement">
                  <div class="indentedDataProperty">URI : <span class="dataDetailProvUsedUri" @mouseenter="showProvUsedModal = true" @mouseover="handleMouseOver">{{ '"' + prov.uri + '"' }}</span></div>
                  <div class="indentedDataProperty">RDF_type : <span>{{ '"' + prov.rdf_type + '"' }}</span>,</div>
              </div>
            </template><template v-else>
              {{"Null"}}
            </template>
          </div>
        </div>

              
        <!-- PROV_WAS_ASSOCIATED_WITH --> 
        <div
          v-bind:class="{hasNoProvAssociated:!hasProvAssociated}"
        >    
          <div class="provenanceProperties">Prov_was_associated_with : </div>
          <div class="dataModalProvAssociated">
            <template v-if="value.data.provenance.prov_was_associated_with && value.data.provenance.prov_was_associated_with.length > 0">
              <div v-for="(provAssociated, provAssoIndex) in value.data.provenance.prov_was_associated_with" :key="provAssoIndex">
                <!-- for each element, if the URI is from a device, redirect to details page, is she is from (operator: person) just display it  -->

                <div class="provAssociatedDevice" v-if="provAssociated.uri && provAssociated.uri.split('/')[1] === 'device'">
                  <div class="indentedDataProperty">
                    <span>URI: </span>
                    <opensilex-UriLink
                      :uri="provAssociated.uri"
                      :value="provAssociated.uri"
                      :to="{path: '/device/details/' + encodeURIComponent(provAssociated.uri)}"
                      class="dataDetailLinks"
                    ></opensilex-UriLink>
                  </div>
                  <div class="indentedDataProperty">RDF_type : {{ '"' + provAssociated.rdf_type + '"' }},</div>
                </div>

                <div class="provAssociatedPerson" v-else>
                  <div class="indentedDataProperty">URI : {{ '"' + provAssociated.uri + '"'}}</div>
                  <div class="indentedDataProperty">RDF_type : {{ '"' + provAssociated.rdf_type + '"' }},</div>
                </div>
              </div>
            </template><template v-else>
              {{"Null"}}
            </template>
          </div>
        </div>


        <!-- SETTINGS -->
        <div 
          v-bind:class="{hasNoSettings:!hasSettings}"
        >
          <div class="provenanceProperties">{{ $t("DataDetails.settings") }} :</div>
          <div class="dataModalSettings">
            <template v-if="value.data.provenance.settings && value.data.provenance.settings.length != 0">
              <div v-for="(value, setting) in value.data.provenance.settings" :key="setting" class="indentedDataProperty">
                "{{ setting }}": "{{ value }}",
              </div>
            </template><template v-else>
              {{ "Null" }}
            </template>
          </div>
        </div>

        <!-- EXPERIMENTS -->
        <div
          v-bind:class="{hasNoExperiments:!hasExperiments}"
        >
          <div class="provenanceProperties">{{ $t("DataDetails.experiments") }} : </div>
          <div class="dataModalExperiments">
            <template v-if="value.data.provenance.experiments && value.data.provenance.experiments.length > 0">
              <div v-for="(provExpe, expeIndex) in value.data.provenance.experiments" :key="expeIndex">
                <opensilex-UriLink
                  :uri="provExpe"
                  :value="provExpe"
                  :to="{path: '/experiment/details/' + encodeURIComponent(provExpe)}"
                  class="dataDetailLinks indentedDataProperty"
                ></opensilex-UriLink>
              </div>
            </template><template v-else> {{"Null"}}</template>
          </div>
        </div>
        <!-- Fin  provenance sous partie  -->
      </div>
    </div>

    <!-- METADATA -->
   
      <div class="mainPropertiesContainer"
      v-bind:class="{hasNoMetadata:!hasMetadata}"
      >
        <div class="mainProperties">Metadata:</div>
        <div class="dataModalMetadata">
          <template v-if="value.data.metadata && value.data.metadata.length != 0">
            <div v-for="(value, metadata) in value.data.metadata" :key="metadata" class="indentedDataProperty">
              "{{ metadata }}": "{{ value }}",
            </div>
          </template><template v-else>
            {{"Null"}},
          </template>
        </div>
      </div>

      <!-- archive pour datafiles -->

      <!-- filename pour datafiles  -->
   

    <!-- RAW_DATA   que dans data-->
    <div class="mainPropertiesContainer">
      <span class="mainProperties">Raw_data :</span>
      <template v-if="value.data.raw_data && value.data.raw_data.length != 0">
        <div class="indentedDataProperty">
          "{{ value.data.raw_data }}",
        </div>
      </template><template v-else>
        {{"Null"}},
      </template>
    </div>

    <!-- Fenêtre modale prov_used datafile -->
    <div v-if="showProvUsedModal" class="modal-container" v-on:mouseenter="showProvUsedModal = true" v-on:mouseleave="showProvUsedModal = false" :style="{ top: topPosition + 'px', left:leftPosition + 'px' }">
      <div class="modal-content">
        <img class="modal-image"  v-bind:src="modalImageURL" alt=" The image cannot be displayed">
        <!-- <img v-for="image in modalImageURL" :key="image" class="modal-image"  v-bind:src="modalImageURL[image]" alt="Mon image ici"> -->
        <pre class="modal-text" ref="modalText">{{ modalText }}</pre>
      </div>
    </div>

  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component, Prop } from "vue-property-decorator";
import {DataService} from "opensilex-core/api/data.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {DataFileGetDTO} from "opensilex-core/model/dataFileGetDTO";

@Component
export default class DataDetails extends Vue {
  @Prop({
    default: false
  })
  datafile: boolean;

  @Prop()
  value;


  data: any = null;
  provenance: any = null;

    showProvUsedModal: boolean = false;
    modalImageURL: string = "";
   // modalImageURL: Array<string> = []; // devenir un Array<string> ?
    modalText: any = ''; // devenir un Array<any> ?
  topPosition = 0;
  leftPosition = 0;
  dataService: DataService;
  $opensilex: OpenSilexVuePlugin;

  hasProvUsed: boolean = false;
  hasProvAssociated: boolean = false;
  hasSettings: boolean = false;
  hasExperiments: boolean = false;
  hasMetadata: boolean = false;




  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    if (this.value.data.provenance.prov_used!= undefined && this.value.data.provenance.prov_used != null) {
      // for (let prov in this.value.data.provenance.prov_used) {
      //   this.showImage(prov)
      // }
      this.showImage(this.value.data.provenance.prov_used[0])
      this.getDataFileDescription(this.value)
    }

    console.log("this.value : ", this.value);

    if (this.value.data.provenance.prov_used && this.value.data.provenance.prov_used != null) {
      this.hasProvUsed = true;
    }

    if (this.value.data.provenance.prov_was_associated_with && this.value.data.provenance.prov_was_associated_with != null) {
      this.hasProvAssociated = true;
    }

    if (this.value.data.provenance.experiments && this.value.data.provenance.experiments.length > 0) {
      this.hasExperiments = true;
    }

    if (this.value.data.provenance.settings && this.value.data.provenance.settings != undefined) {
      this.hasSettings = true;
    }

    if (this.value.data.metadata && this.value.data.metadata != null){
      this.hasMetadata = true;
    }
  }

    redirectToDetail(ProvAssoUri){
    let uriArr: string[] = ProvAssoUri.split('/');
    if(uriArr[1] === "device"){
      this.$router.push({
        path:
          "/device/details/" +
          encodeURIComponent(ProvAssoUri),
      })
    }
    // if(uriArr[1] === "persons"){
    //   this.$router.push({
    //     path:
    //       "/persons",
    //   })
    // }
    else {
      this.$emit("redirectToDetail")
    }
  }

  handleMouseOver(event: MouseEvent): void {
    this.topPosition = event.clientY - 200;
    this.leftPosition = event.clientX - 580;
  }


      // public getPicturesThumbnails(uri: string, scaled_width?: number, scaled_height?: number, observe?: 'response', headers?: Headers)
      //    public getDataFileDescription(uri: string, observe?: 'response', headers?: Headers)
      //    public getDataFile(uri: string, observe?: 'response', headers?: Headers)


      // getDataFileImage(value){
      //   return this.dataService.getPicturesThumbnails(
      //     value.data.provenance.prov_used[0].uri,
      //     400,
      //     400
      //   )
      //   .then((http: HttpResponse<OpenSilexResponse<any>>) => {
      //     this.modalImageURL = http.response.result;
      //     return http.response.result;
      //   });
      // }



  showImage(item: any) {
    console.log("showImage init")
    let path = "/core/datafiles/" + encodeURIComponent(item.uri) + "/thumbnail?scaled_width=400&scaled_height=400";
      let promise = this.$opensilex.viewImageFromGetService(path);
      promise.then((result) => {
        this.modalImageURL = result;
        // this.modalImageURL.push(result);
      })    
  }


  getDataFileDescription(value) {
    return this.dataService
      .getDataFileDescription(value.data.provenance.prov_used[0].uri)
      .then((http: HttpResponse<OpenSilexResponse<DataFileGetDTO>>) => {
        this.modalText = JSON.stringify(http.response.result, null, 2);

        return http.response.result;
      }
    );
  }


}
</script>

<style scoped lang="scss">
.provUsedElement {
  margin-top: 15px;
  margin-bottom: 15px;
}

.modal-container {
  position: absolute;
  // top: 150px;
  // right: 50px;
  width: 400px;
  height: 400px;
  background-color: lavender;


  // top: 0;
  // left: 100%;
  // z-index: 999;
  // padding: 10px;
  // white-space: nowrap;
  // max-width: calc(100vw - 100%);
}

.modal-image {
  max-height: 400px;
  max-width: 400px;
}

.dataDetailProvUsedUri:hover {
  cursor: pointer;
  color: #007bff;
}

.dataDetailsComponentContainer {
  margin-bottom: 30px;
}

.mainPropertiesContainer {
  margin-top: 10px;
  margin-bottom: 10px;
}

.mainProperties {
  font-size: 1.2em;
  font-weight: bold;
  color: #00A38D;
}

.provenancePropertiesContainter {
    // margin-left: 20px;
    // margin-top: 10px;
  margin: 10px 0 10px 20px;
}

.provenanceProperties {
  font-size: 1.2;
  font-weight: bold;
  margin-bottom: 10px;
  margin-top: 10px
}

.indentedDataProperty {
  margin-left: 20px
}

.hasNoSettings, .hasNoExperiments, .hasNoMetadata, .hasNoProvAssociated, .hasNoProvUsed {
  display: flex;

  .dataModalSettings, .dataModalExperiments, .dataModalMetadata, .dataModalProvAssociated, .dataModalProvUsed {
    margin-top: 10px;
    margin-left: 10px
  }
}

.hasNoMetadata {
  .dataModalMetadata {
    margin-top: 3px;
  }
}

.provAssociatedDevice, .provAssociatedPerson {
  margin-bottom: 10px
}
// .dataDetailProvUsedUri, .dataDetailProvAssociatedUri {
//   line-height: 2;
// }

// .dataDetailRdfType, .dataDetailProvAssociatedRdfType, .dataDetailMetaData, .dataDetailSettings {
//   line-height: 1;
// }

// .dataModalMetaData, .dataModalSettings {
//   margin-top: -20px;
//   margin-bottom: -20px;
// }


</style>

<i18n>
  fr: 
    DataDetails:
      data : Donnée
      datafile : Fichier de données
      confidence : Indice de confiance
      experiments: Expérimentations
      settings: Paramètres
      

  en: 
    DataDetails:
      data : Data
      datafile : Datafile
      confidence: Confidence
      experiments: Experiments
      settings: Settings
</i18n>