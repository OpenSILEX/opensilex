<template>
  <div>
    <b-collapse v-model="showSearchComponent" class="mt-2">
      <opensilex-ExperimentDataVisuForm
        :selectedExperiment="selectedExperiment"
        :scientificObjects="scientificObjectsURI"
        @search="onSearch"
        @update="onUpdate"
      ></opensilex-ExperimentDataVisuForm>
    </b-collapse>

    <b-collapse v-model="showGraphicComponent" class="mt-2">
      <opensilex-DataVisuGraphic
        v-if="showGraphicComponent"
        ref="visuGraphic"
        @dataAnnotationIsClicked="showAnnotationForm"
        @graphicCreated="$emit('graphicCreated')"
      ></opensilex-DataVisuGraphic>
    </b-collapse>

    <opensilex-AnnotationModalForm ref="annotationModalForm" @onCreate="onAnnotationCreated"></opensilex-AnnotationModalForm>
  </div>
</template>

<script lang="ts">
import moment from "moment-timezone";
import Highcharts from "highcharts";
// @ts-ignore
import {
  DataService,
  DataGetDTO,
  ProvenanceGetDTO
} from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ExperimentDataVisuView extends Vue {
  $route: any;
  $opensilex: any;
  dataService: DataService;
  form;
  chartOptionsValue: any;
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;
  showSearchComponent: boolean = true;
  showGraphicComponent: boolean = true;

  selectedExperiment;
  selectedVariable;

  @Prop()
  selectedScientificObjects;

  get scientificObjectsURI() {
    return this.selectedScientificObjects.map(so => {
      return so.uri;
    });
  }

  onAnnotationCreated() {
    this.visuGraphic.updateDataAnnotations();
  }

  showAnnotationForm(target) {
    this.annotationModalForm.showCreateForm([target]);
  }

  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    this.$opensilex.disableLoader();
    this.selectedExperiment = decodeURIComponent(this.$route.params.uri);
  }

  onUpdate(form) {
    this.form = form;
    const datatype = this.selectedVariable.datatype.split("#")[1];
    if (datatype == "decimal" || datatype == "integer") {
      this.showGraphicComponent = false;
      this.$opensilex.enableLoader();
      this.loadSeries();
    } else {
      this.showGraphicComponent = false;
      this.$opensilex.showInfoToast(
        this.$i18n.t("ExperimentDataVisuView.datatypeMessageA") +
          " " +
          datatype +
          " " +
          this.$i18n.t("ExperimentDataVisuView.datatypeMessageB")
      );
    }
  }

  onSearch(form) {
    this.form = form;
    this.showGraphicComponent = false;
    this.$opensilex.enableLoader();

    this.$opensilex
      .getService("opensilex.VariablesService")
      .getVariable(form.variable)
      .then((http: HttpResponse<OpenSilexResponse>) => {
        this.selectedVariable = http.response.result;
        const datatype = this.selectedVariable.datatype.split("#")[1];
        if (datatype == "decimal" || datatype == "integer") {
          this.loadSeries();
        } else {
          this.showGraphicComponent = true;
          this.$opensilex.showInfoToast(
            this.$i18n.t("ExperimentDataVisuView.datatypeMessageA") +
              " " +
              datatype +
              " " +
              this.$i18n.t("ExperimentDataVisuView.datatypeMessageB")
          );
        }
      });
  }

  loadSeries() {
    if (this.selectedScientificObjects && this.form.variable) {
      this.buildSeries();
    }
  }

  buildSeries() {
    var promises = [];
    var promise;
    const series = [];
    let serie;
    this.dataService = this.$opensilex.getService("opensilex.DataService");

    promise = this.buidDataSeries();
    promises.push(promise);

    Promise.all(promises).then(values => {
      let series = [];
      let dataSeries;

      dataSeries = values[0];
      dataSeries.forEach(serie => {
        series.push(serie);
      });

      this.showGraphicComponent = true;
      this.$nextTick(() => {
        this.visuGraphic.reload(series, this.selectedVariable, false);
      });
    });
  }

  buidDataSeries() {
    let series = [],
      serie;
    let promises = [],
      promise;
    this.selectedScientificObjects.forEach((element, index) => {
      promise = this.buildDataSerie(element);
      promises.push(promise);
    });
    return Promise.all(promises).then(values => {
      values.forEach(serie => {
        if (serie !== undefined) {
          series.push(serie);
        }
      });
      return series;
    });
  }

  buildDataSerie(concernedItem) {
    return this.dataService
      .searchDataList(
        this.form.startDate != undefined && this.form.startDate != ""
          ? this.form.startDate
          : undefined,
        this.form.endDate != undefined && this.form.endDate != ""
          ? this.form.endDate
          : undefined,
        undefined,
        [this.selectedExperiment],
        [concernedItem.uri],
        [this.form.variable],
        undefined,
        undefined,
        this.form.provenance ? [this.form.provenance] : undefined,
        undefined, //this.addMetadataFilter(),
        ["date=asc"],
        0,
        50000
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<DataGetDTO>>>) => {
        const data = http.response.result as Array<DataGetDTO>;
        let dataLength = data.length;
        if (dataLength > 0) {
          const cleanData = this.dataTransforme(data, concernedItem);
          if (dataLength > 50000) {
            this.$opensilex.showInfoToast(
              this.$i18n.t(
                "ExperimentDataVisuView.limitSizeMessageA"
              ) +
                " " +
                dataLength +
                " " +
                this.$i18n.t(
                  "ExperimentDataVisuView.limitSizeMessageB"
                ) +  concernedItem.name + this.$i18n.t(
                  "ExperimentDataVisuView.limitSizeMessageC"
                )
            );
          }
          return {
            name: concernedItem.name,
            data: cleanData,
            visible: true
          };
        }
      })
      .catch(error => {});
  }

  // addMetadataFilter() {
  //   let metadata = undefined;
  //   if (
  //     this.form.metadataKey != undefined &&
  //     this.form.metadataKey != "" &&
  //     this.form.metadataValue != undefined &&
  //     this.form.metadataValue != ""
  //   ) {
  //     metadata =
  //       '{"' + this.form.metadataKey + '":"' + this.form.metadataValue + '"}';
  //     return metadata;
  //   }
  // }

  // keep only date/value/uriprovenance properties
  dataTransforme(data, concernedItem) {
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
        objectUri: concernedItem.uri,
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
/*
 * Force Bootstrap v4 transitions
 * (ignores prefers-reduced-motion media feature)
 * https://gist.github.com/robssanches/33c6c1bf4dd5cf3c259009775883d1c0
 */

.fade {
  transition: opacity 0.3s linear !important;
}
.collapsing {
  transition: height 0.8s ease !important;
}

.data-container {
  height: 800px;
}
</style>

<i18n>
en:
    ExperimentDataVisuView:
        datatypeMessageA: The variable datatype is
        datatypeMessageB: At this time only decimal or integer are accepted 
        limitSizeMessageA : "There are "
        limitSizeMessageB : "data for "
        limitSizeMessageC : " .Only the 50 000 first data are displayed."
     
fr:
    ExperimentDataVisuView:
        datatypeMessageA:  le type de donnée de la variable est
        datatypeMessageB: Pour le moment, seuls les types decimal ou entier sont acceptés
        limitSizeMessageA : "Il y a "
        limitSizeMessageB : "données pour "
        limitSizeMessageC : ".Seules les 50 000 premières valeurs sont affichées. "

</i18n>

