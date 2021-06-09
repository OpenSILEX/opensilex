<template>
  <div ref="page">
    <opensilex-DeviceVisualizationForm :device="device" @search="onSearch"></opensilex-DeviceVisualizationForm>

    <div class="d-flex justify-content-center mb-3" v-if="!isGraphicLoaded">
      <b-spinner label="Loading..."></b-spinner>
    </div>

    <opensilex-DataVisuGraphic
      v-if="isGraphicLoaded"
      ref="visuGraphic"
      :deviceType="true"
      @dataAnnotationIsClicked="showAnnotationForm"
    ></opensilex-DataVisuGraphic>

    <opensilex-AnnotationModalForm ref="annotationModalForm" @onCreate="onAnnotationCreated"></opensilex-AnnotationModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import moment from "moment-timezone";
import Highcharts from "highcharts";
// @ts-ignore
import {
  DevicesService,
  DataGetDTO,
  ProvenanceGetDTO
} from "opensilex-core/index";
// @ts-ignore
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

  isGraphicLoaded = true;
  form;
  selectedVariable;
  devicesService: DevicesService;
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;
  @Ref("page") readonly page!: any;

  created() {
    if (this.device == null) {
      this.device = decodeURIComponent(this.$route.params.uri);
    }

    this.devicesService = this.$opensilex.getService(
      "opensilex.DevicesService"
    );
  }

  onAnnotationCreated() {
    this.visuGraphic.updateDataAnnotations();
  }

  showAnnotationForm(target) {
    this.annotationModalForm.showCreateForm([target]);
  }

  onSearch(form) {
    this.isGraphicLoaded = false;
    if (form.variable) {
      this.form = form;

      this.$opensilex
        .getService("opensilex.VariablesService")
        .getVariable(form.variable)
        .then((http: HttpResponse<OpenSilexResponse>) => {
          this.selectedVariable = http.response.result;
          const datatype = this.selectedVariable.datatype.split("#")[1];
          if (datatype == "decimal" || datatype == "integer") {
            this.prepareGraphic();
          } else {
            this.$opensilex.showInfoToast(
              this.$i18n.t("DeviceDataTab.datatypeMessageA") +
                " " +
                datatype +
                " " +
                this.$i18n.t("DeviceDataTab.datatypeMessageB")
            );
          }
        })
        .catch(error => {
          this.isGraphicLoaded = true;
          this.$opensilex.errorHandler(error);
        });
    }
  }

  prepareGraphic() {
    if (this.form) {
      let promise = this.buildDataSerie();
      promise
        .then(value => {
          this.isGraphicLoaded = true;
          let serie;
          if (value) {
            serie = [value];
          } else {
            serie = [];
          }
          this.$nextTick(() => {
            this.visuGraphic.reload(serie, this.selectedVariable, false);
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
        ["date=asc"], //order by
        0,
        50000
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<DataGetDTO>>>) => {
        const data = http.response.result as Array<DataGetDTO>;
        let dataLength = data.length;

        if (dataLength > 0) {
          const cleanData = this.dataTransforme(data);
          if (dataLength > 50000) {
            this.$opensilex.showInfoToast(
              this.$i18n.t("DeviceDataTab.limitSizeMessageA") +
                " " +
                dataLength +
                " " +
                this.$i18n.t("DeviceDataTab.limitSizeMessageB")
            );
          }

          return {
            type: "line",
            name: this.selectedVariable.name,
            data: cleanData,
            visible: true
          };
        }
      });
  }

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
        datatypeMessageA: The variable datatype is
        datatypeMessageB: "At this time only decimal or integer are accepted"
        limitSizeMessageA : "There are "
        limitSizeMessageB : " data .Only the 50 000 first data are displayed."

fr:
    DeviceDataTab:
        visualization: Visualisation
        data: Données
        add: Ajouter des données
        datatypeMessageA:  le type de donnée de la variable est
        datatypeMessageB: "Pour le moment, seuls les types decimal ou entier sont acceptés "
        limitSizeMessageA : "Il y a "
        limitSizeMessageB : " données .Seules les 50 000 premières valeurs sont affichées. "

</i18n>

