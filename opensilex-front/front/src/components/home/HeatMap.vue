// BarGraph.vue
<template>
 <div ref="chart" style="width: 100%; height: 300px"></div>
</template>

<script>
import * as echarts from "echarts";

export default {
 name: "BarGraph",
 props: ["data", "title"],
 mounted() {
    this.chart = echarts.init(this.$refs.chart);
    this.updateChart();
 },
 watch: {
    data: {
      handler() {
        this.updateChart();
      },
      deep: true,
    },
 },
 methods: {
    updateChart() {
      const option = {
        title: {
          top: 30,
          left: 'center',
          text: this.title
        },
        tooltip: {},
        visualMap: {
          min: 0,
          max: 10000,
          type: 'piecewise',
          orient: 'horizontal',
          left: 'center',
          top: 65
        },
        calendar: {
          top: 120,
          left: 30,
          right: 30,
          cellSize: ['auto', 13],
          range: '2016',
          itemStyle: {
            borderWidth: 0.5
          },
          yearLabel: { show: false }
        },
        series: {
          type: 'heatmap',
          coordinateSystem: 'calendar',
          data: this.data // Assuming this.data is the result of getVirtualData('2016')
        }
      };
      this.chart.setOption(option);
    },
 },
};
</script>
