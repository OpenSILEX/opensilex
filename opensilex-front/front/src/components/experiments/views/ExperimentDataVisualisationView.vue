<template>
  <div class="experimentDataVisualisationView">
    <opensilex-PageContent class="pagecontent">

      <!-- Form -->
      <opensilex-ExperimentDataVisualisationForm
        ref="experimentDataVisualisationForm"
        :selectedExperiment="selectedExperiment"
        :scientificObjects.sync="selectedScientificObjects"
        :selectedVar.sync="selectedVariable"
        :refreshSoSelector="refreshSoSelector"
        :refreshProvComponent="refreshProvComponent"
        :soFilter="soFilter"
        :showAllEvents.sync="showEvents"
        @search="onSearch"
        @onValidateScientificObjects="onValidateScientificObjects"
      ></opensilex-ExperimentDataVisualisationForm>

      <div class="d-flex justify-content-center mb-3" v-if="!showGraphicComponent && initLoader">
        <b-spinner label="Loading..."></b-spinner>
      </div>

      <!-- Graphic -->
      <opensilex-DataVisualisationGraphic
        v-if="showGraphicComponent"
        ref="visuGraphic"
        class="experimentDataVisualisationGraphic"
        @addEventIsClicked="showEventForm"
        @dataAnnotationIsClicked="showAnnotationForm"
        :showEvents="showEvents"
        :startDate="experimentDataVisualisationForm.startDate"
        :endDate="experimentDataVisualisationForm.endDate"
      ></opensilex-DataVisualisationGraphic>


      <!-- Annotations -->
      <opensilex-AnnotationModalForm 
        ref="annotationModalForm"
        @onCreate="onAnnotationCreated"
      >
      </opensilex-AnnotationModalForm>
      
      <!-- Events -->
      <opensilex-EventModalForm
        ref="eventsModalForm"
        :target="target"
        :eventCreatedTime="eventCreatedTime"
        @onCreate="onEventCreated"
        :context="{experimentURI: selectedExperiment}"
      ></opensilex-EventModalForm>

    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import moment from "moment-timezone";
import Highcharts from "highcharts";
import {
  DataService,
  DataGetDTO,
  EventsService,
  EventGetDTO,
  VariableDetailsDTO,
  VariablesService,
  ScientificObjectsService
  // @ts-ignore
} from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import { Component, Ref, Prop, PropSync } from "vue-property-decorator";
import OpenSilexVuePlugin from '../../../models/OpenSilexVuePlugin'
import Vue from "vue";

@Component
export default class ExperimentDataVisualisationView extends Vue {
  $route: any;
  $opensilex: OpenSilexVuePlugin;
  dataService: DataService;
  eventsService: EventsService;
  VariablesService: VariablesService;
  soService: ScientificObjectsService; 

  eventCreatedTime = "";
  target = [] ;
  form;
  chartOptionsValue: any;

  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;
  @Ref("eventsModalForm") readonly eventsModalForm!: any;
  @Ref("experimentDataVisualisationForm") readonly experimentDataVisualisationForm!: any;

  showSearchComponent: boolean = true;
  showGraphicComponent: boolean = false;
  initLoader : boolean = false;
  selectedExperiment;
  selectedVariable = [];
  selectedVariablesObjectsList = [];
  eventTypesColorArray = [];
  selectedObjects = [];
  selectedScientificObjects= [];
  selectedScientificObjectsWithLabel: Array<{id: string, label: string}> = [];
  showEvents = false;
  multipleVariables = false;
  showDataVisuView = false;
  @Ref("page") readonly page!: any;

  selectedSO = false;
  selectedVar = false;
  selectedVarLimit;
  selectedSOLimit; 


  @Prop()
  refreshSoSelector;

  @Prop()
  refreshProvComponent;

  @Prop()
  soFilter;

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        if (this.showGraphicComponent) {
          this.onUpdate(this.form);
        }
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  onAnnotationCreated() {
    this.visuGraphic.updateDataAnnotations();
  }

  showAnnotationForm(target) {
    this.annotationModalForm.showCreateForm([target]);
  }

    // refresh count at event creation
  onEventCreated() {
    this.onUpdate(this.form);
    this.experimentDataVisualisationForm.getTotalEventsCount();
  }

