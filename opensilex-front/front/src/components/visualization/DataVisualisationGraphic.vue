<template>
  <div style="min-height:300px;">
    
    <!-- Context menu on data right click-->
    <b-list-group
      v-show="contextMenuShow"
      ref="contextMenu"
      class="contextMenu"
      :style="{ top: topPosition + 'px', left:leftPosition + 'px' }"
    >
      <b-list-group-item
       href="#" @click="addEventClick">{{ $t("DataVisualisationGraphic.addEvent") }}
      </b-list-group-item>

      <b-list-group-item
        href="#"
        @click="dataAnnotationClick"
      >{{ $t("DataVisualisationGraphic.dataAnnotation") }}
      </b-list-group-item>
    </b-list-group>

    <div class="card">
      <!--Graphic options start-->
      <div ref="header" class="card-header" v-if="chartOptions.length">

        <opensilex-HelpButton
          :label="helpText"
          @click="helpModal.show()"
          class="dataVisualisationGraphicHelpButton"
        ></opensilex-HelpButton>

        <div class="card-header-right mr-4">
          <b-dropdown right size="lg" variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content>
              <opensilex-Icon icon="fa#bars" size="lg" />
              <span class="sr-only">Search</span>
            </template>

            <b-dropdown-item v-if="this.lineType" href="#" @click="scatter">
              <opensilex-Icon icon="fa#braille" />
              {{ $t("DataVisualisationGraphic.scatterPlotView") }}
            </b-dropdown-item>
            <b-dropdown-item v-else href="#" @click="scatter">
              <opensilex-Icon icon="fa#chart-line" />
              {{ $t("DataVisualisationGraphic.chartLineView") }}
            </b-dropdown-item>

            <b-dropdown-item href="#" @click="fullscreen">
              <opensilex-Icon icon="fa#expand" />
              {{ $t("DataVisualisationGraphic.fullscreen") }}
            </b-dropdown-item>

            <b-dropdown-item href="#" @click="exportPNG">
              <opensilex-Icon icon="fa#download" />
              {{ $t("DataVisualisationGraphic.download") }}
            </b-dropdown-item>
          </b-dropdown>
        </div>
      </div>
      <!--Graphic options end-->

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

    <!-- Data informations on click on graphic point -Begin- -->
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
    <!-- Data informations on click on graphic point End-->

    <b-modal size="lg" ref="helpModal" scrollable centered>
      <opensilex-DataVisuHelp></opensilex-DataVisuHelp>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import {
  DataService,
  ProvenanceGetDTO,
  AnnotationsService,
  AnnotationGetDTO,
  EventsService,
  EventDetailsDTO
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import moment from "moment-timezone";
import Highcharts from "highcharts";
import ClickOutside from "vue-click-outside";
import exportingInit from "highcharts/modules/exporting";
import HighchartsCustomEvents from "highcharts-custom-events";
import { start } from "@popperjs/core";
HighchartsCustomEvents(Highcharts);
exportingInit(Highcharts);

@Component({
  directives: {
    ClickOutside
  }
})
export default class DataVisualisationGraphic extends Vue {
  $opensilex: any;

  @Ref("helpModal") readonly helpModal!: any;
  @Ref("contextMenu") readonly contextMenu!: any;
  @Ref("experimentDataVisuForm") readonly experimentDataVisuForm!: any;
  // @Ref("selectedVariable") readonly ExperimentDataVisuView!: any;
  contextMenuShow = false;
  // intervalContextMenuShow = false;
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
  selectedValue;
  selectedOffset;
  variable;
  selectedPointsCount = 0;

  lineType = false;
  lineWidth = false;
  yAxis;

  @Prop()
  showEvents : boolean;

  series = [];
  private concernedItem;
  chartOptionsValues: any = [];

  get helpText() {
    return this.showEvents ? this.$i18n.t("DataVisualisationGraphic.rightClick") :  ""
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
  startDate;

  @Prop()
  endDate;

  data: any = null;
  provenance: any = null;
  annotations = [];
  event: any = null;
  getStartDate = null;
  getEndDate = null;

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
    this.getStartDate = this.startDate; // getStartDate recupere la valeur
    this.getEndDate = this.endDate;
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

  get chartOptions() {
    let that = this;
    let previousPoint;
    if (this.series.length > 0) {
      return [
        {
          chart: {
            zoomType: "x",
            marginLeft: 80,
            height: that.series.length > 8 ? 650 : 500,
            type: that.lineType ? "line" : "scatter",
            events: {
              click: function(e) {
                let chart = that.highchartsRef[0].chart;
                chart.tooltip.hide(0);
              },
              render: function() {
                that.selectedPointsCount = 0;
                this.series.forEach(element => {
                  if (element.points && element.name !== "Navigator 1") {
                    that.selectedPointsCount += element.points.length;
                  }
                });
                this.detailEventShow = false;
                this.detailDataShow = false;
                that.$emit("graphicCreated");
              }
            }
          },
          title: {
            text: ""
          },
          labels: {
            format: ""
          },
          subTitle: {
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
            enabled: this.deviceType ? true : false,
            margin: 5,
            y: -4
          },
          legend: {
            enabled: true,
            floating: false,
            verticalAlign: "bottom",
            align: "center",
          },

          xAxis: {
            type: "datetime",
            title: { text: "time" },
            min: this.getStartDate? moment(this.getStartDate).toDate().getTime(): undefined,
            max: this.getEndDate? moment(this.getEndDate).toDate().getTime(): undefined,
            labels: {
              formatter: function() {
                return Highcharts.dateFormat("%e-%b-%Y %H:%M:%S", this.value);
              }
            },
            marginBottom: this.resizeLegendMargin(),
            ordinal: false,
            crosshair: true,
            showLastLabel: true,
            endOnTick: true
          },
          yAxis: this.yAxis,

          tooltip: {
            useHTML: true,
            formatter: function(tooltip) {
              if (this.point.y) {
                let date = moment
                  .parseZone(this.point.data.date)
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
                  this.point.series.name +
                  " :" +
                  '<span style=" color:' +
                  this.point.color +
                  '" ><b> ' +
                  this.point.text +
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
                        color: previousPoint.originalColor
                      });
                    // Set this points color to black
                    this.update({ color: "black", originalColor: this.color });
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

  closeMenu() {
    this.closeContextMenu();
    //this.closeIntervalContextMenu();
  }

  reload(series, variable, form) {
    
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

      // Build YAxis if Events are wanted ( one or multiple variables)
    if (this.showEvents) {
      yAxis = [
        // AXE VAR 1
       {
        labels: {
          x: -3,
          format: "{value}"
        },
        title: {
          text: this.variable[0]
            ? this.variable[0].name + " (" + this.variable[0].unit.name + ")"
            : "",
          style: {
            color: Highcharts.getOptions().colors[1],
            fontWeight: 'bold',
            fontSize: window.innerWidth < 1200
              ? 12
              : 13
          }
        },
        height: "80%",
        offset: 30,
        opposite: true
        },
        // AXE VAR 2
        {
          labels: {
            align: "right",
            x: -3
          },
           title: {
            text: this.variable[1]
              ? this.variable[1].name + " (" + this.variable[1].unit.name + ")"
              : "",
          style: {
            color: Highcharts.getOptions().colors[1],
            fontWeight: 'bold',
            fontSize: window.innerWidth < 1200
              ? 12
              : 13
            }
          },        
          height: "80%",
          lineWidth: 2,
          opposite: false,
          offset: 0,
          linkedTo:0 // second graduation
        },
   
        // AXE EVENTS
        {
          labels: {
            align: "right",
            x: -3,
          },
          title: {
            text: "Events"
          },
          style: {
            color: Highcharts.getOptions().colors[1],
            fontWeight: 'bold',
            fontSize: window.innerWidth < 1200
              ? 12
              : 13
          },
          top: "85%",
          height: "15%",
          offset: 0,
          lineWidth: 2,
        }
      ];
    } else {
      // Build YAxis if Events are not wanted ( one or multiple var)
      yAxis = [{
        // Primary yAxis
        labels: {
          x: -3,
          format: "{value}"
        },
        title: {
          text: this.variable[0]
            ? this.variable[0].name + " (" + this.variable[0].unit.name + ")"
            : "",
          style: {
            color: Highcharts.getOptions().colors[1],
            fontWeight: 'bold',
            fontSize: window.innerWidth < 1200
              ? 12
              : 13
          }
        },
        offset: 30,
        opposite: true
      },
      // Secondary yAxis
      {
        title: {
          text: this.variable[1]
            ? this.variable[1].name + " (" + this.variable[1].unit.name + ")"
            : "",
          style: {
            color: Highcharts.getOptions().colors[1],
            fontWeight: 'bold',
            fontSize: window.innerWidth < 1200
              ? 12
              : 13
          },
        },
          labels: {
            x: 10,
            format: "{value}"
          },
        offset: 30,
        opposite: false,
        linkedTo:0 // second graduation
      }];
    }   
  return yAxis;
}

  // on right click on datapoint from a graphic serie
  pointRightClick(e, graphic) {
    this.closeMenu();
    let chart = graphic.series.chart;
    chart.tooltip.hide();

      // show context menu
    if (e.point.data && graphic.series) {
      this.contextMenuShow = true;
      let chartWidth = this.highchartsRef[0].chart.chartWidth;

      this.$nextTick(() => {
        // wait to have the contextMenu Width
        let menuWidth = this.contextMenu.clientWidth;

        // define X/Y position for context menu,
        // if possible, place it before the point, otherwise if exceeds graphic limit border place it after the point
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

      this.selectedOffset = e.point.offset;
      this.selectedTime = e.point.data.date;
      this.selectedTimeToSend = e.point.dateWithOffset;
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

  detailProvenanceClick() {
    this.contextMenuShow = false;
    let toSend = {
      data: this.selectedData,
      provenance: this.selectedProvenance
    };
   this.$emit("detailProvenanceIsClicked", toSend);
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
      this.highchartsRef[0].chart.exportChart({ type: "image/jpg" });
    } else {
      this.$opensilex.showInfoToast(
        "Too much points : " +
          this.selectedPointsCount +
          " Must be < 1500 points"
      );
    }
  }

  // resize space between graphic and legends
  resizeLegendMargin (){
     //  2 elements
    if (this.series.length <= 2) {
      return 100
    } 
    //   3 to 4
    else if (this.series.length > 2 && this.series.length <= 4) {
      if (window.innerWidth <= 1200){
        //small
        return 120
      }
      else {
        //large
        return 100
      }
    }
    //  5 to 8
    else if (this.series.length > 4 && this.series.length <= 8) {
      if (window.innerWidth <= 1030){
        //mobile
        return 210
      }
      else if (window.innerWidth >1030 && window.innerWidth <= 1285){
        //small
        return 160
      }
      else if (window.innerWidth <= 1700 && window.innerWidth > 1285) {
        //medium
        return 140
      }
      else {
        //large
        return 110
      }
    }
    //  9 to 12
    else if (this.series.length > 8 && this.series.length <= 12) {
      if (window.innerWidth <= 1030){
        //mobile
        return 240
      }
      else if (window.innerWidth >1030 && window.innerWidth <= 1285){
        //small
        return 190
      }
      else if (window.innerWidth <= 1700 && window.innerWidth > 1285) {
        //medium
        return 160
      }
      else {
        //large
        return 130
      }
    }
    // 13 to 16
    else if (this.series.length > 12 && this.series.length <= 16) {
      if (window.innerWidth <= 1015){
        //small
        return 300
      }
      else if (window.innerWidth <= 1450 && window.innerWidth > 1015) {
        //medium
        return 220
      }
      else {
        //large
        return 150
      }
    }
    //  17 to 20
    else if (this.series.length > 16 && this.series.length <= 20) {
      if (window.innerWidth <= 1015){
        //small
        return 300
      }
      else if (window.innerWidth <= 1450 && window.innerWidth > 1015) {
        //medium
        return 220
      }
      else {
        //large
        return 170
      }
    }
    //  21 to 24
    else if (this.series.length > 20 && this.series.length <= 24) {
      if (window.innerWidth <= 1015){
        //small
        return 300
      }
      else if (window.innerWidth <= 1450 && window.innerWidth > 1015) {
        //medium
        return 240
      }
      else {
        //large
        return 190
      }
    }
    // more than 25
    else {
      if (window.innerWidth <= 1450) {
        //small
        return 300
      }
      else {
      //large
      return 210
      }
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
}
</script>

<style scoped lang="scss">
.card-header {
  height: 60px;
  border-bottom: none;
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

.dataVisualisationGraphicHelpButton {
  color: #00A38D;
  border:none;
  margin-bottom: 5px;
}

.dataVisualisationGraphicHelpButton:hover{
  background-color: #00A38D;
  border-color: #00A38D;
  color: #f1f1f1
}

</style>


<i18n>
  fr: 
    DataVisualisationGraphic:
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
    DataVisualisationGraphic:
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
