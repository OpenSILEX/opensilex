<template>
<div>
      <h3 v-if="datafile">{{ $t("DataDetails.datafile") }}</h3>
      <h3 v-else>{{ $t("DataDetails.data") }}</h3>
      <pre class="testpre">
        <div>{ </div>
          <div>  "uri": {{ value.data.uri ? '"' + value.data.uri + '"' : "null"}},</div>
          <div>  "date": {{ value.data.date ? '"' + value.data.date + '"' : "null" }},</div>
          <!-- <div>  "target": {{ value.data.target ? '"' + value.data.target + '"' : "null" }},</div> -->
          <!-- TARGET -->
          <div class="dataModalTarget">  "target":
            <template v-if="value.data.target">
              <opensilex-UriLink
                :uri="value.data.target"
                :value="value.data.target"
                :to="{path: '/scientific-objects/details/' + encodeURIComponent(value.data.target)}"
                class="dataDetailLinks"
              ></opensilex-UriLink>
            </template><template v-else> {{"null"}}</template>
          </div>
          <!-- <div>  "variable": {{ value.data.variable ? '"' + value.data.variable + '"' : "null" }},</div> -->
          <!-- VARIABLES  -->
          <div class="dataModalVariable">  "variable": 
            <template v-if="value.data.variable">
              <opensilex-UriLink
                :uri="value.data.variable"
                :value="value.data.variable"
                :to="{path: '/variable/details/' + encodeURIComponent(value.data.variable)}"
                class="dataDetailLinks"
              ></opensilex-UriLink>
            </template><template v-else> {{"null"}}</template>
          </div>
          <div>  "value": {{ value.data.value ? value.data.value : "null" }},</div>
          <div>  "confidence": {{ value.data.confidence ? '"' + value.data.confidence + '"' : "null" }},</div>
          <!-- PROVENANCE  -->
          <div>  "provenance": {</div>
            <!-- <div>    "uri": {{ value.data.provenance.uri ? '"' + value.data.provenance.uri + '"' : "null" }},</div> -->
      <span>"uri" : <template v-if="value.data.provenance.uri"><opensilex-UriLink :uri="value.data.provenance.uri" :value="value.data.provenance.uri" :to="{path: '/provenances/details/' + encodeURIComponent(value.data.provenance.uri)}" class="dataDetailLinks"></opensilex-UriLink>
            </template><template v-else> {{"null"}}</template></span>
            <!-- PROV_USED -->
            <div>    "prov_used": [ </div>
              <template v-if="value.data.provenance.prov_used && value.data.provenance.prov_used.length > 0">
                <div v-for="(prov, index) in value.data.provenance.prov_used" :key="index" class="testProvUsedWithLineheight">
        {
          <span class="dataDetailProvUsedUri">"uri": <span @mouseenter="showProvUsedModal = true" @mouseover="handleMouseOver">{{ '"' + prov.uri + '"' }}</span></span>
          <span class="dataDetailRdfType">"rdf_type": <span>{{ '"' + prov.rdf_type + '"' }}</span></span>
        },</div>
        </template><template v-else>
          {{"null"}}
        </template>
          <div>    ], </div>
        
            <!-- <div>    "prov_used": [ </div>
              <div>      { </div>
                <div>        "uri": <span @mouseenter="showProvUsedModal = true" @mouseover="handleMouseOver">{{ value.data.provenance.prov_used ? '"' + value.data.provenance.prov_used[0].uri + '"' : "null" }}</span>,</div>
                <div>        "rdf_type": {{ value.data.provenance.prov_used ? '"' + value.data.provenance.prov_used[0].rdf_type + '"' : "null" }}</div>
              <div>      } </div>
            <div>    ], </div> -->

            <!-- PROV_WAS_ASSOCIATED_WITH            ----- > gérer fait que ce puisse être device OU person ? -->     
            <div>    "prov_was_associated_with": [ </div>
              <template v-if="value.data.provenance.prov_was_associated_with && value.data.provenance.prov_was_associated_with.length > 0">
                <div v-for="(provAssociated, provAssoIndex) in value.data.provenance.prov_was_associated_with" :key="provAssoIndex" class="testProvUsedWithoutLineheight">
        {
          <span class="dataDetailProvAssociatedUri">"uri": {{ '"' + provAssociated.uri + '"'}}</span>
          <span class="dataDetailProvAssociatedRdfType">"rdf_type": {{ '"' + provAssociated.rdf_type + '"' }}</span>
        },</div>
        </template><template v-else>
          {{"null"}}
        </template>
          <div>    ], </div>
              <!-- <div>      { </div>
                <div>        "uri": {{ value.data.provenance.prov_was_associated_with ? '"' + value.data.provenance.prov_was_associated_with[0].uri + '"' : "null" }},</div>
                <div>        "rdf_type": {{ value.data.provenance.prov_was_associated_with ? '"' + value.data.provenance.prov_was_associated_with[0].rdf_type + '"' : "null" }}</div>
              <div>      } </div> -->

            <!-- SETTINGS -->
            <!-- <div>    "settings": {{ value.data.provenance.settings ? '"' + value.data.provenance.settings + '"' : "null" }},</div> -->
            <div>    "settings": </div>
              <div class="dataModalSettings">
              <template v-if="value.data.provenance.settings && value.data.provenance.settings.length != 0">
                <span v-for="(value, setting) in value.data.provenance.settings" :key="setting" class="dataDetailSettings">
        "{{ setting }}": "{{ value }}",
        </span>
        </template><template v-else>
      {{"null"}}
      </template>
            </div>

            <!-- <div>    "experiments": [ </div>
              <div>        {{ value.data.provenance.experiments? '"' + value.data.provenance.experiments[0] + '"' : "null" }}</div>
            <div>    ] </div> -->
            <!-- EXPERIMENTS -->
          <div class="dataModalExperiments">    "experiments": [ </div>
            <template v-if="value.data.provenance.experiments && value.data.provenance.experiments.length > 0">
              <div v-for="(provExpe, expeIndex) in value.data.provenance.experiments" :key="expeIndex" class="testProvUsedWithoutLineheight">

        <opensilex-UriLink
            :uri="provExpe"
            :value="provExpe"
            :to="{path: '/experiment/details/' + encodeURIComponent(provExpe)}"
            class="dataDetailLinks"
        ></opensilex-UriLink>
              </div>
            </template><template v-else> {{"null"}}</template>
            <div>    ] </div>
          <div>  }, </div>

          <!-- METADATA -->
          <div>  "metadata": </div>
          <div class="dataModalMetaData">
            <template v-if="value.data.metadata && value.data.metadata.length != 0">
            <span v-for="(value, metadata) in value.data.metadata" :key="metadata" class="dataDetailMetaData">
      "{{ metadata }}": "{{ value }}",
            </span>
  </template><template v-else>
    {{"null"}},
  </template>
  </div>
          
          <!-- <div> {{ value.data.metadata ? '"' + value.data.metadata + '"' : "null" }},</div> -->
          <div>  "raw_data": {{ value.data.raw_data ? '"' + value.data.raw_data + '"' : "null" }}</div>
        <div>} </div>
      </pre>

      <!-- Fenêtre modale prov_used datafile -->
      <div v-if="showProvUsedModal" class="modal-container" v-on:mouseenter="showProvUsedModal = true" v-on:mouseleave="showProvUsedModal = false" :style="{ top: topPosition + 'px', left:leftPosition + 'px' }">
        <div class="modal-content">
          <img class="modal-image"  v-bind:src="modalImageURL" alt="Mon image ici">
          <!-- <img v-for="image in modalImageURL" :key="image" class="modal-image"  v-bind:src="modalImageURL[image]" alt="Mon image ici"> -->
          <pre class="modal-text" ref="modalText">{{ modalText }}</pre>
        </div>
      </div>
        <!-- <span>test lien: <a href="http://localhost:8080/app/scientific-objects/details/http%3A%2F%2Fwww.phenome-fppn.fr%2Fm3p%2Farch%2F2017%2Fc17000002" class="coloredLink">lienTestVersOS</a> </span> -->


    <!-- <div>    "prov_used": [ </div>
                <template v-if="value.data.provenance.prov_used && value.data.provenance.prov_used.length > 0">
                    <div v-for="(prov, index) in value.data.provenance.prov_used" :key="index" class="testProvUsedWithLineheight">
            {
            <span class="dataDetailProvUsedUri">"uri": <span @mouseenter="showProvUsedModal = true" @mouseover="handleMouseOver">{{ '"' + prov.uri + '"' }}</span></span>
            <span class="dataDetailRdfType">"rdf_type": <span>{{ '"' + prov.rdf_type + '"' }}</span></span>
            },</div>
            </template><template v-else>
            {{"null"}}
            </template>
            <div>    ], </div> -->


        <!-- <div>{{ value}}</div> -->


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



  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    if (this.value.data.provenance.prov_used!= undefined && this.value.data.provenance.prov_used != null) {
      this.showImage(this.value.data.provenance.prov_used[0])
      this.getDataFileDescription(this.value)
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
        console.log(this.value)
    let path = "/core/datafiles/" + encodeURIComponent(item.uri) + "/thumbnail?scaled_width=400&scaled_height=400";
      let promise = this.$opensilex.viewImageFromGetService(path);
      promise.then((result) => {
        this.modalImageURL = result;
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
.testpre{
  line-height: 0.5;
  font-size: 1em;
  overflow: hidden;
}
.testpre div, span { 
  font-family: monospace, monospace;
}


.testProvUsedWithLineheight {
  line-height: 1;
  margin-top: -10px;
}
.testProvUsedWithoutLineheight {
  // line-height: 1;
  margin-top: -10px;
}

// .coloredLink {
//   color: #007bff
// }

.dataModalVariable, .dataModalTarget, .dataModalExperiments {
  display: flex;
  margin: -5px 0 -5px 0;
  
}

.uri {
  line-height: normal !important;
  margin-top: -5px;
  
}

// opensilex-UriLink * {
// overflow: initial !important;
// overflow-x: clip !important;
// }

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

.dataDetailProvUsedUri, .dataDetailProvAssociatedUri {
  line-height: 2;
}

.dataDetailRdfType, .dataDetailProvAssociatedRdfType, .dataDetailMetaData, .dataDetailSettings {
  line-height: 1;
}

.dataModalMetaData, .dataModalSettings {
  margin-top: -20px;
  margin-bottom: -20px;
}


</style>

<i18n>
  fr: 
    DataDetails:
      data : Donnée
      datafile : Fichier de données
      

  en: 
    DataDetails:
      data : Data
      datafile : Datafile
</i18n>