  showEventForm(value) {
    this.target = value.target;
    this.eventCreatedTime = value;
    this.eventsModalForm.showCreateForm();
  }

  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    this.eventsService = this.$opensilex.getService("opensilex.EventsService");
    this.$opensilex.disableLoader();
    this.selectedExperiment = decodeURIComponent(this.$route.params.uri);
    this.VariablesService = this.$opensilex.getService("opensilex.VariablesService");
  }

    // when event is created or if this.ShowGraphicComponent is true:
  onUpdate(form) {
    this.form = form;
    const datatype = this.selectedVariable[0].datatype.split("#")[1];
    if (datatype == "decimal" || datatype == "integer") {
      this.showGraphicComponent = false;
      this.$opensilex.enableLoader();
      this.loadSeries();
    } else {
      this.showGraphicComponent = false;
      this.$opensilex.showInfoToast(
        this.$i18n.t("ExperimentDataVisualisationView.datatypeMessageA") +
          " " +
          datatype +
          " " +
          this.$i18n.t("ExperimentDataVisualisationView.datatypeMessageB")
      );
    }
  }

  buildColorsSOArray() {
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
      "#caaf34",
      "#4F72D6",
      "#DB78E1",
      "#97CA68",
    ];
    let index = 0;
      // Give a different color from colorPalette array for each SO
    this.selectedScientificObjects.forEach((element, index) => {
      this.eventTypesColorArray[element] = colorPalette[index];
      index++;
    });
  }

  // function called on visualization button
  onSearch(form) {
    this.initLoader = true;
    this.form = form;
    this.showGraphicComponent = false;
    this.$opensilex.enableLoader();
    this.multipleVariables = this.selectedVariable.length > 1;

    if (!this.selectedVariable && !this.selectedScientificObjects) {
      return;
    }

    this.selectedSOLimit = 15;
    this.selectedVarLimit = 2;

    // if choosed SO > 15 / Var > 2 : error message.
    if(this.selectedScientificObjects.length > this.selectedSOLimit || this.selectedVariable.length > this.selectedVarLimit) {
      alert(this.$t('ExperimentDataVisualisationView.selectedSOLimitSize'));
      this.selectedSO=false;
      this.selectedVar=false;
    }
    else {

      this.VariablesService
        .getVariablesByURIs(this.selectedVariable)
        .then((http: HttpResponse<OpenSilexResponse>) => {
          this.selectedVariablesObjectsList = http.response.result;
            this.showGraphicComponent = true;
            this.buildColorsSOArray();
            this.loadSeries();
        }
      )
    }  
  }

  // BuildSeries only if form is completed
  loadSeries() {
    if (this.selectedScientificObjects && this.selectedVariablesObjectsList) {
      this.buildSeries();
    }
  }

  // build all series, from DataSeries & EventsSeries
  // buildSeries | <= buildDataSeries <= buildDataSerie
  //             | <= buildEventsSeries <= buildEventsSerie
  buildSeries() {
    var promises = [];
    var promise;
    const series = [];
    let serie;
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    
    this.$opensilex.disableLoader();
    promise = this.buildEventsSeries();
    promises.push(promise);

      for (let variable of this.selectedVariablesObjectsList) {
        promise = this.buildDataSeries(variable);
        promises.push(promise);
      }

    Promise.all(promises).then(values => {
      let series = [];

      for (let value of values) {
        if(value) {
          for (let serie of value){
            series.push(serie)
          }
        }
      }

      this.showGraphicComponent = true;
      // reload only when all datas are loaded
      this.$nextTick(() => {
         this.visuGraphic.reload(series, this.selectedVariablesObjectsList, this.form, { showEvents:this.showEvents });

      // and scroll to graphic
        let graphic = document.querySelector('.experimentDataVisualisationGraphic');
        graphic.scrollIntoView({
          behavior: 'smooth',
        });
      });
    });
  }
  
  // build DataSeries with elements from each DataSerie
  buildDataSeries(selectedVariable: VariableDetailsDTO) {
    let series = [],
    serie;
    let promises = [],
    promise;

    this.selectedScientificObjectsWithLabel.forEach((element, index) => {
      promise = this.buildDataSerie(element, selectedVariable, index);
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

  // Build each singular Dataserie on dataService.searchDataList format
  buildDataSerie(concernedItem: {id: string, label: string}, selectedVariable: VariableDetailsDTO, index) {
    return this.dataService
      .searchDataList(
        this.form.startDate,
        this.form.endDate,
        undefined,
        [this.selectedExperiment],         // experiment
        [concernedItem.id],                   // targets / os
        [selectedVariable.uri],            // variables
        undefined,
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
              this.$i18n.t("ExperimentDataVisualisationView.limitSizeMessageA") +
                " " +
                dataLength +
                " " +
                this.$i18n.t("ExperimentDataVisualisationView.limitSizeMessageB") +
                concernedItem.label +
                this.$i18n.t("ExperimentDataVisualisationView.limitSizeMessageC")
            );
          }
            if (this.multipleVariables) {
              return {
                  name: concernedItem.label + " / " + selectedVariable.name,
                  data: cleanData,
                  visible: true,
                  color:this.eventTypesColorArray[concernedItem.id],
                  legendColor: this.eventTypesColorArray[concernedItem.id],
              }
            }
          else {
            return {
                name: concernedItem.label,
                data: cleanData,  
                visible: true,
                color:this.eventTypesColorArray[concernedItem.id],
                legendColor: this.eventTypesColorArray[concernedItem.id]
            };
          }
        }
      })
      .catch(this.$opensilex.errorHandler);
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
        objectUri: concernedItem.id,
        provenanceUri: element.provenance.uri,
        data: element
      };
      cleanData.push(toAdd);
    });
    return cleanData;
  }

  // Build EventsSeries for each OS with elements from each EventsSerie
  buildEventsSeries() {
    if (this.form && this.showEvents) {
      let series = [],
        serie;
      let promises = [],
        promise;

      this.selectedScientificObjectsWithLabel.forEach((element, index) => {
        promise = this.buildEventsSerie(element);
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
    } else {
      return null;
    }
  }

  // Build each singular Eventsserie on eventsService.searchEvents format
  buildEventsSerie(concernedItem: {id: string, label: string}) {
    return this.eventsService
      .searchEvents(
        undefined,
        this.form.startDate != undefined && this.form.startDate != ""
          ? this.form.startDate
          : undefined,
        this.form.endDate != undefined && this.form.endDate != ""
          ? this.form.endDate
          : undefined,
        concernedItem.id,
        undefined,
        undefined,
        0,
        0
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<EventGetDTO>>>) => {
        const events = http.response.result as Array<EventGetDTO>;
        // If they have events
        if (events.length > 0) {
          const cleanEventsData = [];
          let convertedDate, toAdd, label, title;

          // for each of them 
          events.forEach(element => {
            label = element.rdf_type_name
              ? element.rdf_type_name
              : element.rdf_type;
            // if start date -> search end dat and put in on label
            if (element.start != null) {
              let endTime = element.end ? element.end : "en cours..";
              label = label + "(End: " + endTime + ")";
            }
            title = label.charAt(0).toUpperCase();
            let stringDateWithoutUTC;
            // if start date -> stringdate = start date
            if (element.start != null) {
              stringDateWithoutUTC =
                moment.parseZone(element.start).format("YYYYMMDD HHmmss") +
                "+00:00";
            // else stringdate = end date
            } else {
              stringDateWithoutUTC =
                moment.parseZone(element.end).format("YYYYMMDD HHmmss") +
                "+00:00";
            }

            let dateWithoutUTC = moment(stringDateWithoutUTC).valueOf();
            toAdd = {
              x: dateWithoutUTC,
              title: title,
              text: label,
              eventUri: element.uri,
            };

            cleanEventsData.push(toAdd);
          });

          let name = concernedItem.label;
          return {
            type: "flags",
            allowOverlapX: false,
            name: (this.$t('ExperimentDataVisualisationView.events')) + " -> " + name,
            lineWidth: 1,
            yAxis: 2,
            data: cleanEventsData,
            style: {
              color: "white"
            },
            fillColor:this.eventTypesColorArray[concernedItem.id],
            legendColor: this.eventTypesColorArray[concernedItem.id]          
          };
        } else {
          return undefined;
        }
      });
  }

  timestampToUTC(time) {
    var day = Highcharts.dateFormat("%Y-%m-%dT%H:%M:%S+0000", time);
    return day;
  }  
  
  onValidateScientificObjects(scientificObjects) {
    if (scientificObjects === undefined) {
      return;
    }
    this.selectedScientificObjectsWithLabel = scientificObjects;
  }
}
</script>

