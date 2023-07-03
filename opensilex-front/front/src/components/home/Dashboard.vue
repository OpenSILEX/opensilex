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


        <!-- Graphic-->
        <div class="graph1"
             v-if="$opensilex.getConfig().dashboard.graph1.variable"
        >
          <opensilex-Histogram
            :variableChoice="$opensilex.getConfig().dashboard.graph1.variable"
          >
          </opensilex-Histogram>
        </div>

        <!-- Logo -->
        <div class="graph1 logoContainer"
             v-if="!$opensilex.getConfig().dashboard.graph1.variable"
        >
          <img
            v-bind:src="$opensilex.getResourceURI('images/dashboardLogo', ['png', 'svg', 'jpg'])"
            class="dashboardCentralLogo"
            alt
          />
        </div>

        <!-- Twitter -->
        <div class="twitter">
          <opensilex-Twitter></opensilex-Twitter>
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
    "favorites graph1 graph1 twitter";
  width: 100%;
}

.favorites { grid-area: favorites; }

.twitter { grid-area: twitter; }

.graph1 { grid-area: graph1; }

.logoContainer{
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 60vh;
}

.dashboardCentralLogo{
  opacity: 0.4;
  min-height: 35vh;
  max-height: 50%;
  max-width: 80%;
}

/** Mozilla Firefox */
@supports (-moz-appearance:none) {
  .logoContainer {
    height: 80%;
  }
}

@media (max-height: 700px) {
  .dashboardCentralLogo{
    margin-top : 20%;
  }
  /**Media Querry for Mozilla */
  @supports (-moz-appearance:none) {
    .dashboardCentralLogo {
      margin-top: 0%;
    }
  }
}

@media only screen and (min-width: 2000px) {
  .dashboardCentralLogo{
    margin-top: 5%
  }
}
@media (max-width: 1180px) {
  .gridMainContainer {
    display: block;
  }
  .favorites, .twitter, .graph1{
    width: 100%;
  }
  .logoContainer{
    display: none;
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
