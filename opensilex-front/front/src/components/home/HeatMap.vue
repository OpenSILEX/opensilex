<template>
  <div>
    <div ref="chart" style="width: 100%; height: 400px"></div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue, Watch } from "vue-property-decorator";
import * as echarts from "echarts";

interface Variable {
  first_element_date: string;
  last_element_date: string;
  nb_of_elements: number;
  occurence: number;
}

interface Data {
  variables: Record<string, Variable>;
  name: string;
}

@Component
export default class HeatmapChart extends Vue {
  @Prop({ required: true }) readonly year!: number; // Propriété obligatoire de type number pour l'année
  @Prop() private data!: Data; // Propriété de type Data

  private localSelectedYear: string = this.year.toString(); // Stockage local de l'année sélectionnée en chaîne
  private availableYears: string[] = ["2021", "2022", "2023", "2024"];
  private data2021: Array<number | null> = new Array(365).fill(null);
  private data2022: Array<number | null> = new Array(365).fill(null);
  private data2023: Array<number | null> = new Array(365).fill(null);
  private data2024: Array<number | null> = new Array(365).fill(null);
  private myChart: any = null; // Instance de l'objet chart

  @Watch("year")
  onYearChanged(newVal: number) {
    // Observer les changements de la propriété year
    this.localSelectedYear = newVal.toString();
    this.updateChart();
  }

  mounted() {
    this.fillDataArrays(); // Remplir les tableaux de données
    this.initChart(); // Initialiser le graphique
  }

  beforeDestroy() {
    if (this.myChart) {
      this.myChart.dispose(); // Nettoyer l'instance de graphique avant la destruction du composant
    }
  }

  private fillDataArrays() {
    // Remplir les tableaux de données avec les valeurs
    Object.values(this.data.variables).forEach((variable) => {
      const startDate = new Date(variable.first_element_date);
      const endDate = new Date(variable.last_element_date);

      for (
        let date = new Date(startDate);
        date <= endDate;
        date.setDate(date.getDate() + 1)
      ) {
        const year = date.getFullYear();
        const dayOfYear = Math.floor(
          (date.getTime() - new Date(year, 0, 0).getTime()) / (1000 * 60 * 60 * 24)
        );

        const dataKey = `data${year}` as keyof this;
        const dataYear = (this[dataKey] as unknown) as Array<number | null>;

        if (dataYear) {
          dataYear[dayOfYear] = (dataYear[dayOfYear] || 0) + variable.nb_of_elements;
        }
      }
    });

    // Remplacer les 0 par null dans les tableaux
    ["data2021", "data2022", "data2023", "data2024"].forEach((yearData) => {
      const dataArray = (this[yearData as keyof this] as unknown) as Array<number | null>;
      this[yearData as keyof this] = (dataArray.map((value) =>
        value === 0 ? null : value
      ) as unknown) as this[keyof this]; // Conversion finale en this[keyof this]
    });
  }

  private initChart() {
    // Initialiser le graphique avec ECharts
    const chartDom = this.$refs.chart as HTMLDivElement;
    this.myChart = echarts.init(chartDom);
    this.updateChart();
  }

  private updateChart() {
    // Mettre à jour le graphique avec les nouvelles données
    const data =
      ((this[`data${this.localSelectedYear}` as keyof this] as unknown) as Array<
        number | null
      >) || [];

    const option = {
      title: {
        left: "center",
        text: this.data.name,
      },
      tooltip: {},
      visualMap: {
        min: 0,
        max: 100000,
        type: "piecewise",
        orient: "horizontal",
        left: "center",
        top: "50%",
      },
      calendar: {
        left: 30,
        right: 30,
        cellSize: ["auto", 13],
        range: this.localSelectedYear,
        itemStyle: {
          borderWidth: 0.5,
        },
        yearLabel: { show: true },
      },
      series: {
        type: "heatmap",
        coordinateSystem: "calendar",
        data: data
          .map((value, index) => {
            const date = new Date(parseInt(this.localSelectedYear), 0, index + 1);
            return [echarts.time.format(date, "{yyyy}-{MM}-{dd}", false), value];
          })
          .filter((item) => item[1] !== null),
      },
    };

    this.myChart.setOption(option);
  }
}
</script>
