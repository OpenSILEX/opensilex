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
          text: this.title,
          left: "center",
          top: "10%",
          textStyle: {
            fontSize: 15,
          },
        },
        tooltip: {
          trigger: "axis",
          axisPointer: {
            type: "shadow",
          },
        },
        legend: {
          data: this.data.series.map((s) => s.name),
        },
        grid: {
          left: "3%",
          right: "4%",
          bottom: "3%",
          containLabel: true,
        },
        xAxis: {
          type: "category",
          data: this.data.categories,
        },
        yAxis: {
          type: "value",
        },
        series: this.data.series,
      };
      this.chart.setOption(option);
    },
  },
};
</script>
