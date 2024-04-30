<template>
  <div ref="heatmapContainer" style="width: 100%; height: 500px; margin-top: 10%"></div>
</template>

<script>
import * as echarts from "echarts";

export default {
  name: "Heatmap",
  props: {
    siteData: {
      type: Object,
      required: true,
    },
    title: String,
  },
  mounted() {
    this.generateHeatMap();
  },
  watch: {
    siteData: {
      handler() {
        this.generateHeatMap();
      },
      deep: true,
    },
  },
  methods: {
    generateHeatMap() {
      const chartDom = this.$refs.heatmapContainer;
      const myChart = echarts.init(chartDom);
      const option = {
        title: {
          top: 0,
          left: "center",
          text: this.title,
          subtext:
            "from " +
            this.siteData.first_element_date +
            " to " +
            this.siteData.last_element_date,
        },
        tooltip: {},
        visualMap: {
          min: 0,
          max: 50000,
          type: "piecewise",
          orient: "horizontal",
          left: "center",
          top: 65,
        },
        calendar: {
          top: 120,
          left: 30,
          right: 30,
          cellSize: ["auto", 13],
          range: [this.siteData.first_element_date, this.siteData.last_element_date],

          yearLabel: { show: false },
        },
        series: {
          type: "heatmap",
          coordinateSystem: "calendar",
          data: this.transformSiteDataToHeatmapData(),
        },
      };

      myChart.setOption(option);
    },

    transformSiteDataToHeatmapData() {
      const data = [];
      Object.entries(this.siteData.has_variables).forEach(([key, value]) => {
        const date = new Date(value.first_element_date);
        const endDate = new Date(value.last_element_date);
        while (date <= endDate) {
          data.push([
            echarts.time.format(date, "{yyyy}-{MM}-{dd}", false),
            value.nb_of_elements,
          ]);
          date.setDate(date.getDate() + 1);
        }
      });
      return data;
    },
  },
};
</script>

<style scoped></style>
