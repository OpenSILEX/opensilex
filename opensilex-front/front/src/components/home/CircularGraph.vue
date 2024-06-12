<template>
  <div ref="chart" style="width: 100%; height: 400px"></div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import * as echarts from "echarts";

@Component
export default class BarChart extends Vue {
  private chart: echarts.ECharts | null = null;

  mounted() {
    this.initChart();
  }

  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose();
    }
  }

  private filterZeroValues(data: number[]) {
    return data.map((value) => (value === 0 ? null : value));
  }

  private initChart() {
    const chartDom = this.$refs.chart as HTMLDivElement;
    this.chart = echarts.init(chartDom);

    const option = {
      tooltip: {
        trigger: "axis",
        axisPointer: {
          type: "shadow",
        },
      },
      legend: {
        type: "scroll",
        orient: "horizontal",
        left: "center",
        top: "top",
      },
      grid: {
        left: "3%",
        right: "4%",
        bottom: "3%",
        containLabel: true,
      },
      xAxis: [
        {
          type: "category",
          data: ["Taxonomy", "Sub-Taxonomy", "Scientific Measurements"],
        },
      ],
      yAxis: [
        {
          type: "value",
        },
      ],
      series: [
        {
          name: "Taxonomy",
          type: "bar",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([862, 0, 0]),
          markLine: {
            lineStyle: {
              type: "dashed",
            },
            data: [[{ type: "min" }, { type: "max" }]],
          },
        },
        {
          name: "Class",
          type: "bar",
          barWidth: 5,
          stack: "Taxonomy",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([620, 0, 0]),
        },
        {
          name: "Family",
          type: "bar",
          stack: "Taxonomy",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([120, 0, 0]),
        },
        {
          name: "Genus",
          type: "bar",
          stack: "Taxonomy",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([60, 0, 0]),
        },
        {
          name: "Order",
          type: "bar",
          stack: "Taxonomy",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([62, 0, 0]),
        },
        {
          name: "Species",
          type: "bar",
          stack: "Taxonomy",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([31, 0, 0]),
        },
        {
          name: "Sub-Taxonomy",
          type: "bar",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, 862, 0]),
          markLine: {
            lineStyle: {
              type: "dashed",
            },
            data: [[{ type: "min" }, { type: "max" }]],
          },
        },
        {
          name: "Common Name",
          type: "bar",
          barWidth: 5,
          stack: "Sub-Taxonomy",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, 701, 0]),
        },
        {
          name: "Scientific Name",
          type: "bar",
          stack: "Sub-Taxonomy",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, 120, 0]),
        },
        {
          name: "Espece Brut",
          type: "bar",
          stack: "Sub-Taxonomy",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, 60, 0]),
        },
        {
          name: "Name Raw",
          type: "bar",
          stack: "Sub-Taxonomy",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, 62, 0]),
        },
        {
          name: "Species Raw",
          type: "bar",
          stack: "Sub-Taxonomy",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, 31, 0]),
        },
        {
          name: "Scientific Measurements",
          type: "bar",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, 0, 1600]),
          markLine: {
            lineStyle: {
              type: "dashed",
            },
            data: [[{ type: "min" }, { type: "max" }]],
          },
        },
        {
          name: "Abundance",
          type: "bar",
          barWidth: 5,
          stack: "Scientific Measurements",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, 0, 734]),
        },
        {
          name: "Flower Density",
          type: "bar",
          stack: "Scientific Measurements",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, 0, 134]),
        },
        {
          name: "Monocot Percent",
          type: "bar",
          stack: "Scientific Measurements",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, 0, 74]),
        },
        {
          name: "Final Seed Number",
          type: "bar",
          stack: "Scientific Measurements",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, 0, 84]),
        },
        {
          name: "Initial Seed Number",
          type: "bar",
          stack: "Scientific Measurements",
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, 0, 84]),
        },
      ],
    };

    this.chart.setOption(option);
  }
}
</script>

<style scoped>
/* Ajoutez des styles personnalisés ici si nécessaire */
</style>
