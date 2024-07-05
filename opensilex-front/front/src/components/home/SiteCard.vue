<template>
  <div class="card">
    <div class="card-header">
      <h3 class="justify-self-center">{{ data.name }}</h3>
    </div>
    <div class="card-content">
      <time-line-chart class="justify-self-center" :data="data"></time-line-chart>
      <heat-map
        class="justify-self-center"
        :data="data"
        :filters="[filter.start_date, filter.end_date]"
        :title="data.name"
        :year="year"
      >
      </heat-map>
      <variable-group-histogram
        class="justify-self-center"
        :result="data"
        :title="`${data[0]}`"
      ></variable-group-histogram>
      <variable-histogram class="justify-self-center" :result="data">
      </variable-histogram>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from "vue-property-decorator";
import HeatMap from "./HeatMap.vue";
import TimeLineChart from "./TimeLineChart.vue";
import VariableGroupHistogram from "./VariableGroupHistogram.vue";
import VariableHistogram from "./VariableHistogram.vue";

@Component({
  components: {
    VariableGroupHistogram,
    HeatMap,
    VariableHistogram,
    TimeLineChart,
    SiteCard,
  },
})
export default class SiteCard extends Vue {
  @Prop({ required: true }) data!: any;
  @Prop({ required: true }) filter!: any;
  @Prop({ required: true }) index!: number;
  @Prop({ required: true }) year!: number;

  created() {
    console.log(this.year, this.data);
  }
}
</script>

<style scoped>
.card {
  border: 1px solid #ccc;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  width: 100%;
  justify-content: center;
  align-items: center;
}

.card-header {
  background: #f5f5f5;
  padding: 10px;
  display: grid;
  grid-template-rows: auto auto auto;
  justify-content: center;
  align-items: center;
  cursor: default;
}

.toggle-container {
  display: flex;
  justify-content: center;
  cursor: pointer;
  padding-top: 10px;
}

.toggle-arrow {
  font-size: 18px;
}

.card-content {
  padding: 10px;
}
</style>
