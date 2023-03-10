<template>
  <div style="min-height:400px;">

    <div>
      <div class="card-body p-0">
        <highcharts
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
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
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
     },  
    }
};

HighchartsCustomEvents(Highcharts);
exportingInit(Highcharts);

@Component({
  directives: {
    ClickOutside
  }
})
export default class VisualisationGraphic extends Vue {
  $opensilex: any;

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

  series: Array<Highcharts.SeriesLineOptions> = [];
  chartOptionsValues: any = [];

  startDate;
  endDate;

  @Prop({
    default: false
  })
  deviceType;


  @Prop({
    default: false
  })
  lType;

  @Prop({
    default: false
  })
  lWidth;

  data: any = null;

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


  get chartOptions() {
    const that = this;
    let previousPoint;
    let previousPointColor;
    if (this.series.length > 0) {
      return [
        {
          chart: {
            zoomType: "x",
            marginLeft: 10,
            height: this.resizeHeight(),
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
            floating: false,
            verticalAlign: "bottom",
            align: "center",
            y: 0
            
          },
          xAxis: {
            type: "datetime",
            title: { text: "Time" },
            min: this.startDate? new Date(this.startDate).getTime(): undefined,
            max: this.endDate? new Date(this.endDate).getTime(): undefined,
            labels: {
              formatter: function() {
                return Highcharts.dateFormat("%e-%b-%Y %H:%M", this.value);
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
              const point = this.point as Highcharts.Point & {
                data: any,
                text: any
              };
              if (point.y) {

                let date = Highcharts.dateFormat("%d-%m-%Y %H:%M:%S", this.x)

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
                  opacity: 0.4
                }
              },
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


  reload(series: Array<Highcharts.SeriesLineOptions>, variable, form) {

    this.variable = variable;
    if (series.length > 0) {
      this.yAxis = this.buildYAxis();
    }
    this.series = series;
  }

  buildYAxis() {
    let yAxis;

      yAxis = {
        labels: {
          align: "right",
          x: -3
        },
        title: {
          text: this.variable
              ? this.variable.name + " (" + this.variable.unit.name + ")"
              : "",
          style: {
            fontWeight: 'bold',
            color: Highcharts.getOptions().colors[1],
          }
        }
      };
    return yAxis;
  }

  fullscreen() {
    this.highchartsRef[0].chart.fullscreen.toggle();
  }

  exportPNG() {
    if (this.selectedPointsCount < 1500) {
      this.highchartsRef[0].chart.exportChart({
        type: "image/png",
        filename: new Date().toISOString()
        })
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

      resizeHeight(){
        // many series and more than 1700 
        if(this.series.length >= 8 && window.innerWidth >1700){
          return 580
        }
        //many series and between 1400 et 1700
        if(this.series.length >= 8 && window.innerWidth >= 1400 && window.innerWidth <= 1700){
          return 400
        }
        //many series and less than 1400
        if(this.series.length >= 8 && window.innerWidth < 1400){
          return 300
        }
        // few series and more than 1700 :
        if(this.series.length < 8 && window.innerWidth > 1700){
          return 580
        }

        // few series and between 1400 et 1700
        if(this.series.length < 8 && window.innerWidth >= 1400 && window.innerWidth <= 1700){
          return 350
        }
        // few series and less than 1400
        else {
          return 300
        }
      }

    resizeLegendMargin () {
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
            return 150
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
}
</script>

<style scoped lang="scss">
</style>

<i18n>
fr:
  VisualisationGraphic:
    fullscreen : Plein ecran
    download : Télecharger l'image
    time : Time

en:
  VisualisationGraphic:  
    fullscreen : Fullscreen
    download : Download image
    time : Temps
</i18n>
