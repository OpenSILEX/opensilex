<template>
  <div>
    <div ref="chart" style="width: 100%; height: 400px"></div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";
import * as echarts from "echarts";

interface Variable {
  nb_of_elements: number;
  first_element_date: string;
  last_element_date: string;
  occurrence: number;
}

interface Data {
  variables: Record<string, Variable>;
  name: string;
  firstElementDate: string;
  lastElementDate: string;
  keywords: string[];
}

@Component
export default class LargeAreaChart extends Vue {
  @Prop({ required: true }) private data!: Data;
  private myChart: echarts.ECharts | null = null;

  mounted() {
    this.initChart();
  }

  beforeDestroy() {
    if (this.myChart) {
      this.myChart.dispose();
    }
  }

  private initChart() {
    const chartDom = this.$refs.chart as HTMLDivElement;
    this.myChart = echarts.init(chartDom);

    const { variables, firstElementDate, lastElementDate } = this.data;

    // Initialiser les dates de début et de fin
    const base = +new Date(firstElementDate);
    const end = +new Date(lastElementDate);
    const oneDay = 24 * 3600 * 1000;
    const date: string[] = [];
    const data: number[] = [];
    const variablesData: Record<string, number[]> = {};

    // Créer un tableau de dates et un tableau de valeurs
    for (let time = base; time <= end; time += oneDay) {
      const now = new Date(time);
      date.push([now.getFullYear(), now.getMonth() + 1, now.getDate()].join("/"));
      data.push(0);
    }

    // Initialiser les tableaux pour chaque variable
    Object.keys(variables).forEach((key) => {
      variablesData[key] = new Array(data.length).fill(0);
    });

    // Remplir les données en fonction des variables
    Object.entries(variables).forEach(([key, variable]) => {
      const start = +new Date(variable.first_element_date);
      const finish = +new Date(variable.last_element_date);
      for (let time = start; time <= finish; time += oneDay) {
        const dayIndex = Math.floor((time - base) / oneDay);
        if (dayIndex >= 0 && dayIndex < data.length) {
          data[dayIndex] += variable.nb_of_elements;
          variablesData[key][dayIndex] = variable.nb_of_elements;
        }
      }
    });

    const option = {
      tooltip: {
        trigger: "axis",
        formatter: (params: any) => {
          const date = params[0].axisValue;
          let tooltipText = `Date: ${date}<br/>`;
          params.forEach((param: any) => {
            tooltipText += `${param.seriesName}: ${param.data}<br/>`;
          });
          Object.entries(variablesData).forEach(([key, values]) => {
            tooltipText += `${key}: ${values[params[0].dataIndex]}<br/>`;
          });
          return tooltipText;
        },
      },
      title: {
        left: "center",
        text: "Large Area Chart",
      },
      toolbox: {
        feature: {
          dataZoom: {
            yAxisIndex: "none",
          },
          restore: {},
          saveAsImage: {},
        },
      },
      xAxis: {
        type: "category",
        boundaryGap: false,
        data: date,
      },
      yAxis: {
        type: "value",
        boundaryGap: [0, "100%"],
      },
      dataZoom: [
        {
          type: "inside",
          start: 0,
          end: 10,
        },
        {
          start: 0,
          end: 10,
        },
      ],
      series: [
        {
          name: "Data",
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
          data: data,
        },
      ],
    };

    if (option && this.myChart) {
      this.myChart.setOption(option);
    }
  }
}
</script>

<style scoped>
/* Ajoutez des styles personnalisés ici si nécessaire */
</style>
