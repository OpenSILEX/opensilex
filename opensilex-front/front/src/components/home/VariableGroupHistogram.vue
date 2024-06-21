<template>
  <div ref="chart" style="width: 100%; height: 400px"></div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import * as echarts from "echarts";

@Component
export default class VariableGroupHistogram extends Vue {
  @Prop({ required: true }) result!: any;

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

  private processData() {
    const groups: Record<string, { total: number; variables: any[] }> = {};
    const variables = this.result.variables;

    for (const key in variables) {
      const variable = variables[key];
      if (variable.group) {
        if (!groups[variable.group]) {
          groups[variable.group] = { total: 0, variables: [] };
        }
        groups[variable.group].total += variable.nb_of_elements;
        groups[variable.group].variables.push({
          name: key.split("/").pop(),
          type: "bar",
          stack: variable.group,
          barWidth: 40,
          emphasis: {
            focus: "series",
          },
          data: this.filterZeroValues([0, variable.nb_of_elements]),
        });
      }
    }

    const seriesData: any[] = [];
    for (const group in groups) {
      seriesData.push({
        name: group,
        type: "bar",
        barWidth: 80,
        emphasis: {
          focus: "series",
        },
        data: this.filterZeroValues([groups[group].total, 0]),
        markLine: {
          lineStyle: {
            type: "dashed",
          },
          data: [[{ type: "min" }, { type: "max" }]],
        },
      });
      seriesData.push(...groups[group].variables);
    }

    return seriesData;
  }

  private initChart() {
    const chartDom = this.$refs.chart as HTMLDivElement;
    this.chart = echarts.init(chartDom);

    const processedSeries = this.processData();

    const option = {
      tooltip: {
        trigger: "axis",
        axisPointer: {
          type: "shadow",
        },
      },
      toolbox: {
        feature: {
          restore: {},
          saveAsImage: {},
        },
        top: "20",
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
          data: ["Group and Variables"],
        },
      ],
      yAxis: [
        {
          type: "log",
          axisLabel: {
            formatter: (value: number) => {
              return value;
            },
          },
        },
      ],
      series: processedSeries,
    };

    this.chart.setOption(option);
  }
}
</script>

<style scoped>
/* Ajoutez des styles personnalisés ici si nécessaire */
</style>
