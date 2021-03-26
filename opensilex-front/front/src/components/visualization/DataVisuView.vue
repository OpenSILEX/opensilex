<template>
  <div>
    <b-collapse v-model="showSearchComponent" class="mt-2">
      <opensilex-DataVisuForm
        :selectedExperiment="selectedExperiment"
        :scientificObjects="scientificObjectsURI"
        @search="onSearch"
      ></opensilex-DataVisuForm>
    </b-collapse>

    <b-collapse v-model="showGraphicComponent" class="mt-2">
      <opensilex-DataVisuGraphic
        v-if="showGraphicComponent"
        ref="visuGraphic"
        @detailProvenanceIsClicked="showProvenanceDetailComponent"
        @graphicCreated="$emit('graphicCreated')"
      ></opensilex-DataVisuGraphic>
    </b-collapse>
    <opensilex-DataProvenanceModalView ref="dataProvenanceModalView"></opensilex-DataProvenanceModalView>
  </div>
</template>

<script lang="ts">
import moment from "moment-timezone";
import Highcharts from "highcharts";
import {
  DataService,
  DataGetDTO,
  ProvenanceGetDTO
} from "opensilex-core/index";

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
  @Ref("dataProvenanceModalView") readonly dataProvenanceModalView!: any;
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

    this.$opensilex.enableLoader();
    if (value.provenance != undefined && value.provenance != null) {
      this.getProvenance(value.provenance).then(provenance => {
     
        value.provenance = provenance;
        this.dataProvenanceModalView.setProvenance(value);
        this.dataProvenanceModalView.show();
      });
    }
  }
  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    this.$opensilex.disableLoader();
    this.selectedExperiment = decodeURIComponent(this.$route.params.uri);
  }

  onSearch(form) {
    this.form = form;
    this.showGraphicComponent = true;

    if (form.variable) {
        this.$opensilex
          .getService("opensilex.VariablesService")
          .getVariable(form.variable)
          .then((http: HttpResponse<OpenSilexResponse>) => {
            this.selectedVariable = http.response.result;
            this.loadSeries();
          });
    }
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

      this.visuGraphic.reload(series, this.selectedVariable);
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
        this.form.startDate != undefined && this.form.startDate != ""?this.form.startDate: undefined,
         this.form.endDate != undefined && this.form.endDate != ""?this.form.endDate: undefined,
        undefined,
        [this.selectedExperiment],
        [concernedItem.uri],
        [this.form.variable],
        undefined,
        undefined,
        this.form.provenance ? [this.form.provenance] : undefined,
        this.addMetadataFilter(),
        undefined,
        0,
        1000000
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<DataGetDTO>>>) => {
        const data = http.response.result as Array<DataGetDTO>;
        if (data.length > 0) {
          const cleanData = this.dataTransforme(data, concernedItem);

          return {
            name: concernedItem.name,
            data: cleanData,
            visible: true
          };
        }
      })
      .catch(error => {
        console.log(error);
      });
  }

  addMetadataFilter() {
    let metadata = undefined;
    if (this.form.metadataKey != undefined && this.form.metadataKey != ""
    && this.form.metadataValue != undefined && this.form.metadataValue != "") {
      metadata = '{"' + this.form.metadataKey + '":"' + this.form.metadataValue + '"}'
      return metadata;
    }
  }

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

