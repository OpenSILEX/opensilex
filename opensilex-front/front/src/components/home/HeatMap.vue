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
    filters: [String, String],
    title: String,
  },
  mounted() {
    console.log("filters: ", this.filters);
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
          subtext: "from " + this.getRange()[0] + " to " + this.getRange()[1],
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
          range: this.getRange(),

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

    getRange() {
      if (this.filters[0] && !this.filters[1])
        return [this.filters[0], this.siteData.last_element_date];
      else if (!this.filters[0] && this.filters[1])
        return [this.siteData.first_element_date, this.filters[1]];
      else if (this.filters[0] && this.filters[1])
        return [this.filters[0], this.filters[1]];
      return [this.siteData.first_element_date, this.siteData.last_element_date];
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
