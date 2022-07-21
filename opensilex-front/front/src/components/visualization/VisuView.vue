<template>
  <div  @click="clickEvent">
 
    <b-collapse v-model="showSearchComponent" class="mt-2">
      <opensilex-VisuForm :selectedExperiment="selectedExperiment" @search="onSearch" ></opensilex-VisuForm>
    </b-collapse>

    <b-collapse v-model="showGraphicComponent" class="mt-2">

      <opensilex-VisuImages 
      ref="visuImages"
       v-if="showImages"
      @imageIsHovered="onImageIsHovered"
      @imageIsUnHovered=" onImageIsUnHovered"
         ></opensilex-VisuImages>

      <opensilex-VisuGraphic
        v-if="showGraphicComponent"
        ref="visuGraphic"
        :showEvents="showEvents"
        @search="showSearchComponent=!showSearchComponent;showGraphicComponent=!showGraphicComponent;"
      ></opensilex-VisuGraphic>
      
    </b-collapse>

   <!--  <opensilex-ProvenanceView></opensilex-ProvenanceView>

    <opensilex-ModalForm
      v-if="showEventFormComponent"
      ref="addEventForm"
      component="opensilex-AddEventForm"
      editTitle="VisuView.eventUpdate"
      icon="ik#ik-flag"
      @onUpdate="onEventCreate()"
    ></opensilex-ModalForm>

    <opensilex-ModalForm
      ref="addAnnotationForm"
      component="opensilex-AddAnnotationForm"
      editTitle="VisuView.annotationUpdate"
      icon="ik#ik-flag"
    ></opensilex-ModalForm> -->

    
  </div>
</template>