<style scoped lang="scss">

.fade {
  transition: opacity 0.3s linear !important;
}
.collapsing {
  transition: height 0.8s ease !important;
}

.experimentDataVisualisationGraphic {
  min-width: 100%;
  max-width: 100vw;
}
</style>

<i18n>
en:
    ExperimentDataVisualisationView:
        datatypeMessageA: The variable datatype is
        datatypeMessageB: At this time only decimal or integer are accepted 
        limitSizeMessageA : "There are "
        limitSizeMessageB : "data for "
        limitSizeMessageC : " .Only the 50 000 first data are displayed."
        selectedSOLimitSize: "The selection has too many lines for this feature, refine your search, maximum= 15 scientific objects and 2 variables."
        events: "Events"
     
fr:
    ExperimentDataVisualisationView:
        datatypeMessageA:  le type de donnée de la variable est
        datatypeMessageB: Pour le moment, seuls les types decimal ou entier sont acceptés
        limitSizeMessageA : "Il y a "
        limitSizeMessageB : "données pour "
        limitSizeMessageC : ".Seules les 50 000 premières valeurs sont affichées. "
        selectedSOLimitSize: "La selection comporte trop de lignes pour cette fonctionnalité, affinez votre recherche, maximum= 15 objets scientifiques et 2 variables."
        events: "Evenements"

</i18n>



