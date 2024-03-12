<template>
  <div ref="chart" style="width: 400px; height: 300px; margin-bottom: 0px"></div>
</template>

<script>
import * as echarts from "echarts";

export default {
  name: "CircularGraph",
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
          text: this.title, // Use the title prop here
          x: "center", // Center the title
          top: "20%",
          textStyle: {
            fontSize: 15, // Adjust the font size as needed
          },
        },
        series: [
          {
            type: "pie",
            radius: "30%",
            data: this.data,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: "rgba(0, 0, 0, 0.5)",
              },
            },
          },
        ],
        tooltip: {
          trigger: "item",
          formatter: "Sample Density <br/>{b} : {c} ({d}%)",
        },
      };
      this.chart.setOption(option);
    },
  },
};
</script>

<style scoped>
.chart {
  height: 400px;
}
</style>
