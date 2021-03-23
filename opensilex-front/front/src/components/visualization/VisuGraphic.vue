<template>
  <div>
    <b-card
      bg-variant="secondary"
      text-variant="white"
      class="contextMenu"
      v-if="contextMenuShow"
      :style="{ top: topPosition + 'px', left:leftPosition + 'px' }"
    >
      <p>Prov:{{selectedProvenance}}</p>
      <b-list-group>
        <b-list-group-item href="#" @click="detailProvenanceClick">Details</b-list-group-item>
        <b-list-group-item href="#">Add annotation/confidence on data</b-list-group-item>
        <b-list-group-item href="#" @click="eventCreateClick">Add event on Scientific object</b-list-group-item>
        <b-list-group-item
          href="#"
          @click="annotationCreateClick"
        >Add annotation on Scientific object</b-list-group-item>
      </b-list-group>
    </b-card>

    <div class="card">
      <div ref="header" class="card-header">
        <div class="properties-dropdown" @click="$emit('search')">
          <a
            class="dropdown-toggle btn btn-icon btn-primary"
            href="#"
            id="propertiesDropdown"
            role="button"
            data-toggle="dropdown"
            aria-haspopup="true"
            aria-expanded="false"
          >
            <opensilex-Icon icon="fa#bars" />
          </a>
        </div>
        <div class="card-header-right mr-4">
          <b-dropdown right size="lg" variant="link" toggle-class="text-decoration-none" no-caret>
            <template #button-content>
              <opensilex-Icon icon="fa#ellipsis-v" />
              <span class="sr-only">Search</span>
            </template>
            <b-dropdown-item href="#" @click="fullscreen">
              <opensilex-Icon icon="fa#expand" />Fullscreen
            </b-dropdown-item>
            <b-dropdown-item href="#" @click="exportPNG">
              <opensilex-Icon icon="fa#download" />Download image
            </b-dropdown-item>
          </b-dropdown>
        </div>
      </div>

      <div class="card-body p-0">
        <div v-if="!chartOptions.length">No result</div>
        <highcharts
          style="max-height:500px;"
          v-else
          v-for="(options, index) in chartOptions"
          :options="options"
          v-bind:key="index"
          :constructor-type="'stockChart'"
          ref="highcharts"
        ></highcharts>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref, Prop } from "vue-property-decorator";
import Vue from "vue";
import moment from "moment-timezone";
import Highcharts from "highcharts";

import exportingInit from "highcharts/modules/exporting";
//import offlineExporting from "highcharts/modules/offline-exporting";
import HighchartsCustomEvents from "highcharts-custom-events";
HighchartsCustomEvents(Highcharts);
exportingInit(Highcharts);
//offlineExporting(Highcharts);

@Component
export default class VisuGraphic extends Vue {
  $opensilex: any;

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
  variables;
  selectedPointsCount = 0;
  @Prop()
  showEvents: boolean;

