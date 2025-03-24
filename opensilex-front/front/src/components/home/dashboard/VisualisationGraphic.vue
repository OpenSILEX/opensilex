<template>
  <div style="min-height:400px;">
    <div>
      <div class="card-body p-0">
        visu graphique
        <!-- <highcharts
          v-for="(options, index) in chartOptions"
          :options="options"
          :key="index"
          :constructor-type="'stockChart'"
          ref="highcharts"
        ></highcharts> -->
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, watch, onMounted, onBeforeUnmount, computed } from "vue";
// import Highcharts from "highcharts";
// import ClickOutside from "vue-click-outside";
// import exportingInit from "highcharts/modules/exporting";
// import HighchartsCustomEvents from "highcharts-custom-events";

// type HighchartsOptions = Highcharts.Options & {
//   plotOptions: {
//     series: any;
//   };
// };

// HighchartsCustomEvents(Highcharts);
// exportingInit(Highcharts);

export default defineComponent({
  // directives: {
  //   ClickOutside,
  // },
  props: {
    deviceType: {
      type: Boolean,
      default: false,
    },
    lType: {
      type: Boolean,
      default: false,
    },
    lWidth: {
      type: Boolean,
      default: false,
    },
  },
  setup(props) {
    // const highchartsRef = ref<Highcharts.Chart | null>(null);
    const chartOptionsValues = ref<any>([]);
    // const series = ref<Highcharts.SeriesLineOptions[]>([]);
    const lineType = ref(props.lType);
    const lineWidth = ref(props.lWidth);
    const selectedPointsCount = ref(0);
    const yAxis = ref({
      title: {
        text: "",
      },
    });

    const langUnwatcher = ref<any>(null);

    // const chartOptions = computed(() => {
    //   if (series.value.length > 0) {
    //     return [
    //       {
    //         chart: {
    //           zoomType: "x",
    //           marginLeft: 10,
    //           height: resizeHeight(),
    //           type: lineType.value ? "line" : "scatter",
    //           events: {
    //             click: (e) => {
    //               let chart = highchartsRef.value?.chart;
    //               chart?.tooltip.hide(0);
    //             },
    //             render: () => {
    //               selectedPointsCount.value = 0;
    //               series.value.forEach((serie) => {
    //                 if (serie.data && serie.name !== "Navigator 1") {
    //                   selectedPointsCount.value += serie.data.length;
    //                 }
    //               });
    //             },
    //           },
    //         },
    //         title: { text: "" },
    //         subtitle: { text: "" },
    //         credits: { enabled: false },
    //         navigation: {
    //           buttonOptions: { enabled: false },
    //         },
    //         exporting: {
    //           sourceWidth: 800,
    //           sourceHeight: 500,
    //           scale: 2,
    //         },
    //         scrollbar: { enabled: false },
    //         rangeSelector: { enabled: false },
    //         navigator: { enabled: !!props.deviceType, margin: 5 },
    //         legend: {
    //           enabled: true,
    //           floating: false,
    //           verticalAlign: "bottom",
    //           align: "center",
    //           y: 0,
    //         },
    //         xAxis: {
    //           type: "datetime",
    //           title: { text: "Time" },
    //           min: undefined, // Add your dynamic range logic here
    //           max: undefined,
    //           labels: {
    //             formatter() {
    //               return Highcharts.dateFormat("%e-%b-%Y %H:%M", this.value);
    //             },
    //           },
    //           marginBottom: resizeLegendMargin(),
    //           ordinal: false,
    //           crosshair: true,
    //           showLastLabel: true,
    //           endOnTick: true,
    //         },
    //         yAxis: yAxis.value,
    //         tooltip: {
    //           useHTML: true,
    //           formatter() {
    //             const point = this.point as Highcharts.Point & {
    //               data: any;
    //               text: any;
    //             };
    //             let date = Highcharts.dateFormat("%d-%m-%Y %H:%M:%S", this.x);
    //             return `${this.point.series.name} : <span style="color:${this.point.color}"><b>${this.point.y}</b></span><br/>Time:<b>${date}</b>`;
    //           },
    //           split: false,
    //           shared: false,
    //         },
    //         series: series.value,
    //         plotOptions: {
    //           line: {
    //             marker: {
    //               enabled: true,
    //               symbol: "circle",
    //               radius: 3,
    //             },
    //           },
    //           series: {
    //             turboThreshold: 100000,
    //             cursor: "pointer",
    //             dataGrouping: {
    //               enabled: true,
    //             },
    //             lineWidth: lineWidth.value ? 2 : 0,
    //             stickyTracking: false,
    //             marker: {
    //               states: {
    //                 hover: { enabled: true },
    //               },
    //             },
    //             states: {
    //               inactive: { opacity: 0.4 },
    //             },
    //           },
    //         },
    //       },
    //     ];
    //   } else {
    //     return [];
    //   }
    // });

    // const resizeHeight = () => {
    //   if (series.value.length >= 8 && window.innerWidth > 1700) {
    //     return 580;
    //   }
    //   if (series.value.length >= 8 && window.innerWidth >= 1400 && window.innerWidth <= 1700) {
    //     return 400;
    //   }
    //   if (series.value.length >= 8 && window.innerWidth < 1400) {
    //     return 300;
    //   }
    //   if (series.value.length < 8 && window.innerWidth > 1700) {
    //     return 580;
    //   }
    //   if (series.value.length < 8 && window.innerWidth >= 1400 && window.innerWidth <= 1700) {
    //     return 350;
    //   } else {
    //     return 300;
    //   }
    // };

    // const resizeLegendMargin = () => {
    //   if (series.value.length <= 2) {
    //     return 100;
    //   }
    //   if (series.value.length > 2 && series.value.length <= 4) {
    //     return window.innerWidth <= 1200 ? 120 : 100;
    //   }
    //   if (series.value.length > 4 && series.value.length <= 8) {
    //     if (window.innerWidth <= 1030) return 210;
    //     if (window.innerWidth > 1030 && window.innerWidth <= 1285) return 160;
    //     if (window.innerWidth <= 1700 && window.innerWidth > 1285) return 140;
    //     return 110;
    //   }
    //   return 100; // Default fallback
    // };

    const scatter = () => {
      lineType.value = !lineType.value;
      lineWidth.value = !lineWidth.value;
    };

    const fullscreen = () => {
      console.log("fullscreen method")
    //   highchartsRef.value?.chart.fullscreen.toggle();
    };

    const exportPNG = () => {
      console.log("exportPNG method")
    //   if (selectedPointsCount.value < 1500) {
    //     highchartsRef.value?.chart.exportChart({
    //       type: "image/png",
    //       filename: new Date().toISOString(),
    //     });
    //   } else {
    //     alert("Too many points: Must be < 1500 points");
    //   }
    };

    // const langOptionChanges = (lang: string) => {
    //   const options = lang === "fr" ? {
    //     months: ["Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"],
    //     weekdays: ["Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"],
    //     shortMonths: ["jan", "fév", "mar", "avr", "mai", "juin", "juil", "aoû", "sep", "oct", "nov", "déc"],
    //     resetZoom: "Réinitialiser le zoom",
    //     decimalPoint: "."
    //   } : {
    //     months: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
    //     weekdays: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"],
    //     shortMonths: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
    //     resetZoom: "Reset zoom",
    //     decimalPoint: "."
    //   };
    //   Highcharts.setOptions({ lang: options });
    // };

    onMounted(() => {
      // langUnwatcher.value = watch(() => langOptionChanges(props.deviceType), (lang) => {
      //   langOptionChanges(lang);
      // });
    });

    onBeforeUnmount(() => {
      langUnwatcher.value?.();
    });

    return {
      // chartOptions,
      scatter,
      // fullscreen,
      // exportPNG,
      // highchartsRef,
    };
  },
});
</script>

<style scoped lang="scss"></style>

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