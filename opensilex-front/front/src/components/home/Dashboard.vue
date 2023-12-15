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
        <!-- <div class="twitter">
          <opensilex-Twitter></opensilex-Twitter>
        </div> -->
    </div>

<!-- treeview simple-->
<template>
  <v-treeview :items="itemsSimple" selectable></v-treeview>
</template>

<v-container>
    <v-select v-model="selectionType" :items="['leaf', 'independent']" label="Selection type"></v-select>
    <v-row>
      <v-col>
        <v-treeview
          v-model="selection"
          :items="items"
          :selection-type="selectionType"
          selectable
          return-object
          open-all
        ></v-treeview>
      </v-col>
      <v-divider vertical></v-divider>
    </v-row>
  </v-container>
  </div>

</template>

<script lang="ts">
import { Component } from 'vue-property-decorator';
import Vue from 'vue';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
//import { VTreeview } from "vuetify/lib";

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
selectionType= "leaf";
    selection= [];
    items= [
      {
        id: 1,
        name: "Root",
        children: [
          { id: 2, name: "Child 1" },
          { id: 3, name: "Child 2" },
          {
            id: 4,
            name: "Child 3",
            children: [
              { id: 5, name: "Grandchild 1" },
              { id: 6, name: "Grandchild 2" },
            ],
          },
        ],
      },
    ];



itemsSimple= [
        {
          id: 1,
          name: 'Applications :',
          children: [
            { id: 2, name: 'Calendar : app' },
            { id: 3, name: 'Chrome : app' },
            { id: 4, name: 'Webstorm : app' },
          ],
        },
        {
          id: 5,
          name: 'Documents :',
          children: [
            {
              id: 6,
              name: 'vuetify :',
              children: [
                {
                  id: 7,
                  name: 'src :',
                  children: [
                    { id: 8, name: 'index : ts' },
                    { id: 9, name: 'bootstrap : ts' },
                  ],
                },
              ],
            },
            {
              id: 10,
              name: 'material2 :',
              children: [
                {
                  id: 11,
                  name: 'src :',
                  children: [
                    { id: 12, name: 'v-btn : ts' },
                    { id: 13, name: 'v-card : ts' },
                    { id: 14, name: 'v-window : ts' },
                  ],
                },
              ],
            },
          ],
        },
        {
          id: 15,
          name: 'Downloads :',
          children: [
            { id: 16, name: 'October : pdf' },
            { id: 17, name: 'November : pdf' },
            { id: 18, name: 'Tutorial : html' },
          ],
        },
        {
          id: 19,
          name: 'Videos :',
          children: [
            {
              id: 20,
              name: 'Tutorials :',
              children: [
                { id: 21, name: 'Basic layouts : mp4' },
                { id: 22, name: 'Advanced techniques : mp4' },
                { id: 23, name: 'All about app : dir' },
              ],
            },
            { id: 24, name: 'Intro : mov' },
            { id: 25, name: 'Conference introduction : avi' },
          ],
        },
      ];

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
    "favorites graph1 graph1 graph1"
    "favorites graph1 graph1 graph1";
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
