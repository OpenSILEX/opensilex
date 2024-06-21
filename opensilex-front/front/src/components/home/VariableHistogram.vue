<template>
  <div ref="chartContainer" style="width: 600px; height: 400px"></div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from "vue-property-decorator";
import * as echarts from "echarts";

interface Variable {
  nb_of_elements: number;
  occurrence: number;
}

interface Result {
  variables: Record<string, Variable>;
}

@Component
export default class VariableHistogram extends Vue {
  @Prop() private result!: Result;

  private chartInstance: any = null;
  private transformedDataLog: Array<{
    name: string;
    value: string;
    occurrence: number;
  }> = [];
  private transformedData: Array<{
    name: string;
    value: number;
    occurrence: number;
  }> = [];
  private colorMap: Record<number, string> = {}; // Object to store occurrence to color mapping

  mounted() {
    this.initChart();
  }

  beforeDestroy() {
    if (this.chartInstance) {
      this.chartInstance.dispose();
    }
  }

  private initChart() {
    const chartDom = this.$refs.chartContainer as HTMLDivElement;
    this.chartInstance = echarts.init(chartDom);
    const option = {
      title: {
        text: "Nombre d'observation par variable (Echelle Logarithmique)",
        left: "center",
        textStyle: {
          color: "#333",
          fontSize: 18,
        },
      },
      tooltip: {
        trigger: "item",
        formatter: (params: any) => {
          const dataItem = this.transformedData[params.dataIndex];
          return `${params.seriesName} <br/>${params.name}: ${dataItem.value}<br/>Présent dans ${dataItem.occurrence} site(s)`;
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
        orient: "horizontal",
        left: "center",
        data: this.getLegendData(),
      },
      grid: {
        containLabel: true,
      },
      xAxis: {
        type: "category",
        data: this.getVariableNames(),
        axisLabel: {
          rotate: 45,
        },
      },
      yAxis: {
        type: "value",
        axisLabel: {
          formatter: "[{value}]",
        },
      },
      series: [
        {
          name: "Variables",
          type: "bar",
          data: this.transformedDataLog.map((item) => ({
            value: item.value,
            itemStyle: { color: this.colorMap[item.occurrence] }, // Assign color based on occurrence
          })),
        },
      ],
    };

    this.chartInstance.setOption(option);
  }

  private transformDataLog() {
    const predefinedColors = [
      "#5184dc",
      "#64e4e4",
      "#64e47f",
      "#d1e464",
      "#e4ae64",
      "#f05858",
    ]; // Predefined colors array
    const variables = this.result.variables;
    const data = Object.entries(variables).map(([name, variable]) => {
      const variableNameParts = name.split("/");
      const variableName = variableNameParts[variableNameParts.length - 1];

      return {
        name: variableName,
        value: Math.log10(variable.nb_of_elements).toFixed(2),
        occurrence: variable.occurrence,
      };
    });

    this.transformedDataLog = data;

    // Create a mapping from occurrence to color
    data.forEach((item) => {
      this.colorMap[item.occurrence] = predefinedColors[item.occurrence - 1];
    });
  }

  private transformData() {
    const variables = this.result.variables;
    const data = Object.entries(variables).map(([name, variable]) => {
      const variableNameParts = name.split("/");
      const variableName = variableNameParts[variableNameParts.length - 1];

      return {
        name: variableName,
        value: variable.nb_of_elements,
        occurrence: variable.occurrence,
      };
    });
    this.transformedData = data;
  }

  private getLegendData() {
    return this.transformedDataLog.map((item) => item.name);
  }

  private getVariableNames() {
    return this.transformedDataLog.map((item) => item.name);
  }

  created() {
    this.transformDataLog();
    this.transformData();
  }
}
</script>
