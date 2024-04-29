<template>
  <div ref="chart" style="width: 50vh; height: 55vh; margin-bottom: 10px"></div>
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
          left: "center",
          subtext: "Densité de mesure par variable",
        },
        tooltip: {
          trigger: "item",
          formatter: "{a} <br/>{b} : {c} ({d}%)",
        },
        legend: {
          left: "center",
          top: "bottom",
          data: this.data.map((item) => item.name), // Assuming each item in data has a 'name' property
        },
        toolbox: {
          show: true,
          feature: {
            mark: { show: true },
            dataView: { show: true, readOnly: false },
            restore: { show: true },
            saveAsImage: { show: true },
          },
        },
        series: [
          {
            name: "Data",
            type: "pie",
            radius: "50%", // Adjust the radius as needed
            center: ["50%", "50%"], // Center the pie chart
            data: this.data,
            roseType: "radius", // This is optional and depends on your preference
            itemStyle: {
              borderRadius: 5,
              marginBottom: 20,
            },
            label: {
              show: false,
            },
            emphasis: {
              label: {
                show: true,
              },
            },
          },
        ],
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
