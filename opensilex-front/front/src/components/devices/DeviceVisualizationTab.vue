<template>
  <div ref="page">
      <opensilex-DeviceVisualizationForm
         :device="device"
          @search="onSearch"
          @update="onUpdate"
          ></opensilex-DeviceVisualizationForm>

    <div class="d-flex justify-content-center mb-3" v-if="!isGraphicLoaded">
      <b-spinner label="Loading..."></b-spinner>
    </div>

    <opensilex-DataVisuGraphic
      v-show="isGraphicLoaded"
      ref="visuGraphic"
      :deviceType="true"
      @detailProvenanceIsClicked="showProvenanceDetailComponent"
      @graphicCreated="onGraphicCreated"
    ></opensilex-DataVisuGraphic>

    <opensilex-DataProvenanceModalView ref="dataProvenanceModalView"></opensilex-DataProvenanceModalView>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import moment from "moment-timezone";
import Highcharts from "highcharts";
import {
  DataService,
  DevicesService,
  DataGetDTO,
  ProvenanceGetDTO
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
@Component
export default class DeviceVisualizationTab extends Vue {
  $opensilex: any;

  @Prop()
  modificationCredentialId;

  get user() {
    return this.$store.state.user;
  }
  get credentials() {
    return this.$store.state.credentials;
  }

  @Prop()
  device;

  isGraphicLoaded = false;
  form;
  selectedVariable;
  devicesService: DevicesService;
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("dataProvenanceModalView") readonly dataProvenanceModalView!: any;
  @Ref("page") readonly page!: any;

  created() {
    if (this.device == null) {
      this.device = decodeURIComponent(this.$route.params.uri);
    }

    this.devicesService = this.$opensilex.getService(
      "opensilex.DevicesService"
    );
  }

  onGraphicCreated() {

    let that = this;
    setTimeout(function() {
      that.page.scrollIntoView({
        behavior: "smooth",
        block: "end",
        inline: "nearest"
      });
    }, 500);
  }

  
  onUpdate(form) {
    this.isGraphicLoaded = false;

    this.form = form;
    let promise = this.buildDataSerie();
    promise
      .then(value => {
        let serie;
        if (value) {
          serie = [value];
        } else {
          serie = [];
        }
        this.visuGraphic.reload(serie, this.selectedVariable);
        this.isGraphicLoaded = true;
      })
      .catch(error => {
        this.isGraphicLoaded = true;
        this.$opensilex.errorHandler(error);
      });
  }

  onSearch(form) {
    this.isGraphicLoaded = false;
    if (form.variable) {
      this.form = form;

      this.$opensilex
        .getService("opensilex.VariablesService")
        .getVariable(form.variable)
        .then((http: HttpResponse<OpenSilexResponse>) => {
          console.log(http.response.result);
          this.selectedVariable = http.response.result;
          console.log(this.form);
          let promise = this.buildDataSerie();
          promise
            .then(value => {
               let serie;
              if (value) {
                serie = [value];
              } else {
                serie = [];
              }
              this.visuGraphic.reload(serie, this.selectedVariable);
              this.isGraphicLoaded = true;
            })
            .catch(error => {
              this.isGraphicLoaded = true;
              this.$opensilex.errorHandler(error);
            });
        })
        .catch(error => {
          this.isGraphicLoaded = true;
          this.$opensilex.errorHandler(error);
        });
    }
  }

  buildDataSerie() {
    return this.devicesService
      .searchDeviceData(
        this.device,
        this.form.startDate != undefined && this.form.startDate != ""
          ? this.form.startDate
          : undefined,
        this.form.endDate != undefined && this.form.endDate != ""
          ? this.form.endDate
          : undefined,
        undefined, //timezone
        undefined, //experiment
        [this.form.variable],
        undefined, //min confidence
        undefined, //max confidence
        this.form.provenance ? [this.form.provenance] : undefined,
        undefined, //this.addMetadataFilter(),
        undefined, //order by
        0,
        1000000
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<DataGetDTO>>>) => {
        const data = http.response.result as Array<DataGetDTO>;
        if (data.length > 0) {
          const cleanData = this.dataTransforme(data);

          return {
            name: this.selectedVariable.name,
            data: cleanData,
            visible: true
          };
        }
      });
  }

// addMetadataFilter() {
//     let metadata = undefined;
//     if (
//       this.form.metadataKey != undefined &&
//       this.form.metadataKey != "" &&
//       this.form.metadataValue != undefined &&
//       this.form.metadataValue != ""
//     ) {
//       metadata =
//         '{"' + this.form.metadataKey + '":"' + this.form.metadataValue + '"}';
//       return metadata;
//     }
//   }


  // keep only date/value/uriprovenance properties
  dataTransforme(data) {
    let toAdd,
      cleanData = [];

    data.forEach(element => {
      let stringDateWithoutUTC =
        moment.parseZone(element.date).format("YYYYMMDD HHmmss") + "+00:00";
      let dateWithoutUTC = moment(stringDateWithoutUTC).valueOf();
      let highchartsDate = Highcharts.dateFormat(
        "%Y-%m-%dT%H:%M:%S",
        dateWithoutUTC
      );
      let offset = moment.parseZone(element.date).format("Z");
      toAdd = {
        x: dateWithoutUTC,
        y: element.value,
        offset: offset,
        dateWithOffset: highchartsDate + offset,
        dataUri: element.uri,
        provenanceUri: element.provenance.uri,
        data: element
      };
      cleanData.push(toAdd);
    });
    return cleanData;
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
  showProvenanceDetailComponent(value) {
    if (value.provenance != undefined && value.provenance != null) {
      this.getProvenance(value.provenance).then(provenance => {
        value.provenance = provenance;
        this.dataProvenanceModalView.setProvenance(value);
        this.dataProvenanceModalView.show();
      }).catch(error => {
          this.$opensilex.errorHandler(error);
        });
    }
  }


  timestampToUTC(time) {
    // var day = moment.unix(time).utc().format();
    var day = Highcharts.dateFormat("%Y-%m-%dT%H:%M:%S+0000", time);
    return day;
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    DeviceDataTab:
        visualization: Visualization
        data: Data
        add: Add data

fr:
    DeviceDataTab:
        visualization: Visualisation
        data: Données
        add: Ajouter des données
</i18n>

