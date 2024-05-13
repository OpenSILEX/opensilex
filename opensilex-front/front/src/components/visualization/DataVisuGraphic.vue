<template>
  <div class="graphContainer">
    <b-list-group
        v-show="contextMenuShow"
        ref="contextMenu"
        class="contextMenu"
        :style="{ top: topPosition + 'px', left:leftPosition + 'px' }"
    >
      <b-list-group-item href="#" @click="addEventClick">{{ $t("DataVisuGraphic.addEvent") }}</b-list-group-item>

      <b-list-group-item
          href="#"
          @click="dataAnnotationClick"
      >{{ $t("DataVisuGraphic.dataAnnotation") }}</b-list-group-item>
    </b-list-group>

    <div class="card">
      <div ref="header" class="card-header" v-if="chartOptions.length">
        <opensilex-HelpButton :label="helpText" @click="helpModal.show()" class="dataVisuGraphicHelpButton"></opensilex-HelpButton>

        <div class="card-header-right mr-4">
          <b-dropdown right size="lg" variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content>
              <opensilex-Icon icon="fa#bars" size="lg" />
              <span class="sr-only">Search</span>
            </template>

            <b-dropdown-item v-if="this.lineType" href="#" @click="scatter">
              <opensilex-Icon icon="fa#braille" />
              {{ $t("DataVisuGraphic.scatterPlotView") }}
            </b-dropdown-item>
            <b-dropdown-item v-else href="#" @click="scatter">
              <opensilex-Icon icon="fa#chart-line" />
              {{ $t("DataVisuGraphic.chartLineView") }}
            </b-dropdown-item>

            <b-dropdown-item href="#" @click="fullscreen">
              <opensilex-Icon icon="fa#expand" />
              {{ $t("DataVisuGraphic.fullscreen") }}
            </b-dropdown-item>

            <b-dropdown-item href="#" @click="exportPNG">
              <opensilex-Icon icon="fa#download" />
              {{ $t("DataVisuGraphic.download") }}
            </b-dropdown-item>
          </b-dropdown>
        </div>
      </div>

      <div v-click-outside="closeMenu" @click="closeContextMenu" class="card-body p-0">
        <highcharts
            v-for="(options, index) in chartOptions"
            :options="options"
            v-bind:key="index"
            :constructor-type="'stockChart'"
            ref="highcharts"
        ></highcharts>
      </div>
    </div>

    <div class="text-center" v-if="detailDataLoad">
      <b-spinner type="grow" label="Spinning"></b-spinner>
    </div>

    <b-card v-if="detailEventShow ">
      <b-card-text
          v-if="event.rdf_type_name"
      >{{ $t("component.common.type") }}: {{event.rdf_type_name}}</b-card-text>

      <b-card-text v-else>{{ $t("component.common.type") }}:{{event.rdf_type}}</b-card-text>

      <b-card-text v-if="event.start">{{ $t("component.common.begin") }}: {{formatEventDate(event.start)}}</b-card-text>
      <b-card-text v-else>{{ $t("component.common.begin") }}: {{formatEventDate(event.end)}}</b-card-text>
      <b-card-text v-if="event.start">{{ $t("component.common.end") }}: {{formatEventDate(event.end)}}</b-card-text>
      <b-card-text
          v-if="event.description"
      >{{ $t("component.common.description") }}: {{event.description}}</b-card-text>

      <b-card-text>{{ $t("component.common.creator") }}: {{event.author}}</b-card-text>
    </b-card>

    <b-card-group deck v-if="detailDataShow ">
      <b-card title="Provenance">
        <pre>{{ provenance }}</pre>
      </b-card>
      <b-card title="Data">
        <pre>{{ data }}</pre>
      </b-card>
    </b-card-group>

    <b-card title="Annotation" v-if="detailDataShow ">
      <b-list-group>
        <b-list-group-item
            v-for="(annotation, index) in annotations"
            href="#"
            class="flex-column align-items-start"
            v-bind:key="index"
        >
          <div class="d-flex w-100 justify-content-between">
            <h5 class="mb-1">{{annotation.motivation.name}}</h5>
            <small class="text-muted">{{annotation.created}}</small>
          </div>

          <p class="mb-1">{{annotation.description}}</p>

          <small class="text-muted">{{annotation.author}}</small>
        </b-list-group-item>
      </b-list-group>
    </b-card>

    <b-modal size="lg" ref="helpModal" scrollable centered hide-footer>
      <opensilex-DataVisuHelp @hideBtnIsClicked="hide()"></opensilex-DataVisuHelp>
    </b-modal>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {AnnotationGetDTO, EventDetailsDTO, ProvenanceGetDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import Highcharts from "highcharts";

import ClickOutside from "vue-click-outside";
import exportingInit from "highcharts/modules/exporting";
import HighchartsCustomEvents from "highcharts-custom-events";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {DataService} from "opensilex-core/api/data.service";
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {EventsService} from "opensilex-core/api/events.service";
import {VariableGetDTO} from "opensilex-core/model/variableGetDTO";
import {OpenSilexStore} from "../../models/Store";

/**
 * Custom type for highcharts options. The event 'contextmenu', corresponding to the
 * right-click of the mouse, is not defined as a typing by Highcharts (even though it works
 * correctly), so we must specify it here.
 */
type HighchartsOptions = Highcharts.Options & {
  plotOptions: {
    series: {
      events: {
        contextmenu: (e: Event) => void
      },
      point: {
        events: {
          contextmenu: (e: Event) => void
        }
      }
    }
  }
};

HighchartsCustomEvents(Highcharts);
exportingInit(Highcharts);

@Component({
  directives: {
    ClickOutside
  }
})
export default class DataVisuGraphic extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: OpenSilexStore;

  @Ref("helpModal") readonly helpModal!: any;
  @Ref("contextMenu") readonly contextMenu!: any;
  @Ref("highcharts") readonly highchartsRef!: any;
  contextMenuShow = false;
  //  intervalContextMenuShow = false;
  detailDataShow = false;
  detailDataLoad = false;
  detailEventShow = false;
  topPosition = 0;
  leftPosition = 0;
  selectedData;
  selectedObject;
  selectedProvenance;
  selectedTime;
  selectedTimeToSend;
  selectedDate;
  selectedValue;
  selectedOffset;
  variables: Array<VariableGetDTO>;
  selectedPointsCount = 0;

  lineType = false;
  lineWidth = false;
  yAxis: Array<Highcharts.YAxisOptions> | Highcharts.YAxisOptions = {
    title: {
      text: ""
    }
  };
  showEvents = false;
  series: Array<Highcharts.SeriesLineOptions> = [];
  private concernedItem;
  chartOptionsValues: any = [];

  startDate;
  endDate;

  get helpText() {
    return this.showEvents ? this.$i18n.t("DataVisuGraphic.rightClick") :  ""
  }

  @Prop({
    default: false
  })
  deviceType;

  @Prop({
    default: true
  })
  activateContextMenuShow;

  @Prop({
    default: true
  })
  isAddEvents;

  @Prop({
    default: false
  })
  lType;

  @Prop({
    default: false
  })
  lWidth;

  @Prop()
  graphicTitle;

  @Prop()
  elementName;

  exportDateFormated;


  data: any = null;
  provenance: any = null;
  annotations = [];
  event: any = null;


  created() {
    Highcharts.wrap(Highcharts.Legend.prototype, "colorizeItem", function(
        proceed,
        item,
        visible
    ) {
      var color = item.color;
      item.color = item.options.legendColor;
      proceed.apply(this, Array.prototype.slice.call(arguments, 1));
      item.color = color;
    });

    this.lineType = this.lType;
    this.lineWidth = this.lWidth;
  }


  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
        () => this.$store.getters.language,
        lang => {
          this.langOptionChanges(lang);
        }
    );

    let lang = this.$store.getters.language;
    this.langOptionChanges(lang);

    let todayDate = new Date();
    let days = todayDate.getDate().toString().padStart(2, '0');
    let months = (todayDate.getMonth() + 1).toString().padStart(2, '0'); // (this method start at month "0")
    let years = todayDate.getFullYear();
    let formatedDate = years + "-" + months + "-" + days;
    this.exportDateFormated = formatedDate;
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  getEventDetail(uri) {
    return this.$opensilex
        .getService<EventsService>("opensilex.EventsService")
        .getEventDetails(uri)
        .then((http: HttpResponse<OpenSilexResponse<EventDetailsDTO>>) => {
          return http.response.result;
        });
  }

  showEventDetail(uri) {
    this.detailDataLoad = true;

    this.getEventDetail(uri).then(event => {
      this.event = event;
      this.detailDataLoad = false;
      this.detailDataShow = false;
      this.detailEventShow = true;
    });
  }

  getAnnotations(uri) {
    return this.$opensilex
        .getService<AnnotationsService>("opensilex.AnnotationsService")
        .searchAnnotations(undefined, uri, undefined, undefined, undefined, 0, 0)
        .then(
            (http: HttpResponse<OpenSilexResponse<Array<AnnotationGetDTO>>>) => {
              const annotations = http.response.result as Array<AnnotationGetDTO>;
              return annotations;
            }
        );
  }

  showAnnotations(dataUri) {
    this.getAnnotations(dataUri)
        .then(annotations => {
          this.annotations = annotations;
        })
        .catch(error => {
          this.$opensilex.errorHandler(error);
        });
  }

  getProvenance(uri) {
    return this.$opensilex
        .getService<DataService>("opensilex.DataService")
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
  }

  showProvenanceDetailComponent(value) {
    if (value.provenance != undefined && value.provenance != null) {
      this.detailDataLoad = true;
      this.getProvenance(value.provenance)
          .then(provenance => {
            value.provenance = provenance;
            this.data = JSON.stringify(value.data, null, 2);
            this.provenance = JSON.stringify(value.provenance, null, 2);
            this.detailEventShow = false;
            this.detailDataLoad = false;
            this.detailDataShow = true;
          })
          .catch(error => {
            this.$opensilex.errorHandler(error);
          });
    }
  }

  get chartOptions(): Array<HighchartsOptions | {
    xAxis: {
      marginBottom: number
    }
  }> {
    const that = this;
    let previousPoint;
    let previousPointColor;
    if (this.series.length > 0) {
      return [
        {
          chart: {
            zoomType: "x",
            marginLeft: 80,
            marginRight: 80,
            height: that.series.length > 8 ? 800 : 700,
            type: that.lineType ? "line" : "scatter",
            events: {
              load: function() {
                that.addLegendHeight(this);
              },
              click: () => {
                let chart = that.highchartsRef[0].chart;
                chart.tooltip.hide(0);
              },
              render: function() {
                that.selectedPointsCount = 0;
                that.series.forEach(serie => {
                  // limit download image
                  if (serie.data && serie.name !== "Navigator 1") {
                    that.selectedPointsCount += serie.data.length;
                  }
                });
                that.detailEventShow = false;
                that.detailDataShow = false;
                that.$emit("graphicCreated");
              }
            }
          },
          subtitle: {
            text: ""
          },
          credits: { enabled: false },
          navigation: {
            buttonOptions: {
              //custom menu
              enabled: false
            }
          },
          exporting: {
            sourceWidth: 1600,
            sourceHeight: 800,
            scale: 2,
            chartOptions: {
              title: {
                text: this.$i18n.t("DataVisuGraphic.name") + " : " + this.elementName + "<br/>" + " URI : " + this.graphicTitle,
              },
              legend: {
                itemWidth: 600,
                itemDistance: 110
              }
            }
          },
          scrollbar: {
            enabled: false
          },
          rangeSelector: {
            //zoom menu
            enabled: false
          },
          navigator: {
            //zoom navigator
            enabled: !!this.deviceType,
            margin: 5
          },
          legend: {
            enabled: true,
            floating: false,
            verticalAlign: "bottom",
            align: "center",
            layout: "horizontal"

          },
          xAxis: {
            type: "datetime",
            title: { text: "time" },
            min: this.startDate? new Date(this.startDate).getTime(): undefined,
            max: this.endDate? new Date(this.endDate).getTime(): undefined,
            labels: {
              formatter: function() {
                return Highcharts.dateFormat("%e-%b-%Y %H:%M:%S", this.value);
              }
            },
            ordinal: false,
            crosshair: true,
            showLastLabel: true,
            endOnTick: true
          },
          yAxis: this.yAxis,

          tooltip: {
            useHTML: true,
            formatter: function(tooltip) {
              const point = this.point as Highcharts.Point & {
                data: any,
                text: any
              };
              if (point.y) {
                return (
                    "" +
                    point.series.name +
                    " :" +
                    '<span style=" color:' +
                    point.color +
                    '" ><b> ' +
                    point.data.value +
                    "</b></span>" +
                    "<br/>Time:<b> " +
                    that.$opensilex.$dateTimeFormatter.formatLocaleDateTime(point.data.date) +
                    "</b> " +
                    "<br/> Target: <b>" +
                    point.data.target +
                    "</b>"
                );
              } else {
                return (
                    "" +
                    point.series.name +
                    " :" +
                    '<span style=" color:' +
                    point.color +
                    '" ><b>' +
                    point.data.value +
                    "</b></span>" +
                    "<br/>Time:<b> " +
                    that.$opensilex.$dateTimeFormatter.formatLocaleDateTime(this.x) +
                    "</b> " +
                    "<br/> Target: <b>" +
                    point.data.target +
                    "</b>"
                );
              }
            },
            split: false,
            shared: false
          },
          series: this.series,
          plotOptions: {
            line: {
              marker: {
                enabled: true,
                symbol: "circle",
                radius: 3,
                fillColor: '#fff004'
              }
            },

            series: {
              turboThreshold: 100000,
              cursor: "pointer",
              dataGrouping: {
                enabled: false,
              },
              lineWidth: this.lineWidth ? 2 : 0, //scatter plot
              stickyTracking: false, //tooltip only on hover
              marker: {
                //don't hide serie on hover
                states: {
                  hover: {
                    enabled: true
                  },
                  radius: 2
                }
              },
              states: {
                inactive: {
                  opacity: 0.4
                }
              },
              events: {
                click: function(e) {
                  if (that.showEvents) {
                    that.lineClick(e, this);
                  }
                },
                contextmenu: function(e) {
                  e.preventDefault();
                }
              },
              point: {
                events: {
                  click: function(e) {
                    e.stopPropagation();
                    that.pointClick(e, this);
                    if (previousPoint)
                      previousPoint.update({
                        color: previousPointColor
                      });
                    // Keep the previous color to reset
                    previousPointColor = this.color;
                    // Set this points color to black
                    this.update({ color: "black" });
                    // Make it our previous point
                    previousPoint = this;
                  },
                  contextmenu: function(e) {
                    e.preventDefault();
                    that.activateContextMenuShow ?
                        that.pointRightClick(e, this) : '';
                  }
                }
              }
            }
          }
        }
      ];
    } else {
      return [];
    }
  }

  formatEventDate(date: string) {
    return this.$opensilex.$dateTimeFormatter.formatLocaleDateTime(date);
  }

  scatter() {
    this.lineType = !this.lineType;
    this.lineWidth = !this.lineWidth;
  }

  linkToCorrectAxis(){
    for (let serie of this.series){
      if (serie.custom?.variable  === this.variables[0].uri) {
        serie.yAxis = 0;
      }
      if (serie.custom?.variable  === this.variables[1].uri) {
        serie.yAxis = 1;
      }
    }
    this.yAxis[0].labels.x = 25; // space between scale numbers and graphic
    this.yAxis[0].title.x = 25; // space between scale numbers and Yaxis title
    this.yAxis[1].opposite = false;
    this.yAxis[1].labels.x = -10;
    this.yAxis[1].title.x = -10;
  }


  closeContextMenu() {
    this.contextMenuShow = false;
  }

  closeMenu() {
    this.closeContextMenu();
  }

  reload(series: Array<Highcharts.SeriesLineOptions>, variable: VariableGetDTO | Array<VariableGetDTO>, form) {
    if(form) {
      this.startDate = form.startDate;
      this.endDate = form.endDate;
    }

    if(form && form.showEvents) {
      this.showEvents = form.showEvents;
    }

    this.detailDataShow = false;
    this.detailEventShow = false;

    this.variables = Array.isArray(variable)
      ? variable
      : [variable];
    if (series.length > 0) {
      this.yAxis = this.buildYAxis(this.showEvents);
    }

    // If numpber of data is this.$store.state.graphDataLimit -> data was truncated -> warn user
    series.forEach((serie) => {
      if (serie.data.length == this.$store.state.graphDataLimit) {
        console.log(serie.data[-1])
        this.$opensilex.showWarningToast(
            this.$i18n.t("DataVisuGraphic.dataLimit", {
              dataLimit: this.$store.state.graphDataLimit,
              //@ts-ignore
              finalDate: serie.data[serie.data.length - 1].data.date,
              variable: serie.name
            }).toString()
        )
      }
    })

    this.series = series;
    this.linkToCorrectAxis();
  }

  buildYAxis(showEvents): Array<Highcharts.YAxisOptions> {
    let yAxis: Array<Highcharts.YAxisOptions> = this.variables.map(variable => {
        return {
          labels: {
            align: "right",
          },
          title: {
            text: variable
                ? variable.name + " (" + variable.unit.name + ")"
                : "",
              x: 20,
            style: {
              color: Highcharts.getOptions().colors[1],
              fontWeight: 'bold',
              fontSize: window.innerWidth < 1200 ? "12" : "13" //fontSize is a string
            }
          }
        };
      });


    if (showEvents) {
      // Resize the var axis
      for (let axis of yAxis) {
        axis.lineWidth = 2;
        axis.height = "80%"
        axis.resize = {
          enabled: true
        }
      }

      // Event axis
      yAxis.push({
        labels: {
          align: "right",
          x: -3
        },
        title: {
          text: "Event"
        },
        top: "85%",
        height: "15%",
        offset: 0,
        lineWidth: 2
      });
    }

    return yAxis;
  }

  pointRightClick(e, graphic) {
    this.closeMenu();
    let chart = graphic.series.chart;
    chart.tooltip.hide();
    if (e.point.data && graphic.series) {
      this.contextMenuShow = true;
      let chartWidth = this.highchartsRef[0].chart.chartWidth;

      this.$nextTick(() => {
        // wait to have the contextMenu Width
        let menuWidth = this.contextMenu.clientWidth;

        if (e.chartX + menuWidth > chartWidth) {
          this.leftPosition = e.pageX - menuWidth - 10;
        } else {
          this.leftPosition = e.pageX + 10;
        }

        this.topPosition = e.pageY;
      });
      this.selectedValue = e.point.y;
      this.selectedObject = e.point.objectUri ? e.point.objectUri : e.point.deviceUri;
      this.selectedProvenance = e.point.provenanceUri;
      this.selectedData = e.point.data.uri;

      this.selectedTime = e.point.data.date;

      this.selectedTimeToSend = new Date(this.selectedTime).toISOString();
      this.selectedOffset = "Z";
    }
  }
  pointClick(e, graphic) {
    this.closeMenu();
    if (e.point.data && graphic.series) {
      let value = {
        data: e.point.data,
        provenance: e.point.provenanceUri
      };
      this.selectedData = e.point.data.uri;
      this.showProvenanceDetailComponent(value);
      this.showAnnotations(e.point.data.uri);
    } else {
      this.showEventDetail(e.point.eventUri);
    }
  }
  updateDataAnnotations() {
    if (this.selectedData) {
      this.showAnnotations(this.selectedData);
    }
  }

  lineClick(e, graphic) {
    this.closeMenu();
  }

  fullscreen() {
    this.highchartsRef[0].chart.fullscreen.toggle();
  }

  dataAnnotationClick() {
    this.contextMenuShow = false;
    this.$emit("dataAnnotationIsClicked", this.selectedData);
  }

  addEventClick() {
    this.contextMenuShow = false;
    this.$emit("addEventIsClicked", {
      time: this.selectedTimeToSend,
      offset: this.selectedOffset,
      target: this.selectedObject
    });
  }

  exportPNG() {
    if (this.selectedPointsCount < 1500) {
      this.highchartsRef[0].chart.exportChart({
        type: "image/png",
        filename: this.exportDateFormated + "_graphic_opensilex"
      });
    } else {
      this.$opensilex.showInfoToast(
          "Too much points : " +
          this.selectedPointsCount +
          " Must be < 1500 points"
      );
    }
  }

  langOptionChanges(lang) {
    if (lang === "fr") {
      Highcharts.setOptions({
        lang: {
          months: [
            "Janvier",
            "Février",
            "Mars",
            "Avril",
            "Mai",
            "Juin",
            "Juillet",
            "Août",
            "Septembre",
            "Octobre",
            "Novembre",
            "Décembre"
          ],
          weekdays: [
            "Dimanche",
            "Lundi",
            "Mardi",
            "Mercredi",
            "Jeudi",
            "Vendredi",
            "Samedi"
          ],
          shortMonths: [
            "jan",
            "fév",
            "mar",
            "avr",
            "mai",
            "juin",
            "juil",
            "aoû",
            "sep",
            "oct",
            "nov",
            "déc"
          ],
          resetZoom: "Réinitialiser le zoom",
          decimalPoint: "."
        }
      });
    } else {
      Highcharts.setOptions({
        lang: {
          months: [
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December"
          ],
          weekdays: [
            "Sunday",
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday"
          ],
          shortMonths: [
            "Jan",
            "Feb",
            "Mar",
            "Apr",
            "May",
            "Jun",
            "Jul",
            "Aug",
            "Sep",
            "Oct",
            "Nov",
            "Dec"
          ],
          resetZoom: "Reset zoom",
          decimalPoint: "."
        }
      });
    }
  }
  hide() {
    this.helpModal.hide();
  }

  onImageIsHovered(indexes) {
    var chart = this.highchartsRef[0].chart;
    chart.series[indexes.serie].data[indexes.point].setState("hover");
    chart.tooltip.refresh(chart.series[indexes.serie].data[indexes.point]);
  }

  onImageIsUnHovered(indexes) {
    var chart = this.highchartsRef[0].chart;
    chart.series[indexes.serie].data[indexes.point].setState();
    chart.tooltip.hide();
  }

  addLegendHeight(highcharts){
    let legendSpace = highcharts.legend.addLegendHeight - highcharts.legend.padding;

    if (legendSpace){
      highcharts.setSize(null, highcharts.chartHeight + legendSpace, false);
    }
  }
}
</script>