<script lang="ts">
import moment from "moment-timezone";
import Highcharts from "highcharts";
import { Image } from "./image";
import {
  DataService,
  DataFileGetDTO,
  OntologyService,
  ResourceTreeDTO,
  DataGetDTO,
  EventsService,
  EventGetDTO
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class VisuView extends Vue {
  $route: any;
  $opensilex: any;
  dataService: DataService;
   eventsService: EventsService; 
  ontologyService: OntologyService;
  form;
  multipleVariables = false;
  chartOptionsValue: any;
/* 
  @Ref("addEventForm") readonly addEventForm!: any;
  @Ref("addAnnotationForm") readonly addAnnotationForm!: any; */
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("visuImages") readonly visuImages!: any;
  thumbnails = [];
  showEvents = true;
  showImages = true;
  showSearchComponent: boolean = true;
  showGraphicComponent: boolean = false;
  /* showEventFormComponent: boolean = false; */

  selectedExperiment;

  @Prop()
  selectedScientificObjects;

  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
   /*  this.eventsService = this.$opensilex.getService("opensilex.EventsService");
    */
    this.$opensilex.disableLoader();
   /*  this.showEventFormComponent = true; */
    this.selectedExperiment = decodeURIComponent(this.$route.params.uri);
  }

  clickEvent() {
    if(this.visuGraphic){
        this.visuGraphic.closeContextMenu();
    }
  }

  onImageIsHovered(indexes){
     if(this.visuGraphic){
        this.visuGraphic.onImageIsHovered(indexes);
    }
  }

  onImageIsUnHovered(indexes){
     if(this.visuGraphic){
        this.visuGraphic.onImageIsUnHovered(indexes);
    }
  }

  onEventCreate() {
    this.loadSeries();
  }
  
  onSearch(form) {
    this.form = form;
    this.multipleVariables = this.form.variable.length > 1 ? true : false;
    this.showEvents = form.showEvents;
    this.showImages = form.showImages;
    this.showSearchComponent = !this.showSearchComponent;
    this.showGraphicComponent = !this.showGraphicComponent;
    this.$opensilex.enableLoader(); //disable on the AddEventForm component to load the events type
    this.loadSeries();
  }

  loadSeries() {
    if (this.form.concernedItems && this.form.variable) {
      this.buildSeries();
    }
  }

  buildSeries() {
    var promises = [];
    var promise;
    const series = [];
    let serie;
    this.dataService = this.$opensilex.getService("opensilex.DataService");
   this.eventsService = this.$opensilex.getService("opensilex.EventsService");
 
  /*   if (this.form.showEvents) {
      promise = this.buildEventSeries();
      promises.push(promise);
    } */
    if (this.multipleVariables) {
      promise = this.buildSeriesMultipleVariables();
    } else {
      if (this.form.showImages) {
        promise = this.buildDataAndImagesSeries(this.form.variable[0]);
      } else {
        promise = this.buidDataSeries(this.form.variable[0], false);
      }
    }
    promises.push(promise);

    Promise.all(promises).then(values => {
      let series = [];
      let dataSeries;
      let eventTypes = null;
      if (this.form.showEvents) {
        values[0].forEach(serie => {
          series.push(serie);
        });
        dataSeries = values[1];
      } else {
        dataSeries = values[0];
      }
      dataSeries.forEach(serie => {
        series.push(serie);
      });
      if (series.length > 0) {
        this.visuGraphic.reload(
          series,
          this.form.variable,
          this.multipleVariables
        );
      }
    });
  }


  buildSeriesMultipleVariables() {
    let series = [],
      serie;
    let promises = [],
      promise;

    if (this.form.showImages) {
      promise = this.buildDataAndImagesSeries(this.form.variable[0]);
      promises.push(promise);
      promise = this.buidDataSeries(this.form.variable[1], true); //only load images on the first variable
      promises.push(promise);
    } else {
      promise = this.buidDataSeries(this.form.variable[0], false);
      promises.push(promise);
      promise = this.buidDataSeries(this.form.variable[1], true);
      promises.push(promise);
    }

    return Promise.all(promises).then(values => {
      let toReturn = [];
      values[0].forEach(serie => {
        toReturn.push(serie);
      });
      values[1].forEach(serie => {
        toReturn.push(serie);
      });
      return toReturn;
    });
  }

  buildDataAndImagesSeries(variable) {
    let series = [],
      serie;
    let promises = [],
      promise;
    this.form.concernedItems.forEach((element, index) => {
      promise = this.buildDataAndImagesSerie(element, index, variable);
      promises.push(promise);
    });
    return Promise.all(promises).then(value => {
      value.forEach(value => {
        value.forEach(serie => {
          series.push(serie);
        });
      });
      return series;
    });
  }

  buildDataAndImagesSerie(concernedItem, index, variable) {
    let series = [],
      serie;
    let promises = [],
      promise;
    promise = this.buildDataSerie(concernedItem, variable, false, true);
    promises.push(promise);
    promise = this.buildImageSerie(concernedItem);
    promises.push(promise);
    return Promise.all(promises).then(values => {
      if (values[0] !== undefined) {
        const dataSerie = values[0];
        if (this.multipleVariables) {
          series.push({
            name: variable,
            data: dataSerie,
            id: "A" + index,
            visible: true
          });
        } else {
          series.push({
            name: concernedItem,
            data: dataSerie,
            id: "A" + index,
            visible: true
          });
        }

        if (values[1] !== undefined) {
          const cleanImageData = values[1];
          serie = {
            type: "flags",
            name: "images/" + concernedItem,
            data: cleanImageData,
            onSeries: "A" + index,
            width: 8,
            height: 8,
            shape: "circlepin",
            lineWidth: 1,
            point: {
              events: {
                stickyTracking: false,
                mouseOver: e => {
                  const toSend = {
                    imageIndex: e.target.index,
                    serieIndex: e.target.series.index
                  };
                  if( this.visuImages){
                     this.visuImages.onImagePointMouseEnter(toSend);
                  }
                  e.preventDefault();
                  return false;
                },
                mouseOut: e => {
                   if( this.visuImages){
                     this.visuImages.onImagePointMouseOut();
                  }
                  e.preventDefault();
                  return false;
                },
                click: event => {
                  imagePointClick(event);
                  event.preventDefault();
                  return false;
                }
              }
            }
          };

          const imagePointClick = event => {

            const toReturn = {
              date: event.point.date,
              serieIndex: event.point.series.index,
              imageIndex: event.point.index,
              concernedItem: event.point.uri
            };
            this.onImagePointClick(toReturn);
          };
          series.push(serie);
        }
      }

      return series;
    });
  }

  buidDataSeries(variable, isSecondVariable) {
    let series = [],
      serie;
    let promises = [],
      promise;
    this.form.concernedItems.forEach((element, index) => {
      promise = this.buildDataSerie(element, variable, isSecondVariable, false);
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

  buildDataSerie(concernedItem, variable, isSecondVariable, withImages) {
  
    return this.dataService.searchDataList(
        this.form.startDate,
        this.form.endDate,
        undefined,
        undefined,
        [concernedItem],
        [variable.id],
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        0,
        1000000
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<DataGetDTO>>>) => {
        const data = http.response.result as Array<DataGetDTO>;
        if (data.length > 0) {
          const cleanData = this.dataTransforme(data, concernedItem);
          if (withImages) {
            return cleanData;
          } else {
            if (isSecondVariable) {
              return {
                name: concernedItem + "/" + variable.name, 
                data: cleanData, 
                visible: true,
                yAxis: 1
              };
            } else {
              if (this.multipleVariables) {
                return {
                  name: concernedItem + "/" + variable.name,
                  data: cleanData,
                  visible: true
                };
              } else {
                return {
                  name: concernedItem,
                  data: cleanData,
                  visible: true
                };
              }
            }
          }
        }
      })
      .catch(error => {
      });
  }

  // keep only date/value/uriprovenance properties
  dataTransforme(data, concernedItem) {
    let toAdd,
      cleanData = [];
    const orderedData = data.sort(
      (a, b) => Date.parse(a.date) - Date.parse(b.date) //sort ascending
    ); //has to be done on the data service
    orderedData.forEach(element => {
      let stringDateWithoutUTC =
        moment.parseZone(element.date).format("YYYYMMDD HHmmss") + "+00:00";
      let dateWithoutUTC = moment(stringDateWithoutUTC).valueOf();
      let highchartsDate = Highcharts.dateFormat(
        "%Y-%m-%dT%H:%M:%S",
        dateWithoutUTC
      );
      let offset = moment.parseZone(element.date).format("Z");
      toAdd = { // one highchart point attributs
        provenanceUri: element.provenance.uri,
        x: dateWithoutUTC,
        y: element.value,     
        dataUri: element.uri,
        objectUri: concernedItem,
        offset: offset,
        dateWithOffset: highchartsDate + offset
      };
      cleanData.push(toAdd);
    });
    return cleanData;
  }

  buildImageSerie(concernedItem) {
    return this.dataService
      .getDataFileDescriptionsBySearch(
        "http://www.opensilex.org/vocabulary/oeso#Image",
        undefined,
        undefined,
        undefined,
        undefined,
        concernedItem,
        undefined,
        undefined,
        undefined,
        undefined,
        0,
        5000
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<DataFileGetDTO>>>) => {
        const result = http.response.result as Array<DataFileGetDTO>;
        const cleanImageData = this.cleanImageData(result, concernedItem);
        return cleanImageData;
      })
      .catch(error => {
      });
  }

  cleanImageData(data, concernedItem) {
    const cleanImageData = [];
    this.distinctDates(data).forEach(element => {
        let stringDateWithoutUTC =
        moment.parseZone(element).format("YYYYMMDD HHmmss") + "+00:00";
      let dateWithoutUTC = moment(stringDateWithoutUTC).valueOf();
      cleanImageData.push({
        x: dateWithoutUTC,
        title: "I",
        uri: concernedItem,
        date: element,
        utc: moment.parseZone(element).format("Z")
      });
    });
    return cleanImageData;
  }

  // extract  array with  distinct dates ( some images can have same date)
  distinctDates(imageData) {
    var flags = [],
      distinctData = [],
      l = imageData.length,
      i;
    for (i = 0; i < l; i++) {
      if (flags[imageData[i].date]) continue;
      flags[imageData[i].date] = true;
      distinctData.push(imageData[i].date);
    }
    return distinctData;
  }

  onImagePointClick(point) {
    const time = point.date;/* 
    const utcTimeStart = this.timestampToUTC(time);
    const utcTimeEnd = this.timestampToUTC(time+1000); */
    const endTime= moment(time).add(1, 's').format();
    return this.dataService
      .getDataFileDescriptionsBySearch(
        "http://www.opensilex.org/vocabulary/oeso#Image",
        time,
        undefined,
        null,
        undefined,
        point.concernedItem,
        undefined,
        undefined,
        undefined,
        undefined,
        0,
        100
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<DataFileGetDTO>>>) => {
        const result = http.response.result as any;
        if (result && result.length > 0) {

          const data = result as Array<DataFileGetDTO>;
          this.imagesFilter(data, point);
        }
      })
      .catch(error => {
        console.log(error);
      });
  }
  imagesFilter(data: Array<DataFileGetDTO>, point: any) {
    data.forEach(element => {
      const image: Image = {
        imageUri: element.uri,
        uri:
          this.$opensilex.getBaseAPI() +
          "/data/file/thumbnail" +
          encodeURIComponent(element.uri) +
          "?scaledHeight=800",
        type: element.rdf_type,
        objectUri: element.target,
        date: element.date,
        provenanceUri: element.provenance.uri,
        imageIndex: point.imageIndex,
        serieIndex: point.serieIndex
      };
      this.visuImages.addImage(image);
    });
  }

  buildEventSeries() {
    let series = [],
      serie;
    let promises = [],
      promise;
    this.form.concernedItems.forEach((element, index) => {
      promise = this.buildEventSerie(element, index);
      promises.push(promise);
    });
    return Promise.all(promises).then(values => {
      values.forEach(serie => {
        if (serie) {
          series.push(serie);
        }
      });
      return series;
    });
  }

  buildEventSerie(concernedItem, index) {
    return this.eventsService
    .searchEvents(undefined, undefined, undefined, concernedItem, undefined, undefined,0, 100000)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        const eventsData = http.response.result.data as Array<EventGetDTO>;
        if (eventsData.length > 0) {
          const cleanEventsData = [];
          let convertedDate, toAdd, label;

          let eventTypesColorArray = [];
          const colorPalette = [
            "#ca6434 ",
            "#427775",
            "#f2dc7c",
            "#0f839c",
            "#a45354",
            "#d3b0ae",
            "#774e42",
            "#776942",
            "#5c4277",
            "#34a0ca",
            "#9334ca",
            "#caaf34"
          ];
          let index = 0;
          eventsData.forEach(element => {
            if (!eventTypesColorArray[element.rdf_type_name]) {
              eventTypesColorArray[element.rdf_type_name] = colorPalette[index];
              index++;
              if (index === 12) {
                index = 0;
              }
            }

            convertedDate = moment.utc(element.start).valueOf();
            label = element.rdf_type_name;
            toAdd = {
              x: convertedDate,
              title: label,
              text: label,
              eventUri: element.uri,
              fillColor: eventTypesColorArray[element.rdf_type_name]
            };
            cleanEventsData.push(toAdd);
          });
          let yAxis;
          if (this.multipleVariables) {
            yAxis = 2;
          } else {
            yAxis = 1;
          }
          if (this.form.concernedItems.length > 1) {
            return {
              type: "flags",
              allowOverlapX: true,
              name: "Events-" + concernedItem,
              lineWidth: 1,
              yAxis: yAxis,
              y: -20,
              data: cleanEventsData,
              style: {
                // text style
                color: "white"
              }
            };
          } else {
            return {
              type: "flags",
              allowOverlapX: true,
              name: "Events",
              lineWidth: 1,
              yAxis: yAxis,
              y: -20,
              data: cleanEventsData,
              style: {
                // text style
                color: "white"
              }
            };
          }
        } else {
          return undefined;
        }
      })
      .catch(error => {
        console.log(error);
      });
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
  VisuView:
    eventUpdate: Add an event
    annotationUpdate: Add annotation

fr:
  VisuView:
    eventUpdate: Ajout d'un évenement
    annotationUpdate: Ajout d'une annotation
</i18n>