  private concernedItem;
  chartOptionsValues: any = [];
  get chartOptions() {
    return this.chartOptionsValues;
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

  reload(series, variables, isMultipleVariable) {
    if (!this.showEvents) {
      this.chartOptionsValues.forEach(element => {
        element.yAxis = {
          title: {
            text: "test"
          }
        };
      });
    }
    this.variables = variables;
    let chartOptionsValue = this.buildGraphic(series, isMultipleVariable);
    this.chartOptionsValues.push(chartOptionsValue);
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
    if (e.point.dataUri && graphic.series) {
      var chart = graphic.series.chart;
      let chartWidth = this.highchartsRef[0].chart.chartWidth;
      if (e.pageX + 300 > chartWidth) {
        this.leftPosition = e.pageX - 300;
      } else {
        this.leftPosition = e.pageX;
      }
      this.topPosition = e.pageY;
      this.contextMenuShow = true;
      this.selectedValue = e.point.y;
      this.selectedObject = e.point.objectUri;
      this.selectedProvenance = e.point.provenanceUri;
      this.selectedData = e.point.dataUri;
      this.selectedOffset = e.point.offset;
      // this.selectedTime = chart.xAxis[0].toValue(e.chartX, false);
    }
  }

  fullscreen() {
    this.highchartsRef[0].chart.fullscreen.toggle();
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

  buildGraphic(value, isMultipleVariable) {
    let that = this;
    let yAxis = this.buildYAxis(isMultipleVariable);

    let chartOptionValue = {
      chart: {
        zoomType: "x",
        events: {
          click: function(e) {
            var chart = that.highchartsRef[0].chart;
            chart.tooltip.hide(0);
          },
          render: function() {
            that.selectedPointsCount = 0;
            this.series.forEach(element => {
              if (element.points && element.name !== "Navigator 1") {
                that.selectedPointsCount += element.points.length;
              }
            });
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
          enabled: false
        }
      },
      exporting: {
        sourceWidth: 800,
        sourceHeight: 500,
        scale: 2
      },
      rangeSelector: {
        enabled: false
      },
      navigator: { enabled: true, margin: 5, y: -4 },
      legend: { layout: "vertical", enabled: true },
      xAxis: {
        type: "datetime",
        title: { text: "time" },
        ordinal: false,
        crosshair: true,
        showLastLabel: true,
        endOnTick: true
      },
      yAxis: yAxis,
      tooltip: {
        shared: true,
        split: true,
        formatter: function() {
          let that: any = this;
          let points = that.points,
            tooltipArray = [
              "<b>" +
                Highcharts.dateFormat("%Y-%m-%d %H:%M:%S", that.x) +
                "</b>"
            ]; //bottom tooltip

          if (points) {
            //data points
            points.forEach(function(point, index) {
              tooltipArray.push(
                point.point.provenanceUri + ": <b>" + point.y + "</b>"
              );
            });

            return tooltipArray;
          } else {
            //provenance point
            let point = that.point;
            if (point && point.eventUri) {
              return (
                '<span style=" background-color:' +
                that.point.fillColor +
                '" > ' +
                that.point.title +
                "</span>"
              );
            } else {
              return "";
            }
          }
        }
      },
      series: value,
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
            enabled: false
          },
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
              }
            }
          }
        }
      }
    };

    return chartOptionValue;
  }

  buildYAxis(isMultipleVariable) {
    let yAxis;
    if (isMultipleVariable) {
      if (this.showEvents) {
        yAxis = [
          {
            labels: {
              align: "right",
              x: -3
            },
            title: {
              text: this.variables[0].name
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
              text: this.variables[1].name
            },
            height: "80%",
            lineWidth: 2,
            resize: {
              enabled: true
            },
            opposite: false
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
        yAxis = [
          {
            labels: {
              align: "right",
              x: -3
            },
            title: {
              text: this.variables[0].name
            },
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
              text: this.variables[1].name
            },
            lineWidth: 2,
            resize: {
              enabled: true
            },
            opposite: false
          }
        ];
      }
    } else {
      if (this.showEvents) {
        yAxis = [
          {
            labels: {
              align: "right",
              x: -3
            },
            title: {
              text: this.variables[0].name
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
            text: this.variables[0].name
          }
        };
      }
    }

    return yAxis;
  }

  eventCreateClick() {
    this.contextMenuShow = false;

    this.$emit("eventCreate", {
      concernedItem: this.selectedObject,
      type: undefined,
      date: moment
        .tz(this.selectedTime, this.selectedOffset)
        .format("YYYY-MM-DD"),
      time: moment.tz(this.selectedTime, this.selectedOffset).format("HH:mm"),
      utc: moment.tz(this.selectedTime, this.selectedOffset).format("Z"),
      creator: [],
      properties: [],
      description: undefined
    });
  }

  annotationCreateClick() {
    this.contextMenuShow = false;

    this.$emit("annotationCreate", {
      concernedItem: this.selectedObject,
      properties: [],
      description: undefined
    });
  }

  detailProvenanceClick() {
    this.contextMenuShow = false;
    this.$emit("detailProvenanceIsClicked", this.selectedProvenance);
  }

  beforeCreate() {}

  cleanData(data) {
    const toReturn = [];
    let toAdd;
    data.forEach(element => {
      toAdd = [moment.utc(element.date).valueOf(), element.value];
      toReturn.push(toAdd);
    });
    return toReturn;
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

  InitHCTheme() {
    const themeffx = {
      chart: {
        plotBackgroundColor: "#EBEBEB",
        style: {
          color: "#000000",
          fontFamily: "Arial, sans-serif"
        }
      },
      /*   colors: [
        "#595959",
        "#F8766D",
        "#A3A500",
        "#00BF7D",
        "#00B0F6",
        "#E76BF3"
      ], */
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
.container {
  height: 400px;
}
.card-header {
  height: 60px;
}
.contextMenu {
  position: absolute;
  z-index: 1001;
  width: 300px;
}
</style>
