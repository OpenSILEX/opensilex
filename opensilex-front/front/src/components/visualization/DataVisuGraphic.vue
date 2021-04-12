<template>
  <div style="min-height:300px;">
    <b-list-group
      v-if="contextMenuShow"
      class="contextMenu"
      :style="{ top: topPosition + 'px', left:leftPosition + 'px' }"
    >
      <b-list-group-item
        href="#"
        @click="detailProvenanceClick"
      >{{ $t("DataVisuGraphic.provenanceDetail") }}</b-list-group-item>
      <!-- <b-list-group-item href="#">{{ $t("DataVisuGraphic.dataAnnotation") }}</b-list-group-item>
      <b-list-group-item href="#">{{ $t("DataVisuGraphic.scientificObjectAnnotation") }}</b-list-group-item>-->
    </b-list-group>

    <div class="card">
      <div ref="header" class="card-header" v-if="chartOptions.length">
        <opensilex-HelpButton label="component.common.help-button" @click="helpModal.show()"></opensilex-HelpButton>
        <div class="card-header-right mr-4">
          <b-dropdown right size="lg" variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content>
              <opensilex-Icon icon="fa#sliders-h" />
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

      <div v-click-outside="closeContextMenu" @click="closeContextMenu" class="card-body p-0">
        <highcharts
          v-for="(options, index) in chartOptions"
          :options="options"
          v-bind:key="index"
          :constructor-type="'stockChart'"
          ref="highcharts"
        ></highcharts>
      </div>
    </div>

    <b-modal size="lg" ref="helpModal" scrollable centered>
      <opensilex-DataVisuHelp></opensilex-DataVisuHelp>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import moment from "moment-timezone";
import Highcharts from "highcharts";

