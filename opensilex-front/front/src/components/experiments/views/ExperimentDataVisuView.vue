<template>
  <div class="experimentDataVisuView">
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
          <i class="icon ik ik-search"></i>
        </div>
      </div>

      <!-- Form -->
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
          v-bind:class="{
        'experimentDataVisuGraphic': searchFiltersToggle,
        'experimentDataVisuGraphicWithoutForm': !searchFiltersToggle
      }"
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
import {DataGetDTO, DataService, EventGetDTO, EventsService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import HighchartsDataTransformer, {
  HighchartsDataTransformerOptions,
  OpenSilexPointOptionsObject
} from "../../../models/HighchartsDataTransformer";
import {ProvEntityModel} from "opensilex-core/model/provEntityModel";
import {AnnotationGetDTO} from "opensilex-core/model/annotationGetDTO";
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {DataFileGetDTO} from "opensilex-core/model/dataFileGetDTO";
import {Image} from "../../visualization/image";

interface ImagePointOptionsObject extends OpenSilexPointOptionsObject {
  prov_used: Array<ProvEntityModel>,
  imageURI: string
}

@Component
export default class ExperimentDataVisuView extends Vue {
  $route: any;
  $opensilex: any;
  dataService: DataService;
  annotationService: AnnotationsService;

  target = [];
  eventCreatedTime = "";

  form;
  chartOptionsValue: any;
  @Ref("visuImages") readonly visuImages!: any;
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;
  @Ref("eventsModalForm") readonly eventsModalForm!: any;
  @Ref("experimentDataVisuForm") readonly experimentDataVisuForm!: any;

  showSearchComponent: boolean = true;
  showGraphicComponent: boolean = false;
  initLoader: boolean = false;
  selectedExperiment;
  selectedVariable;
  eventsService: EventsService;
  eventTypesColorArray = [];
  searchFiltersToggle: boolean = true;
  showImages = true;

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
  onSearchFilterToggleChange() {
    this.$nextTick(() => {
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
    this.showImages = false;
    this.$opensilex.enableLoader();
    this.$opensilex
        .getService("opensilex.VariablesService")
        .getVariable(form.variable[0])
        .then((http: HttpResponse<OpenSilexResponse>) => {
          this.selectedVariable = http.response.result;
          const datatype = this.selectedVariable.datatype.split("#")[1];
          if (datatype == "decimal" || datatype == "integer") {
            this.showImages = true;
            this.buildColorsSOArray();
            this.loadSeries();
          } else {
            this.showGraphicComponent = true;
            this.showImages = true;
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

  // BuildSeries only if form is completed
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
    let promises = [],
        promise;
    this.selectedScientificObjects.forEach((element, index) => {
      promise = this.buildDataSerie(element);
      promises.push(promise);
    });
    Promise.all(promises)
        .then(values => {
          this.showImages = true;
          let series = [];

          if (values[0]) {
            values[0].forEach(element => {
              series.push(element);
            })
          }
          if (values[1]) {
            series.push(values[1]);
          }
          this.$nextTick(() => {
            this.$opensilex.enableLoader();
            this.visuGraphic.reload(
                series,
                this.selectedVariable,
                this.form
            );
          });
        })
        .catch(error => {
          this.showImages = true;
          this.$opensilex.errorHandler(error);
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
              name: "Events ->" + name,
              lineWidth: 1,
              yAxis: 1,
              data: cleanEventsData,
              style: {
                // text style
                color: "white"
              },
              fillColor: this.eventTypesColorArray[concernedItem.uri],
              legendColor: this.eventTypesColorArray[concernedItem.uri]

            };
          } else {
            return undefined;
          }
        });
  }

  async buildDataSerie(concernedItem) {
    if (this.form) {
      let http = await this.dataService
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
        } = await this.transformDataWithImages(data, {scientificObjectUri: concernedItem.uri});
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
        const dataAndImage = [];

        let name = concernedItem.name ? concernedItem.name : concernedItem.uri
        const dataSerie = {
          name: name,
          data: cleanData,
          id: 'A',
          visible: true,
          color: this.eventTypesColorArray[concernedItem.uri],
          legendColor: this.eventTypesColorArray[concernedItem.uri]
        };
        dataAndImage.push(dataSerie)

        if (imageData.length > 0) {
          const imageSerie = {
            type: 'flags',
            name: 'Image/' + name,
            data: imageData,
            onSeries: 'A',
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

.experimentDataVisuGraphicWithoutForm {
  min-width: 100%;
  max-width: 100vw;
}

.visualizeBtn {
  float: right;
  border: none;
  background: #00A28C;
}

.visualizeBtn:disabled,
.visualizeBtn[disabled] {
  background-color: #666666;
  color: #F1F1F1;
}
</style>

<i18n>
en:
  ExperimentDataVisuView:
    datatypeMessageA: The variable datatype is
    datatypeMessageB: At this time only decimal or integer are accepted
    limitSizeMessageA: "There are "
    limitSizeMessageB: "data for "
    limitSizeMessageC: " .Only the 50 000 first data are displayed."

fr:
  ExperimentDataVisuView:
    datatypeMessageA: le type de donnée de la variable est
    datatypeMessageB: Pour le moment, seuls les types decimal ou entier sont acceptés
    limitSizeMessageA: "Il y a "
    limitSizeMessageB: "données pour "
    limitSizeMessageC: ".Seules les 50 000 premières valeurs sont affichées. "

</i18n>

