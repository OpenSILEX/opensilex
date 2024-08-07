<template>
  <div ref="page">
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

      <Transition>
        <div v-show="searchFiltersToggle">
          <!--Form-->
          <opensilex-ScientificObjectVisualizationForm
              ref="scientificObjectVisualizationForm"
              :scientificObject="scientificObject.uri"
              @search="onSearch"
          ></opensilex-ScientificObjectVisualizationForm>
        </div>
      </Transition>

      <div class="d-flex justify-content-center mb-3" v-if="!isGraphicLoaded">
        <b-spinner label="Loading..."></b-spinner>
      </div>

      <!--Visualisation-->
      <opensilex-DataVisuGraphic
          v-if="isGraphicLoaded"
          ref="visuGraphic"
          :selectedScientificObjects="scientificObject.uri"
          :graphicTitle="scientificObject.uri"
          :elementName="elementName"   
          @addEventIsClicked="showAddEventComponent"
          @dataAnnotationIsClicked="showAnnotationForm"
          v-bind:class="{
          'ScientificObjectVisualizationGraphic': searchFiltersToggle,
          'ScientificObjectVisualizationGraphicWithoutForm': !searchFiltersToggle
        }"
      ></opensilex-DataVisuGraphic>


      <opensilex-AnnotationModalForm
          ref="annotationModalForm"
          @onCreate="onAnnotationCreated"
      ></opensilex-AnnotationModalForm>

      <opensilex-EventModalForm
          ref="eventsModalForm"
          :target="target"
          :eventCreatedTime="eventCreatedTime"
          @onCreate="onEventCreated"
      ></opensilex-EventModalForm>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref, Watch} from "vue-property-decorator";
import Vue from "vue";
import Highcharts from "highcharts";
import {DataGetDTO, DataService, EventGetDTO, EventsService,} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {DataFileGetDTO} from "opensilex-core/model/dataFileGetDTO";
import {Image} from "../visualization/image";
import {VariablesService} from "opensilex-core/api/variables.service";
import {AnnotationGetDTO} from "opensilex-core/model/annotationGetDTO";
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import HighchartsDataTransformer, {OpenSilexPointOptionsObject} from "../../models/HighchartsDataTransformer";
import {ProvEntityModel} from "opensilex-core/model/provEntityModel";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

interface ImagePointOptionsObject extends OpenSilexPointOptionsObject {
  prov_used: Array<ProvEntityModel>,
  imageURI: string
}

@Component
export default class ScientificObjectVisualizationTab extends Vue {
  $opensilex: OpenSilexVuePlugin;
  annotationData: any;
  variablesService: VariablesService;
  annotationService: AnnotationsService;
  searchFiltersToggle: boolean = true;

  get user() {
    return this.$store.state.user;
  }

  @Prop()
  scientificObject;

  @Prop()
  elementName;

  isGraphicLoaded = true;
  target = [];
  eventCreatedTime = "";

  form;
  selectedVariable;
  dataService: DataService;
  eventsService: EventsService;
  @Ref("page") readonly page!: any;
  @Ref("visuImages") readonly visuImages!: any;
  @Ref("visuGraphic") readonly visuGraphic!: any;
  @Ref("annotationModalForm") readonly annotationModalForm!: any;
  @Ref("eventsModalForm") readonly eventsModalForm!: any;
  @Ref("scientificObjectVisualizationForm")
  readonly scientificObjectVisualizationForm!: any;
  showImages = true;
  annotations: Array<AnnotationGetDTO> = [];

  created() {
    this.dataService = this.$opensilex.getService("opensilex.DataService");
    this.annotationService = this.$opensilex.getService("opensilex.AnnotationsService");
    this.eventsService = this.$opensilex.getService("opensilex.EventsService");
    this.variablesService = this.$opensilex.getService("opensilex.VariablesService");
  }

  private langUnwatcher;

