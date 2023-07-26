<template>
  <div class="experimentDataVisualisationView">
    <opensilex-VisuImages
        ref="visuImages"
        v-if="showImages"
        @imageIsHovered="onImageIsHovered"
        @imageIsUnHovered=" onImageIsUnHovered"
        @imageIsDeleted=" onImageIsDeleted"
        @onImageAnnotate=" showAnnotationForm"
        @onImageDetails=" onImageDetails"
        @onAnnotationDetails=" onAnnotationDetails"
        bind:class="{
          'ScientificObjectVisualizationImage': showImages,
          'ScientificObjectVisualizationWithoutImages': !showImages
        }"
    ></opensilex-VisuImages>
    <opensilex-PageContent class="pagecontent">

      <!-- Toggle Sidebar-->
      <div class="searchMenuContainer"
           v-on:click="searchFiltersToggle = !searchFiltersToggle"
           :title="searchFiltersPannel()"
      >
        <div class="searchMenuIcon">
          <i class="ik ik-search"></i>
        </div>
      </div>

      <!-- Form -->
      <Transition>
        <div v-show="searchFiltersToggle">
          <opensilex-ExperimentDataVisualisationForm
              ref="experimentDataVisualisationForm"
              :selectedExperiment="selectedExperiment"
              :scientificObjects.sync="selectedScientificObjects"
              :selectedVar.sync="selectedVariable"
              :refreshSoSelector="refreshSoSelector"
              :refreshProvComponent="refreshProvComponent"
              :soFilter="soFilter"
              @search="onSearch"
              @onValidateScientificObjects="onValidateScientificObjects"
          ></opensilex-ExperimentDataVisualisationForm>
        </div>
      </Transition>

      <div class="d-flex justify-content-center mb-3" v-if="!showGraphicComponent && initLoader">
        <b-spinner label="Loading..."></b-spinner>
      </div>

      <!-- Graphic -->
      <opensilex-DataVisuGraphic
          v-if="showGraphicComponent"
          ref="visuGraphic"
          v-bind:class="{
          'experimentDataVisualisationGraphic': searchFiltersToggle,
          'experimentDataVisualisationGraphicWithoutForm': !searchFiltersToggle
        }"
          @addEventIsClicked="showEventForm"
          @dataAnnotationIsClicked="showAnnotationForm"
          :startDate="experimentDataVisualisationForm.startDate"
          :endDate="experimentDataVisualisationForm.endDate"
          :graphicTitle="selectedExperiment"
          :elementName="elementName"
      ></opensilex-DataVisuGraphic>


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
import Highcharts from "highcharts";
import {
  DataGetDTO,
  DataService,
  EventGetDTO,
  EventsService,
  ScientificObjectsService,
  VariableDetailsDTO,
  VariablesService
} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import OpenSilexVuePlugin from '../../../models/OpenSilexVuePlugin'
import Vue from "vue";
import HighchartsDataTransformer, {
  HighchartsDataTransformerOptions,
  OpenSilexPointOptionsObject
} from "../../../models/HighchartsDataTransformer";
import {ProvEntityModel} from "opensilex-core/model/provEntityModel";
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {DataFileGetDTO} from "opensilex-core/model/dataFileGetDTO";
import {Image} from "../../visualization/image";
import {AnnotationGetDTO} from "opensilex-core/model/annotationGetDTO";

interface ImagePointOptionsObject extends OpenSilexPointOptionsObject {
  prov_used: Array<ProvEntityModel>,
  imageURI: string
}

@Component
export default class ExperimentDataVisualisationView extends Vue {
  $route: any;
  $opensilex: OpenSilexVuePlugin;
  dataService: DataService;
  annotationService: AnnotationsService;
  eventsService: EventsService;
  VariablesService: VariablesService;
  soService: ScientificObjectsService;

  eventCreatedTime = "";
  target = [];
  form;
  chartOptionsValue: any;
  @Ref("visuImages") readonly visuImages!: any;
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;
  @Ref("eventsModalForm") readonly eventsModalForm!: any;
  @Ref("experimentDataVisualisationForm") readonly experimentDataVisualisationForm!: any;
  @Ref("page") readonly page!: any;

