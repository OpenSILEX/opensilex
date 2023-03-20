<template>
  <div>
    <!--Header -->
    <opensilex-PageHeader
      icon="fa#tachometer-alt"
      title="DashboardUser.title"
      class= "detail-element-header"
    ></opensilex-PageHeader>

      <!--DataMonitoring -->
      <div class="dashboardDataMonitoringContainer" v-if="$opensilex.getConfig().dashboard.showMetrics">
        <opensilex-DataMonitoring></opensilex-DataMonitoring>
      </div>

      <div class="gridMainContainer">

        <!-- Favorites -->
        <div class="favorites">
          <opensilex-Favorites></opensilex-Favorites>
        </div>


        <!-- Graphic (1)-->
        <div class="graph1"
             v-if="$opensilex.getConfig().dashboard.graph1.variable"
        >
          <opensilex-Histogram
            :variableChoice="$opensilex.getConfig().dashboard.graph1.variable"
          >
          </opensilex-Histogram>
        </div>

        <!-- Twitter -->
        <div class="twitter">
          <opensilex-Twitter></opensilex-Twitter>
        </div>

         <!-- Graphic (2)-->
        <div class="graph2"
          v-if="$opensilex.getConfig().dashboard.graph2.variable"
        >
          <opensilex-Histogram
            :variableChoice="$opensilex.getConfig().dashboard.graph2.variable"
          >
          </opensilex-Histogram>
        </div>

       <!-- Graphic (3)-->
        <div class="graph3"
          v-if="$opensilex.getConfig().dashboard.graph3.variable"
        >
          <opensilex-Histogram
            :variableChoice="$opensilex.getConfig().dashboard.graph3.variable"
          >
          </opensilex-Histogram>
        </div>
    </div>
  </div>

</template>

<script lang="ts">
import { Component } from 'vue-property-decorator';
import Vue from 'vue';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class Dashboard extends Vue {

  width;
  $store: any;
  $opensilex: OpenSilexVuePlugin;

  created() {
    // close global menu on medium / low size
    this.$nextTick(() => {
      if (matchMedia('(max-width: 1625px)').matches) {
        this.$store.commit("hideMenu")
      }
    })
    window.addEventListener("resize", this.resizeScreen);
    this.resizeScreen();
  }

  beforeDestroy() {
    window.removeEventListener("resize", this.resizeScreen);
  }

  resizeScreen() {
    const minSize = 1400;
    if (
      document.body.clientWidth <= minSize &&
      (this.width == null || this.width > minSize)
    ) {
      this.width = document.body.clientWidth;
      this.$store.commit("hideMenu");
    } else if (
      document.body.clientWidth > minSize &&
      (this.width == null || this.width <= minSize)
    ) {
      this.width = document.body.clientWidth;
      this.$store.commit("showMenu");
    }
  }
}
</script>

<style scoped lang="scss">
.gridMainContainer {
  display: grid;
  grid-template-columns: 20% 25% 30% 23%; 
  grid-template-rows: 5% 25% 33.4% 36.6%; 
  gap: 0px 10px;
  grid-auto-flow: column dense;
  justify-items: stretch;
  grid-template-areas: 
    "favorites graph1 graph1 twitter"
    "favorites graph1 graph1 twitter"
    "graph2 graph2 graph3 ."; 
  width: 100%;
}

.favorites { grid-area: favorites; }

.twitter { grid-area: twitter; }

.graph1 { grid-area: graph1; }

.graph2 { grid-area: graph2; }

.graph3 { grid-area: graph3; }


@media (max-width: 1180px) {
  .gridMainContainer {
    display: block;
  }
  .favorites, .twitter, .graph1{
    width: 100%;
  }
}
</style>
<i18n>
en:
  DashboardUser:
    title: Dashboard
    description: Latest platform information

fr:
  DashboardUser:
    title: Tableau de bord
    description: Dernieres informations de la plateforme
</i18n>