  mounted() {
    this.langUnwatcher = this.$store.watch(
        () => this.$store.getters.language,
        lang => {
          this.prepareGraphic();
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

  onEventCreated() {
    this.prepareGraphic();
    this.scientificObjectVisualizationForm.getEvents();
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


  showAddEventComponent(time) {
    this.target = this.scientificObject.uri;
    this.eventCreatedTime = time;
    this.eventsModalForm.showCreateForm();
  }

  showAnnotationForm(target) {
    this.annotationModalForm.showCreateForm([target]);
  }

  onSearch(form) {
    this.searchFiltersToggle = !this.searchFiltersToggle;
    this.isGraphicLoaded = false;
    this.showImages = false;
    this.$opensilex.disableLoader();
    if (form.variable) {
      this.form = form;
      this.variablesService
          .getVariable(form.variable)
          .then((http: HttpResponse<OpenSilexResponse>) => {
            this.selectedVariable = http.response.result;
            const datatype = this.selectedVariable.datatype.split("#")[1];
            if (datatype == "decimal" || datatype == "integer") {
              this.prepareGraphic();
            } else {
              this.isGraphicLoaded = true;
              this.showImages = true;
              this.$opensilex.showInfoToast(
                  this.$i18n.t(
                      "ScientificObjectVisualizationTab.datatypeMessageA"
                  ) +
                  " " +
                  datatype +
                  " " +
                  this.$i18n.t(
                      "ScientificObjectVisualizationTab.datatypeMessageB"
                  )
              );
            }
          })
          .catch(error => {
            this.isGraphicLoaded = true;
            this.showImages = true;
            this.$opensilex.errorHandler(error);
          });
    }
  }

  prepareGraphic() {
    if (this.form) {
      this.$opensilex.disableLoader();
      let promises = [];
      let promise;
      promise = this.buildDataSerie();
      promises.push(promise);
      promise = this.buildEventsSerie();
      promises.push(promise);

      Promise.all(promises)
          .then(values => {
            this.isGraphicLoaded = true;
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
            this.isGraphicLoaded = true;
            this.showImages = true;
            this.$opensilex.errorHandler(error);
          });
    }
  }

  buildEventsSerie() {
    if (this.form && this.form.showEvents) {
      return this.eventsService
          .searchEvents(
              undefined,
              this.form.startDate != undefined && this.form.startDate != ""
                  ? this.form.startDate
                  : undefined,
              this.form.endDate != undefined && this.form.endDate != ""
                  ? this.form.endDate
                  : undefined,
              this.scientificObject.uri,
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

              let eventTypesColorArray = [];
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
                "#caaf34"
              ];
              let index = 0;

              events.forEach(element => {
                if (!eventTypesColorArray[element.rdf_type]) {
                  eventTypesColorArray[element.rdf_type] = colorPalette[index];
                  index++;
                  if (index === 12) {
                    index = 0;
                  }
                }
                label = element.rdf_type_name
                    ? element.rdf_type_name
                    : element.rdf_type;
                if (element.start != null) {
                  let endTime = element.end ? element.end : "en cours..";
                  label = label + "(End: " + endTime + ")";
                }
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
                  fillColor: eventTypesColorArray[element.rdf_type]
                };

                cleanEventsData.push(toAdd);
              });
              return {
                type: "flags",
                allowOverlapX: false, // when flags have same datetimes
                name: "Events",
                lineWidth: 1,
                yAxis: 1,
                data: cleanEventsData,
                style: {
                  // text style
                  color: "white"
                }
              };
            } else {
              return undefined;
            }
          });
    } else {
      return null;
    }
  }

  async buildDataSerie() {
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
              undefined,
              [this.scientificObject.uri],
              [this.form.variable],
              undefined,
              undefined,
              undefined,
              this.form.provenance ? [this.form.provenance] : undefined,
              undefined, //this.addMetadataFilter(),
              undefined,
              ["date=asc"],
              0,
              this.$store.state.graphDataLimit
          );

      const data = http.response.result as Array<DataGetDTO>;

      let dataLength = data.length;

      if (dataLength === 0) {
        this.$opensilex.showInfoToast(
            this.$t("component.common.search.noDataFound").toString());
      }

      if (dataLength >= 0) {
        const {cleanData, imageData} = await this.transformDataWithImages(data);

        const dataAndImage = [];

        const dataSerie = {
          name: this.scientificObject.name,
          data: cleanData,
          visible: true
        };
        dataAndImage.push(dataSerie)
        if (imageData.length > 0) {
          const imageSerie = {
            type: 'flags',
            name: 'Image/' + this.scientificObject.name,
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
  async transformDataWithImages(data: Array<DataGetDTO>): Promise<{
    cleanData: Array<OpenSilexPointOptionsObject>;
    imageData: Array<ImagePointOptionsObject>
  }> {
    const cleanData = HighchartsDataTransformer.transformDataForHighcharts(data);
    let imageData: Array<ImagePointOptionsObject> = [];
    var annotations = [];

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
.ScientificObjectVisualizationImage {
  height: 150px;
}

.ScientificObjectVisualizationWithoutImage {
  height: 0px;
}

.ScientificObjectVisualizationGraphic {
  min-width: calc(100% - 450px);
  max-width: calc(100vw - 380px);
}

.ScientificObjectVisualizationGraphicWithoutForm {
  min-width: 100%;
  max-width: 100vw;
}
</style>

<i18n>
en:
  ScientificObjectVisualizationTab:
    visualization: Visualization
    data: Data
    add: Add data
    datatypeMessageA: The variable datatype is
    datatypeMessageB: At this time only decimal or integer are accepted
    limitSizeMessageA: "There are "
    limitSizeMessageB: " data .Only the 50 000 first data are displayed."

fr:
  ScientificObjectVisualizationTab:
    visualization: Visualisation
    data: Données
    add: Ajouter des données
    datatypeMessageA: le type de donnée de la variable est
    datatypeMessageB: Pour le moment, seuls les types decimal ou entier sont acceptés
    limitSizeMessageA: "Il y a "
    limitSizeMessageB: " données .Seules les 50 000 premières valeurs sont affichées. "
</i18n>
