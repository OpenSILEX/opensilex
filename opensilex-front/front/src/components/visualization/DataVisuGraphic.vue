<template>
  <div style="min-height:300px;">
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

    <!-- <b-list-group
      v-show="intervalContextMenuShow"
      ref="intervalContextMenu"
      class="contextMenu"
      :style="{ top: topPosition + 'px', left:leftPosition + 'px' }"
    >
      <b-list-group-item
        v-if="showEvents"
        href="#"
        @click="addEventClick"
      >{{ $t("DataVisuGraphic.addEvent") }}</b-list-group-item>

    </b-list-group>-->

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

      <b-card-text v-if="event.start">{{ $t("component.common.begin") }}: {{event.start}}</b-card-text>
      <b-card-text v-else>{{ $t("component.common.begin") }}: {{event.end}}</b-card-text>
      <b-card-text v-if="event.start">{{ $t("component.common.end") }}: {{event.end}}</b-card-text>
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
import moment from "moment-timezone";
import Highcharts from "highcharts";

import ClickOutside from "vue-click-outside";
import exportingInit from "highcharts/modules/exporting";
import HighchartsCustomEvents from "highcharts-custom-events";

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
  $opensilex: any;

  @Ref("helpModal") readonly helpModal!: any;
  @Ref("contextMenu") readonly contextMenu!: any;
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
  variable;
  selectedPointsCount = 0;

  lineType = false;
  lineWidth = false;
  yAxis = {
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

  data: any = null;
  provenance: any = null;
  annotations = [];
  event: any = null;

  // created(){
  //   Highcharts.setOptions({
  //     time: {
  //       useUTC: false
  //     }
  //   });
  // }

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
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  getEventDetail(uri) {
    return this.$opensilex
        .getService("opensilex.EventsService")
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
        .getService("opensilex.AnnotationsService")
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
        .getService("opensilex.DataService")
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

  get chartOptions(): Array<HighchartsOptions> {
    const that = this;
    let previousPoint;
    let previousPointColor;
    if (this.series.length > 0) {
      return [
        {
          chart: {
            zoomType: "x",
            marginBottom: that.series.length > 8 ? 130 : 100,
            marginLeft: 80,
            height: that.series.length > 8 ? 500 : 400, //ok until 20 or 30 series depends on the name (uri) lenght....
            type: that.lineType ? "line" : "scatter",
            events: {
              click: function(e) {
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
          title: {
            text: ""
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
            sourceWidth: 800,
            sourceHeight: 500,
            scale: 2
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
            floating: true,
            verticalAlign: "bottom",
            align: "center"
          },
          xAxis: {
            type: "datetime",
            title: { text: "time" },
            min: this.startDate? moment(this.startDate).toDate().getTime(): undefined,
            max: this.endDate? moment(this.endDate).toDate().getTime(): undefined,
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
                let date = moment
                  .parseZone(point.data.date)
                  .format("DD-MM-YYYY HH:mm:ss");
                return (
                    "" +
                    this.point.series.name +
                    " :" +
                    '<span style=" color:' +
                    this.point.color +
                    '" ><b> ' +
                    this.point.y +
                    "</b></span>" +
                    "<br/>Time:<b> " +
                    date +
                    "</b> "
                );
              } else {
                return (
                  "" +
                  point.series.name +
                  " :" +
                  '<span style=" color:' +
                  point.color +
                  '" ><b> ' +
                  point.text +
                  "</b></span>" +
                  "<br/>Time:<b> " +
                  Highcharts.dateFormat("%d-%m-%Y %H:%M:%S", this.x) +
                  "</b> "
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
                radius: 3
              }
            },

            series: {
              turboThreshold: 100000,
              cursor: "pointer",
              dataGrouping: {
                enabled: true
              },
              lineWidth: this.lineWidth ? 2 : 0, //scatter plot
              stickyTracking: false, //tooltip only on hover
              marker: {
                //don't hide serie on hover
                states: {
                  hover: {
                    enabled: true
                  }
                }
              },
              states: {
                inactive: {
                  opacity: 1
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

  scatter() {
    this.lineType = !this.lineType;
    this.lineWidth = !this.lineWidth;
  }

  @Ref("highcharts") readonly highchartsRef!: any;

  closeContextMenu() {
    this.contextMenuShow = false;
  }
  // closeIntervalContextMenu() {
  //   this.intervalContextMenuShow = false;
  // }

  closeMenu() {
    this.closeContextMenu();
    //this.closeIntervalContextMenu();
  }

  reload(series: Array<Highcharts.SeriesLineOptions>, variable, form) {

    if(form) {
      this.startDate = form.startDate;
      this.endDate = form.endDate;
    }

    if(form && form.showEvents) {
      this.showEvents = form.showEvents;
    }

    this.detailDataShow = false;
    this.detailEventShow = false;

    this.variable = variable;
    if (series.length > 0) {
      this.yAxis = this.buildYAxis(this.showEvents);
    }
    this.series = series;
  }

  buildYAxis(isEvents) {
    let yAxis;

    if (isEvents) {
      yAxis = [
        {
          labels: {
            align: "right",
            x: -3
          },
          title: {
            text: this.variable
                ? this.variable.name + " (" + this.variable.unit.name + ")"
                : ""
          },
          height: "80%",
          lineWidth: 2,
          resize: {
            enabled: true
          }
        },
        {
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
        }
      ];
    } else {
      yAxis = {
        labels: {
          align: "right",
          x: -3
        },
        title: {
          text: this.variable
              ? this.variable.name + " (" + this.variable.unit.name + ")"
              : ""
        }
      };
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

      let momentDate = moment(this.selectedTime);
      this.selectedOffset = momentDate.format("Z");
      this.selectedTimeToSend = momentDate.format("YYYY-MM-DDTHH:mm:ssZ");
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

  // detailProvenanceClick() {
  //   this.contextMenuShow = false;
  //   let toSend = {
  //     data: this.selectedData,
  //     provenance: this.selectedProvenance
  //   };
  //  this.$emit("detailProvenanceIsClicked", toSend);
  // }

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
      this.highchartsRef[0].chart.exportChart({ type: "image/jpg" });
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
}
</script>

<style scoped lang="scss">
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
</i18n>
