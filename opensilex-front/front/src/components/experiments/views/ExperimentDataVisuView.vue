<template>
  <div class="experimentDataVisuView">
    <opensilex-PageContent class="pagecontent">

      <!-- Toggle Sidebar-->
      <div class="searchMenuContainer"
        v-on:click="searchFiltersToggle = !searchFiltersToggle"
        :title="searchFiltersPannel()"
      >
        <div class="searchMenuIcon">
          <i class="icon ik ik-search"></i>
        </div>
      </div>

      <Transition>
        <div v-show="searchFiltersToggle">
          <!--Form-->
          <opensilex-ExperimentDataVisuForm
            ref="experimentDataVisuForm"
            :selectedExperiment="selectedExperiment"
            :scientificObjects="scientificObjectsURI"
            @search="onSearch"
            @update="onUpdate"
          ></opensilex-ExperimentDataVisuForm>
        </div>
      </Transition>

    <div class="d-flex justify-content-center mb-3" v-if="!showGraphicComponent && initLoader">
      <b-spinner label="Loading..."></b-spinner>
    </div>

    <!-- Graphic -->
    <opensilex-DataVisuGraphic
      v-if="showGraphicComponent"
      ref="visuGraphic"
      @addEventIsClicked="showEventForm"
      @dataAnnotationIsClicked="showAnnotationForm"
      @graphicCreated="$emit('graphicCreated')"
      class="experimentDataVisuGraphic"
      v-bind:class ="{
        'experimentDataVisuGraphic': searchFiltersToggle,
        'experimentDataVisuGraphicWithoutForm': !searchFiltersToggle
      }"
    ></opensilex-DataVisuGraphic>

    <opensilex-AnnotationModalForm 
    ref="annotationModalForm"
     @onCreate="onAnnotationCreated"
     >
     </opensilex-AnnotationModalForm>
    
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
import {DataGetDTO, DataService, EventGetDTO, EventsService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import HighchartsDataTransformer from "../../../models/HighchartsDataTransformer";

@Component
export default class ExperimentDataVisuView extends Vue {
  $route: any;
  $opensilex: any;
  dataService: DataService;

  target = [] ;
  eventCreatedTime = "";

  form;
  chartOptionsValue: any;

  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;
  @Ref("eventsModalForm") readonly eventsModalForm!: any;
  @Ref("experimentDataVisuForm") readonly experimentDataVisuForm!: any;

  showSearchComponent: boolean = true;
  showGraphicComponent: boolean = false;
  initLoader : boolean = false;
  selectedExperiment;
  selectedVariable;
  eventsService: EventsService;
  eventTypesColorArray = [];
  searchFiltersToggle: boolean = true;

  @Prop()
  selectedScientificObjects;

  get scientificObjectsURI() {
    return this.selectedScientificObjects.map(so => {
      return so.uri;
    });
  }

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

    // simulate window resizing to resize the graphic when the filter panel display changes
  @Watch("searchFiltersToggle")
  onSearchFilterToggleChange(){
    this.$nextTick(()=> { 
      window.dispatchEvent(new Event('resize'));
    })  
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

  onEventCreated() {
    this.onUpdate(this.form);
    this.experimentDataVisuForm.getTotalEventsCount();
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
      "#caaf34"
    ];
    let index = 0;
    this.selectedScientificObjects.forEach((element, index) => {
      this.eventTypesColorArray[element.uri] = colorPalette[index];
      index++;
      if (index === 12) {
        index = 0;
      }
    });
  }

  onSearch(form) {
    this.searchFiltersToggle = !this.searchFiltersToggle
    this.initLoader = true;
    this.form = form;
    this.showGraphicComponent = false;
    this.$opensilex.enableLoader();
    this.$opensilex
      .getService("opensilex.VariablesService")
      .getVariable(form.variable[0])
      .then((http: HttpResponse<OpenSilexResponse>) => {
        this.selectedVariable = http.response.result;
        const datatype = this.selectedVariable.datatype.split("#")[1];
        if (datatype == "decimal" || datatype == "integer") {

          this.buildColorsSOArray();
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
    
    this.$opensilex.disableLoader();
    promise = this.buildEventsSeries();
    promises.push(promise);
    promise = this.buidDataSeries();
    promises.push(promise);

    Promise.all(promises).then(values => {
      let series = [];

      if (values[0]) {
        values[0].forEach(serie => {
          series.push(serie);
        });
      }

      if (values[1]) {
        values[1].forEach(serie => {
          series.push(serie);
        });
      }

      this.showGraphicComponent = true;
      this.$nextTick(() => {
        this.visuGraphic.reload(series, this.selectedVariable, this.form);
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

  buildEventsSeries() {
    if (this.form && this.form.showEvents) {
      let series = [],
        serie;
      let promises = [],
        promise;
      this.selectedScientificObjects.forEach((os, index) => {
        promise = this.buildEventsSerie(os);
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

  buildEventsSerie(concernedItem) {
    return this.eventsService
      .searchEvents(
        undefined,
        this.form.startDate != undefined && this.form.startDate != ""
          ? this.form.startDate
          : undefined,
        this.form.endDate != undefined && this.form.endDate != ""
          ? this.form.endDate
          : undefined,
        concernedItem.uri,
        undefined,
        undefined,
        0,
        0
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<EventGetDTO>>>) => {
        const events = http.response.result as Array<EventGetDTO>;
        if (events.length > 0) {
          const cleanEventsData = [];
          let convertedDate, toAdd, label, title;


          events.forEach(element => {
            label = element.rdf_type_name
              ? element.rdf_type_name
              : element.rdf_type;
            if (element.start != null) {
              let endTime = element.end ? element.end : "en cours..";
              label = label + "(End: " + endTime + ")";
            }
            // if (element.end) {
            //   if (element.is_instant) {
            //     title = label;
            //   } else {
            //     title = label + "(End)";
            //   }
            // }
            title = label.charAt(0).toUpperCase();
            let timestamp;
            if (element.start != null) {
              timestamp = new Date(element.start).getTime();
            } else {
              timestamp = new Date(element.end).getTime();
            }

            toAdd = {
              x: timestamp,
              title: title,
              text: label,
              eventUri: element.uri,
            };

            cleanEventsData.push(toAdd);
          });
          let name = concernedItem.name ? concernedItem.name : concernedItem.uri
          return {
            type: "flags",
            allowOverlapX: false,
            name: "Events ->"+ name,
            lineWidth: 1,
            yAxis: 1,
            data: cleanEventsData,
            style: {
              // text style
              color: "white"
            },
            fillColor:this.eventTypesColorArray[concernedItem.uri],
            legendColor: this.eventTypesColorArray[concernedItem.uri]
            
          };
        } else {
          return undefined;
        }
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

        if (dataLength === 0){
          this.$opensilex.showInfoToast(
          this.$t("component.common.search.noDataFound").toString());
        }

        if (dataLength >= 0) {
          const cleanData = HighchartsDataTransformer.transformDataForHighcharts(data, {scientificObjectUri: concernedItem.uri});
          if (dataLength > 50000) {
            this.$opensilex.showInfoToast(
              this.$i18n.t("ExperimentDataVisuView.limitSizeMessageA") +
                " " +
                dataLength +
                " " +
                this.$i18n.t("ExperimentDataVisuView.limitSizeMessageB") +
                concernedItem.name +
                this.$i18n.t("ExperimentDataVisuView.limitSizeMessageC")
            );
          }

          let name = concernedItem.name ? concernedItem.name : concernedItem.uri
          return {
            name: name,
            data: cleanData,
            visible: true,
            color:this.eventTypesColorArray[concernedItem.uri],
            legendColor: this.eventTypesColorArray[concernedItem.uri]
          };
        }
      })
      .catch(error => {});
  }

  searchFiltersPannel() {
    return this.$t("searchfilter.label")
  }
}
</script>

<style scoped lang="scss">
/*
 * Force Bootstrap v4 transitions
 * (ignores prefers-reduced-motion media feature)
 *  https://gist.github.com/robssanches/33c6c1bf4dd5cf3c259009775883d1c0
 */

.fade {
  transition: opacity 0.3s linear !important;
}
.collapsing {
  transition: height 0.8s ease !important;
}

.experimentDataVisuView {
  min-height: 700px;
  margin-top: 15px
}

.experimentDataVisuGraphic {
  min-width: calc(100% - 450px);
  max-width: calc(100vw - 380px);
}

.experimentDataVisuGraphicWithoutForm{
  min-width: 100%;
  max-width: 100vw;
}

.visualizeBtn {
 float:right;
 border:none;
 background: #00A28C;
}
.visualizeBtn:disabled,
.visualizeBtn[disabled]{
  background-color: #666666;
  color: #F1F1F1;
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