<style scoped lang="scss">

.graphContainer {
  height: auto;
  min-height:600px;
}

.card-header {
  height: 60px;
}

.dataVisuGraphicHelpButton {
  color: #00A38D;
  border:none;
  margin-bottom: 5px;
}

.dataVisuGraphicHelpButton:hover{
  background-color: #00A38D;
  border-color: #00A38D;
  color: #f1f1f1
}

.contextMenu {
  position: absolute;
  z-index: 1001;
  max-width: 140px;
  -webkit-transform: translateY(-100%);
  transform: translateY(-100%);
}
.contextMenu .list-group-item {
  padding: 0.25rem 0.25rem;
  background-color: #f7f7f7;
  border: 1px solid #cccccc;
}
.contextMenu .list-group-item:hover {
  background-color: #cccccc;
}
</style>


<i18n>
fr:
  DataVisuGraphic:
    provenanceDetail : Details de la provenance
    dataAnnotation : Annoter la donnée
    scientificObjectAnnotation : Ajouter une annotation à l' objet scientifique
    addEvent : Ajouter un evenement
    scatterPlotView : Mode nuage de points
    chartLineView : Mode courbe
    fullscreen : Plein ecran
    download : Télecharger l'image
    rightClick : click droit sur un point pour ajouter un evénement ou une annotation
    name: Nom
    dataLimit: Seules les {dataLimit} premières données de '{variable}' ont été chargées. La date finale a été reculée à '{finalDate}'. Vous pouvez mettre à jour la date de début pour charger des données ultérieures.

en:
  DataVisuGraphic:
    provenanceDetail : Provenance detail
    dataAnnotation : Annotate data
    scientificObjectAnnotation : Add scientific object's annotation
    addEvent : Add an event
    scatterPlotView : Scatter plot view
    chartLineView : Chart line view
    fullscreen : Fullscreen
    download : Download image
    rightClick : right click on a point to add event or annotation
    name: Name
    dataLimit: Only the first {dataLimit} data of '{variable}' were loaded. The final date was moved back to '{finalDate}'. You can update your starting date to load later data.
</i18n>
