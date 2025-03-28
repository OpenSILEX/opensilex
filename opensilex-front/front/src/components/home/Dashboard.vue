<template>
  <div>
    <!-- Header -->
    <opensilex-PageHeader
      icon="fa#tachometer-alt"
      :hasIcon="true"
      :title="t('DashboardUser.title')"
      :description="t('DashboardUser.description')"
      class="detail-element-header"
    ></opensilex-PageHeader>


      <opensilex-DataMonitoring></opensilex-DataMonitoring>
    <!-- DataMonitoring -->
    <!-- <div
      class="dashboardDataMonitoringContainer"
      v-if="opensilex.getConfig().dashboard.showMetrics"
    >
      <span> data monitoring </span>
    </div> -->

    <div class="gridMainContainer">
      <!-- Favorites -->
      <div class="favorites">
        <opensilex-Favorites></opensilex-Favorites>
      </div>

      <!-- Graphic -->
      <div class="graph1" v-if="opensilex.getConfig().dashboard.graph1.variable">
        <opensilex-Histogram 
          :variableChoice="opensilex.getConfig().dashboard.graph1.variable"
        ></opensilex-Histogram>
      </div>

      <!-- Logo -->
      <div class="graph1 logoContainer" v-if="!opensilex.getConfig().dashboard.graph1.variable">
        <img
          :src="opensilex.getResourceURI('images/dashboardLogo', ['png', 'svg', 'jpg'])"
          class="dashboardCentralLogo"
          alt
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, onMounted, onUnmounted, ref, inject } from "vue";
import { useStore } from "vuex";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { useI18n } from "vue-i18n";
import Histogram from "./dashboard/Histogram.vue";
import DataMonitoring from "./dashboard/DataMonitoring.vue"

export default defineComponent({
  name: "Dashboard",
    components: {
    Histogram,
    DataMonitoring
  },
  setup() {
    const store = useStore();
    const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
    const width = ref<number | null>(null);
    const { t } = useI18n(); // récupération de la fonction t

    // Définir le titre dans setup() et le retourner
    const title = t("DashboardUser.title");

    const resizeScreen = () => {
      const minSize = 1400;
      if (document.body.clientWidth <= minSize && (width.value == null || width.value > minSize)) {
        width.value = document.body.clientWidth;
        store.commit("hideMenu");
      } else if (document.body.clientWidth > minSize && (width.value == null || width.value <= minSize)) {
        width.value = document.body.clientWidth;
        store.commit("showMenu");
      }
    };

    onMounted(() => {
      if (matchMedia("(max-width: 1625px)").matches) {
        store.commit("hideMenu");
      }
      window.addEventListener("resize", resizeScreen);
      resizeScreen();
    });

    onUnmounted(() => {
      window.removeEventListener("resize", resizeScreen);
    });

    // Retourner toutes les propriétés nécessaires dans le template
    return {
      opensilex,
      width,
      title,
      t,
    };
  },
});
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

.favorites {
  grid-area: favorites;
}

.twitter {
  grid-area: twitter;
}

.graph1 {
  grid-area: graph1;
}

.logoContainer {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 60vh;
}

.dashboardCentralLogo {
  opacity: 0.4;
  min-height: 35vh;
  max-height: 50%;
  max-width: 80%;
}

@supports (-moz-appearance: none) {
  .logoContainer {
    height: 80%;
  }
}

@media (max-height: 700px) {
  .dashboardCentralLogo {
    margin-top: 20%;
  }
  @supports (-moz-appearance: none) {
    .dashboardCentralLogo {
      margin-top: 0%;
    }
  }
}

@media only screen and (min-width: 2000px) {
  .dashboardCentralLogo {
    margin-top: 5%;
  }
}

@media (max-width: 1180px) {
  .gridMainContainer {
    display: block;
  }
  .favorites,
  .twitter,
  .graph1 {
    width: 100%;
  }
  .logoContainer {
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
    description: Dernières informations de la plateforme
</i18n>
