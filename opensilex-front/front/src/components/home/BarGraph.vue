<template>
  <div ref="chart" style="width: 100%; height: 100px"></div>
</template>

<script>
import * as echarts from "echarts";

export default {
  name: "LargeAreaChart",
  props: ["data"],
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
        tooltip: {
          trigger: "axis",
          position: function (pt) {
            return [pt[1], "10%"];
          },
        },
        toolbox: {
          feature: {},
        },
        xAxis: {
          type: "category",
          boundaryGap: false,
          data: this.data.date,
          show: false, // This line hides the x-axis
        },
        yAxis: {
          type: "value",
          boundaryGap: [0, "100%"],
        },
        dataZoom: [
          {
            type: "slider",
            start: 1990,
            end: 2040,
          },
          {
            start: 1990,
            end: 2040,
          },
        ],
        series: [
          {
            name: "Fake Data",
            type: "line",
            symbol: "none",
            sampling: "lttb",
            itemStyle: {
              color: "rgb(255, 70, 131)",
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: "rgb(255, 158, 68)",
                },
                {
                  offset: 1,
                  color: "rgb(255, 70, 131)",
                },
              ]),
            },
          },
        ],
      };
      this.chart.setOption(option);
    },
  },
};
</script>