import ClickOutside from "vue-click-outside";
import exportingInit from "highcharts/modules/exporting";
import HighchartsCustomEvents from "highcharts-custom-events";
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
  contextMenuShow = false;
  topPosition;
  leftPosition;
  selectedData;
  selectedObject;
  selectedProvenance;
  selectedTime;
  selectedDate;
  selectedValue;
  selectedOffset;
  variable;
  selectedPointsCount = 0;

  lineType = true;
  lineWidth = true;
  yAxis = {
    title: {
      text: "test"
    }
  };
  series = [];
  private concernedItem;
  chartOptionsValues: any = [];

  @Prop({
    default: false
  })
  deviceType;

  get chartOptions() {
    let that = this;
    if (this.series.length > 0) {
      return [
        {
          chart: {
            zoomType: "x",
            type: that.lineType ? "line" : "scatter",
            events: {
              click: function(e) {
                var chart = that.highchartsRef[0].chart;
                chart.tooltip.hide(0);
              },
              render: function() {
                that.selectedPointsCount = 0;
                this.series.forEach(element => {
                  // limit download image
                  if (element.points && element.name !== "Navigator 1") {
                    that.selectedPointsCount += element.points.length;
                  }
                });
                that.$emit("graphicCreated");
              }
            }
          },
          title: {
            text: ""
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
            enabled:   false,
         
          },
          navigator: {
            //zoom navigator
            enabled:true,
            margin: 5,
            y: -4
          },
          legend: {
            layout: "vertical",
            enabled: true
          },
          xAxis: {
            type: "datetime",
            title: { text: "time" },
            tickInterval:3600 * 1000,
            // labels: {
            //   formatter: function() {
            //     return Highcharts.dateFormat("%d/%m", this.value);
            //   }
            // },
            ordinal: false,
            crosshair: true,
            showLastLabel: true,
            endOnTick: true
          },
          yAxis: this.yAxis,

          tooltip: {
            useHTML: true,
            formatter: function(tooltip) {
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
                Highcharts.dateFormat("%Y-%m-%d %H:%M:%S", this.x) +
                "</b> "
              );

              //   if (this.points) {
              //     return this.points.reduce(function(s, point) {
              //       return (
              //         s +
              //         '<br/><span style=" color:' +
              //         point.color +
              //         '" > ' +
              //         point.series.name +
              //         ": " +
              //         point.y +
              //         "</span>"
              //       );
              //     }, "<b>" +
              //       Highcharts.dateFormat("%Y-%m-%d %H:%M:%S", this.x) +
              //       "</b>");
              //   } else {
              //     console.log("this");
              //     console.log(this);
              //     return (
              //       Highcharts.dateFormat("%Y-%m-%d %H:%M:%S", this.x) +
              //       "</b><br/>" +
              //       '<span style=" color:' +
              //       this.point.color +
              //       '" > ' +
              //       this.point.series.name +
              //       ": " +
              //       this.point.y +
              //       "</span>" +
              //       "<b>"
              //     );
              //     // return tooltip.defaultFormatter.call(this, tooltip);
              //   }
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
                  },
                  radius: 2
                }
              },
              states: {
                inactive: {
                  opacity: 1
                }
              },
              events: {
                click: function(e) {
                  // that.rightClick(e, this);
                }
              },
              point: {
                events: {
                  click: function(e) {
                    e.stopPropagation();
                    that.rightClick(e, this);
                  },
                  contextmenu: function() {
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

  created() {
    this.InitHCTheme();
  }

  closeContextMenu() {
    if (this.contextMenuShow) {
      this.contextMenuShow = false;
    }
  }

  reload(series, variable) {
    this.variable = variable;
    this.series = series;
    if (series.length > 0) {
      this.yAxis = this.buildYAxis();
    }
  }

  buildYAxis() {
    let yAxis;
    yAxis = {
      labels: {
        align: "right",
        x: -3,
        events: {
          click: function() {
          }
        }
      },
      title: {
        text: this.variable.name + " (" + this.variable.unit.name + ")"
      }
    };
    return yAxis;
  }

  flagsRightClick(e) {
    if (this.contextMenuShow) {
      this.contextMenuShow = false;
    }
  }

  rightClick(e, graphic) {
    if (this.contextMenuShow) {
      this.contextMenuShow = false;
    }
    if (e.point.data && graphic.series) {
      var chart = graphic.series.chart;
      let chartWidth = this.highchartsRef[0].chart.chartWidth;
      if (e.pageX + 300 > chartWidth) {
        this.leftPosition = e.pageX - 295;
      } else {
        this.leftPosition = e.pageX + 5;
      }
      this.topPosition = e.pageY;
      this.contextMenuShow = true;
      this.selectedValue = e.point.y;
      this.selectedObject = e.point.objectUri;
      this.selectedProvenance = e.point.provenanceUri;
      this.selectedData = e.point.data;
      this.selectedOffset = e.point.offset;
      // this.selectedTime = chart.xAxis[0].toValue(e.chartX, false);
    }
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

  InitHCTheme() {
    const themeffx = {
      chart: {
        plotBackgroundColor: "#EBEBEB",
        style: {
          color: "#000000",
          fontFamily: "Arial, sans-serif"
        }
      },
      colors: [
        "#00a38d",
        "#a32300",
        "#a37500",
        "#4700a3",
        "#00b0f6",
        "#e76bf3",
        "#595959",
        "#f2dc7c"
      ],
      xAxis: {
        labels: {
          style: {
            color: "#666666"
          }
        },
        title: {
          style: {
            color: "#000000"
          }
        },
        startOnTick: false,
        endOnTick: false,
        gridLineColor: "#FFFFFF",
        gridLineWidth: 1.5,
        tickWidth: 1.5,
        tickLength: 5,
        tickColor: "#666666",
        minorTickInterval: 0,
        minorGridLineColor: "#FFFFFF",
        minorGridLineWidth: 0.5
      },
      yAxis: {
        labels: {
          style: {
            color: "#666666"
          }
        },
        title: {
          style: {
            color: "#000000"
          }
        },
        startOnTick: false,
        endOnTick: false,
        gridLineColor: "#FFFFFF",
        gridLineWidth: 1.5,
        tickWidth: 1.5,
        tickLength: 5,
        tickColor: "#666666",
        minorTickInterval: 0,
        minorGridLineColor: "#FFFFFF",
        minorGridLineWidth: 0.5
      },
      legendBackgroundColor: "rgba(0, 0, 0, 0.5)",
      background2: "#505053",
      dataLabelsColor: "#B0B0B3",
      textColor: "#C0C0C0",
      contrastTextColor: "#F0F0F3",
      maskColor: "rgba(255,255,255,0.3)"
    };

    Highcharts.theme = themeffx;
    // Apply the theme
    Highcharts.setOptions(Highcharts.theme);
    Highcharts.setOptions({
      time: {
        useUTC: true
      }
    });
  }
}
</script>

<style scoped lang="scss">
.card-header {
  height: 60px;
}
.contextMenu {
  position: absolute;
  z-index: 1001;
  width: 300px;
}
</style>

<i18n>
  fr: 
    DataVisuGraphic:
      provenanceDetail : Details de la provenance
      dataAnnotation : Ajouter une annotation sur la donnée (V2)
      scientificObjectAnnotation : Ajouter une annotation à l' objet scientifique (V2)
      scatterPlotView : Mode nuage de points
      chartLineView : Mode courbe
      fullscreen : Plein ecran
      download : Télecharger l'image

  en: 
    DataVisuGraphic:
      provenanceDetail : Provenance detail
      dataAnnotation : Add data's annotation (V2)
      scientificObjectAnnotation : Add scientific object's annotation (V2)
      scatterPlotView : Scatter plot view
      chartLineView : Chart line view
      fullscreen : Fullscreen
      download : Download image
</i18n>