<template>
  <div style="min-height: 400px;">
    <div class="card-body p-0">
      <highcharts
        v-for="(options, index) in chartOptions"
        :key="index"
        :options="options"
        :constructor-type="'stockChart'"
        ref="highcharts"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount, computed, watch, inject } from 'vue';
import { useStore } from 'vuex';
import Highcharts from 'highcharts';
import exportingInit from 'highcharts/modules/exporting';
import HighchartsCustomEvents from 'highcharts-custom-events';
import OpenSilexVuePlugin from '../../../models/OpenSilexVuePlugin';

// Extensions Highcharts
HighchartsCustomEvents(Highcharts);
exportingInit(Highcharts);

// Props
const props = defineProps<{
  deviceType?: boolean;
  lType?: boolean;
  lWidth?: boolean;
}>();

// Refs & Reactive Data
const store = useStore();
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const highchartsRef = ref([]);
const topPosition = ref(0);
const leftPosition = ref(0);
const selectedPointsCount = ref(0);
const series = ref<Highcharts.SeriesOptionsType[]>([]);
const yAxis = ref<any>({
  title: { text: '' }
});
const variable = ref<any>(null);
const startDate = ref<any>(null);
const endDate = ref<any>(null);
const lineType = ref(props.lType ?? false);
const lineWidth = ref(props.lWidth ?? false);




// Watchers
const langWatcher = watch(
  () => store.getters.language,
  (lang) => langOptionChanges(lang),
  { immediate: true }
);

// Methods
function resizeHeight() {
  return 400;
}

function resizeLegendMargin() {
  return 40;
}

function langOptionChanges(lang: string) {
  if (lang === 'fr') {
    Highcharts.setOptions({
      lang: {
        months: ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'],
        weekdays: ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'],
        shortMonths: ['jan', 'fév', 'mar', 'avr', 'mai', 'juin', 'juil', 'aoû', 'sep', 'oct', 'nov', 'déc'],
        resetZoom: 'Réinitialiser le zoom',
        decimalPoint: '.'
      }
    });
  } else {
    Highcharts.setOptions({
      lang: {
        months: ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'],
        weekdays: ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'],
        shortMonths: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
        resetZoom: 'Reset zoom',
        decimalPoint: '.'
      }
    });
  }
}

function buildYAxis() {
  return {
    labels: { align: 'right', x: -3 },
    title: {
      text: variable.value ? `${variable.value.name} (${variable.value.unit.name})` : '',
      style: {
        fontWeight: 'bold',
        color: Highcharts.getOptions().colors?.[1]
      }
    }
  };
}

function reload(newSeries: Highcharts.SeriesOptionsType[], newVariable: any) {
  variable.value = newVariable;
  if (newSeries.length > 0) yAxis.value = buildYAxis();
  series.value = newSeries;
}

function fullscreen() {
  highchartsRef.value[0].chart.fullscreen.toggle();
}

function exportPNG() {
  if (selectedPointsCount.value < 1500) {
    highchartsRef.value[0].chart.exportChart({
      type: 'image/png',
      filename: new Date().toISOString()
    });
  } else {
    $opensilex.showInfoToast(`Too much points: ${selectedPointsCount.value}. Must be < 1500`);
  }
}

function scatter() {
  lineType.value = !lineType.value;
  lineWidth.value = !lineWidth.value;
}

// Computed
const chartOptions = computed(() => {
  if (!series.value.length) return [];

  return [
    {
      chart: {
        zoomType: 'x',
        marginLeft: 10,
        height: resizeHeight(),
        type: lineType.value ? 'line' : 'scatter',
        events: {
          click: () => {
            highchartsRef.value[0].chart.tooltip.hide(0);
          },
          render: function () {
            selectedPointsCount.value = 0;
            series.value.forEach((serie: any) => {
              if (serie.data && serie.name !== 'Navigator 1') {
                selectedPointsCount.value += serie.data.length;
              }
            });
          }
        }
      },
      title: { text: '' },
      subtitle: { text: '' },
      credits: { enabled: false },
      navigation: { buttonOptions: { enabled: false } },
      exporting: {
        sourceWidth: 800,
        sourceHeight: 500,
        scale: 2
      },
      scrollbar: { enabled: false },
      rangeSelector: { enabled: false },
      navigator: {
        enabled: props.deviceType,
        margin: 5
      },
      legend: {
        enabled: true,
        floating: false,
        verticalAlign: 'bottom',
        align: 'center',
        y: 0
      },
      xAxis: {
        type: 'datetime',
        title: { text: 'Time' },
        min: startDate.value ? new Date(startDate.value).getTime() : undefined,
        max: endDate.value ? new Date(endDate.value).getTime() : undefined,
        labels: {
          formatter: function () {
            return Highcharts.dateFormat('%e-%b-%Y %H:%M', this.value);
          }
        },
        marginBottom: resizeLegendMargin(),
        ordinal: false,
        crosshair: true,
        showLastLabel: true,
        endOnTick: true
      },
      yAxis: yAxis.value,
      tooltip: {
        useHTML: true,
        formatter: function () {
          const point = this.point as any;
          const dateStr = Highcharts.dateFormat('%d-%m-%Y %H:%M:%S', this.x);
          const value = point.y ?? point.text;
          return `${point.series.name} : <span style="color:${point.color}"><b>${value}</b></span><br/>Time:<b> ${dateStr}</b>`;
        },
        split: false,
        shared: false
      },
      series: series.value,
      plotOptions: {
        line: {
          marker: { enabled: true, symbol: 'circle', radius: 3 }
        },
        series: {
          turboThreshold: 100000,
          cursor: 'pointer',
          dataGrouping: { enabled: true },
          lineWidth: lineWidth.value ? 2 : 0,
          stickyTracking: false,
          marker: {
            states: { hover: { enabled: true } }
          },
          states: { inactive: { opacity: 0.4 } }
        }
      }
    }
  ];
});

// Lifecycle
onMounted(() => {
  Highcharts.wrap(Highcharts.Legend.prototype as any, 'colorizeItem', function (
    proceed,
    item,
    visible
  ) {
    const color = item.color;
    item.color = item.options.legendColor;
    proceed.call(this, visible);
    item.color = color;
  });
});

onBeforeUnmount(() => {
  langWatcher(); // Stop watcher
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