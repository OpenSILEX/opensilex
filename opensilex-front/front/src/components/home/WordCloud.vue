<template>
  <div ref="chartContainer" style="width: 600px; height: 400px"></div>
</template>

<script>
import * as echarts from "echarts";

export default {
  name: "LogHistogram",
  data() {
    return {
      chartInstance: null,
      transformedData: [],
    };
  },
  props: {
    result: {},
  },
  mounted() {
    this.initChart();
  },
  beforeDestroy() {
    if (this.chartInstance) {
      this.chartInstance.dispose();
    }
  },
  methods: {
    initChart() {
      const chartDom = this.$refs.chartContainer;
      this.chartInstance = echarts.init(chartDom);
      const option = {
        title: {
          text: "Log-Scaled sample density per variable", // Texte du titre
          left: "center", // Positionnement horizontal du titre
          textStyle: {
            color: "#333", // Couleur du texte du titre
            fontSize: 18, // Taille du texte du titre
          },
        },
        tooltip: {
          trigger: "item", // Changed to item for single series
          formatter: "{a} <br/>{b}: {c}", // Customize tooltip content
        },
        legend: {
          orient: "horizontal",
          left: "center",
          data: this.getLegendData(), // Use a method to generate legend data
        },
        grid: {
          containLabel: true,
        },
        xAxis: {
          type: "category",
          data: this.getVariableNames(), // Use a method to get variable names
          axisLabel: {
            rotate: 45, // Rotate labels for better readability
          },
        },
        yAxis: {
          type: "value",
          axisLabel: {
            formatter: "[{value}]", // Custom formatter for y-axis labels
          },
        },
        series: [
          {
            name: "Log-Scaled sample density per variable",
            type: "bar",
            data: this.transformedData,
            label: {
              show: true,
              position: "insideTop",
            },
          },
        ],
      };

      this.chartInstance.setOption(option);
    },
    transformData() {
      console.log(this.result);
      const variables = this.result.has_variables;
      const data = Object.entries(variables).map(([name, variable]) => {
        const variableNameParts = name.split("/");
        const variableName = variableNameParts[variableNameParts.length - 1];

        return {
          name: variableName,
          value: Math.log10(variable.nb_of_elements).toFixed(2),
        };
      });
      this.transformedData = data;
    },
    getLegendData() {
      return this.transformedData.map((item) => item.name);
    },
    getVariableNames() {
      return this.transformedData.map((item) => item.name);
    },
  },
  created() {
    this.transformData();
  },
};
</script>