  showSearchComponent: boolean = true;
  showGraphicComponent: boolean = false;
  initLoader: boolean = false;
  selectedExperiment;
  selectedVariable = [];
  selectedVariablesObjectsList = [];
  eventTypesColorArray = [];
  selectedObjects = [];
  selectedScientificObjects = [];
  selectedScientificObjectsWithLabel: Array<{ id: string, label: string }> = [];
  multipleVariables = false;
  showDataVisuView = false;

  selectedSO = false;
  selectedVar = false;
  selectedVarLimit;
  selectedSOLimit;
  searchFiltersToggle: boolean = true;
  showImages = true;


  @Prop()
  refreshSoSelector;

  @Prop()
  refreshProvComponent;

  @Prop()
  soFilter;

  @Prop()
  graphicTitle;

  @Prop()
  elementName;

  private langUnwatcher;

  mounted() {
    this.langUnwatcher = this.$store.watch(
        () => this.$store.getters.language,
        lang => {
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
    this.experimentDataVisualisationForm.getTotalEventsCount();
  }

  onImageIsHovered(indexes) {
    if (this.visuGraphic) {
      this.visuGraphic.onImageIsHovered(indexes);
    }
  }

  onImageIsUnHovered(indexes) {
    if (this.visuGraphic) {
      this.visuGraphic.onImageIsUnHovered(indexes);
    }
  }

  showEventForm(value) {
    this.target = value.target;
    this.eventCreatedTime = value;
    this.eventsModalForm.showCreateForm();
  }

  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    this.annotationService = this.$opensilex.getService("opensilex.AnnotationsService");
    this.eventsService = this.$opensilex.getService("opensilex.EventsService");
    this.$opensilex.disableLoader();
    this.selectedExperiment = decodeURIComponent(this.$route.params.uri);
    this.VariablesService = this.$opensilex.getService("opensilex.VariablesService");
  }

  searchFiltersPannel() {
    return this.$t("searchfilter.label")
  }

  // simulate window resizing to resize the graphic when the filter panel display changes
  @Watch("searchFiltersToggle")
  onSearchFilterToggleChange() {
    this.$nextTick(() => {
      window.dispatchEvent(new Event('resize'));
    })
  }

  buildColorsSOArray() {
    const colorPalette = [
      "#ca6434",
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
    this.searchFiltersToggle = !this.searchFiltersToggle
    this.initLoader = true;
    this.form = form;
    this.showGraphicComponent = false;
    this.showImages = false;
    this.$opensilex.enableLoader();
    this.multipleVariables = this.selectedVariable.length > 1;

    if (!this.selectedVariable && !this.selectedScientificObjects) {
      return;
    }

    this.selectedSOLimit = 15;
    this.selectedVarLimit = 2;

    // if choosed SO > 15 / Var > 2 : error message.
    if (this.selectedScientificObjects.length > this.selectedSOLimit || this.selectedVariable.length > this.selectedVarLimit) {
      alert(this.$t('ExperimentDataVisualisationView.selectedSOLimitSize'));
      this.selectedSO = false;
      this.selectedVar = false;
    } else {

      this.VariablesService
          .getVariablesByURIs(this.selectedVariable)
          .then((http: HttpResponse<OpenSilexResponse>) => {
                this.selectedVariablesObjectsList = http.response.result;
                const datatype = this.selectedVariablesObjectsList[0].datatype.split("#")[1];
                if (datatype == "decimal" || datatype == "integer") {
                  if (this.form.startDate === "") {
                    this.form.startDate = undefined;
                  }
                  if (this.form.endDate === "") {
                    this.form.endDate = undefined;
                  }
                  this.showImages = true;
                  this.buildColorsSOArray();
                  this.loadSeries();
                } else {
                  this.showGraphicComponent = true;
                  this.showImages = false;
                  this.$opensilex.showInfoToast(
                      this.$i18n.t("ExperimentDataVisualisationView.datatypeMessageA") +
                      " " +
                      datatype +
                      " " +
                      this.$i18n.t("ExperimentDataVisualisationView.datatypeMessageB")
                  );
                }
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
    let promises = [];
    this.dataService = this.$opensilex.getService("opensilex.DataService");

    this.$opensilex.disableLoader();
    promises.push(this.buildEventsSeries());

    for (let variable of this.selectedVariablesObjectsList) {
      promises.push(this.buildDataSeries(variable));
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
        this.visuGraphic.reload(series, this.selectedVariablesObjectsList, this.form);

        // and scroll to graphic
        let graphic = document.querySelector('.experimentDataVisualisationGraphicWithoutForm');
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

    return Promise.all(promises)
        .then(values => {
          this.showImages = true;

          values.forEach(serie => {
            if (Array.isArray(serie)) {
              serie.forEach(element => {
                series.push(element);
              });
            } else if (serie !== undefined) {
              series.push(serie);
            }
          });

          return series;

        })

  }

  // Build each singular Dataserie on dataService.searchDataList format
  async buildDataSerie(concernedItem: { id: string, label: string }, selectedVariable: VariableDetailsDTO, index) {
    if (this.form) {
      let http = await this.dataService
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
              undefined,
              ["date=asc"],
              0,
              50000
          );

      const data = http.response.result as Array<DataGetDTO>;
      let dataLength = data.length;

      if (dataLength === 0) {
        this.$opensilex.showInfoToast(
            this.$t("component.common.search.noDataFound").toString());
      }

      if (dataLength >= 0) {
        const {
          cleanData,
          imageData
        } = await this.transformDataWithImages(data, {scientificObjectUri: concernedItem.id});
        // const cleanData = HighchartsDataTransformer.transformDataForHighcharts(data, {scientificObjectUri: concernedItem.id});
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
        const dataAndImage = [];

        let name = this.multipleVariables ? concernedItem.label + " / " + selectedVariable.name : concernedItem.label

        const dataSerie = {
          name: name,
          data: cleanData,
          visible: true,
          color: this.eventTypesColorArray[concernedItem.id],
          legendColor: this.eventTypesColorArray[concernedItem.id],
          custom: {
            variable: selectedVariable.uri
          }
        }
        dataAndImage.push(dataSerie)

        if (imageData.length > 0) {
          const imageSerie = {
            type: 'flags',
            name: 'Image/' + name,
            data: imageData,
            width: 8,
            height: 8,
            shape: 'circlepin',
            lineWidth: 1,
            point: {
              events: {
                stickyTracking: false,
                mouseOver: e => {
                  const toSend = {
                    imageIndex: e.target.index,
                    serieIndex: e.target.series.index
                  };
                  if (this.visuImages) {
                    this.visuImages.onImagePointMouseEnter(toSend);
                  }
                  e.preventDefault();
                  return false;
                },
                mouseOut: e => {
                  if (this.visuImages) {
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
              concernedItem: event.point.prov_used[0].uri
            };
            this.onImagePointClick(toReturn);
            if (this.visuImages) {
              this.visuImages.onImagePointClick(toReturn);
            }
          };
          dataAndImage.push(imageSerie);
        }

        return dataAndImage;

      }
    } else {
      return null;
    }
  }

  onImagePointClick(point) {
    return this.dataService
        .getDataFileDescription(
            point.concernedItem
        )
        .then((http: HttpResponse<OpenSilexResponse<DataFileGetDTO>>) => {
          const result = http.response.result as any;
          if (result) {
            const data = result as DataFileGetDTO;
            this.imagesFilter(data, point);
          }
        })
        .catch(error => {
          console.log(error)
        });
  }

  imagesFilter(element: DataFileGetDTO, point: any) {
    let path = "/core/datafiles/" + encodeURIComponent(element.uri) + "/thumbnail?scaled_width=640&scaled_height=320";
    let promise = this.$opensilex.viewImageFromGetService(path);
    promise.then((result) => {
      const image: Image = {
        imageUri: element.uri,
        src: result,
        title: this.$opensilex.$dateTimeFormatter.formatLocaleDateTime(element.date),
        type: element.rdf_type,
        objectUri: element.target,
        date: element.date,
        provenanceUri: element.provenance.uri,
        imageIndex: point.imageIndex,
        serieIndex: point.serieIndex
      };
      this.visuImages.addImage(image);
    })
  }

  onImageIsDeleted(indexes) {
    this.visuImages.onImageIsDeleted(indexes);
  }

  onImageDetails(indexes) {
    this.visuImages.onImageDetails(indexes);
  }

  onAnnotationDetails(indexes) {
    this.visuImages.onAnnotationDetails(indexes);
  }

  /**
   * Transforms the data array to a Highcharts point options array, and creates the image point array if needed.
   *
   * @todo Optimize the fetching of annotations
   *
   * @param data
   */
  async transformDataWithImages(data: Array<DataGetDTO>, options?: HighchartsDataTransformerOptions): Promise<{
    cleanData: Array<OpenSilexPointOptionsObject>;
    imageData: Array<ImagePointOptionsObject>
  }> {
    const cleanData = HighchartsDataTransformer.transformDataForHighcharts(data);
    let imageData: Array<ImagePointOptionsObject> = [];

    for (let point of cleanData) {
      if (Array.isArray(point.data.provenance.prov_used) && point.data.provenance.prov_used.length > 0) {
        const annotations = await this.getAnnotations(point.data.provenance.prov_used[0].uri);
        imageData.push({
          ...point,
          title: "I",
          prov_used: point.data.provenance.prov_used,
          imageURI: point.data.provenance.prov_used[0].uri,
          color: annotations.length > 0 ? "#FF0000" : undefined
        });
      }
    }

    return {
      cleanData,
      imageData
    };
  }

  getAnnotations(uri: string): Promise<Array<AnnotationGetDTO>> {
    return this.annotationService
        .searchAnnotations(undefined, uri, undefined, undefined, undefined, 0, 0)
        .then(
            (http: HttpResponse<OpenSilexResponse<Array<AnnotationGetDTO>>>) => {
              return http.response.result as Array<AnnotationGetDTO>;
            }
        );
  }

  // Build EventsSeries for each OS with elements from each EventsSerie
  buildEventsSeries() {
    if (this.form && this.form.showEvents) {
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
  buildEventsSerie(concernedItem: { id: string, label: string }) {
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
              let timestamp;
              // if start date -> stringdate = start date
              if (element.start != null) {
                timestamp = new Date(element.start).getTime();
                // else stringdate = end date
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

            let name = concernedItem.label;
            return {
              type: "flags",
              allowOverlapX: false,
              name: (this.$t('ExperimentDataVisualisationView.events')) + " -> " + name,
              lineWidth: 1,
              yAxis: this.selectedVariablesObjectsList.length,
              data: cleanEventsData,
              style: {
                color: "white"
              },
              fillColor: this.eventTypesColorArray[concernedItem.id],
              legendColor: this.eventTypesColorArray[concernedItem.id]
            };
          } else {
            return undefined;
          }
        });
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
  min-width: calc(100% - 400px);
  max-width: calc(100vw - 400px);
}

.experimentDataVisualisationGraphicWithoutForm {
  min-width: 100%;
  max-width: 100vw;
}
</style>

<i18n>
en:
  ExperimentDataVisualisationView:
    datatypeMessageA: The variable datatype is
    datatypeMessageB: At this time only decimal or integer are accepted
    limitSizeMessageA: "There are "
    limitSizeMessageB: "data for "
    limitSizeMessageC: " .Only the 50 000 first data are displayed."
    selectedSOLimitSize: "The selection has too many lines for this feature, refine your search, maximum= 15 scientific objects and 2 variables."
    events: "Events"

fr:
  ExperimentDataVisualisationView:
    datatypeMessageA: le type de donnée de la variable est
    datatypeMessageB: Pour le moment, seuls les types decimal ou entier sont acceptés
    limitSizeMessageA: "Il y a "
    limitSizeMessageB: "données pour "
    limitSizeMessageC: ".Seules les 50 000 premières valeurs sont affichées. "
    selectedSOLimitSize: "La selection comporte trop de lignes pour cette fonctionnalité, affinez votre recherche, maximum= 15 objets scientifiques et 2 variables."
    events: "Evenements"

</i18n>



