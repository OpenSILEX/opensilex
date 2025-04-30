<template>
  <opensilex-Overlay :show="isSearching">
    <opensilex-Card
      class="stats-card"
      :no-footer="true"
      :no-header="true"
      icon="fa#sort-numeric-down"
      label="DataMonitoring.title"
    >
      <template v-slot:body>
        <div class="globalStatsContainer">
          <span class="stats-values">

            <!-- Expériences -->
            <span class="expe">
              <opensilex-Icon icon="bi#bi-layers"/>
              <span>
                {{ nbExperiments }} ({{ deltaExperiments }}) 
                <a href="/app/experiments" class="metricsElementTitle" :title="t('DataMonitoring.redirectionToExpe')">
                  {{ $t("component.menu.experiments") }}
                </a>
              </span>
            </span>

            <!-- Objets scientifiques -->
            <!-- <span v-if="deltaScientificObjects.includes('+')" id="popover-so" class="so" > -->
            <span v-show="deltaScientificObjects.includes('+')" id="popover-so" class="so">

              <opensilex-Icon icon="bi#bi-bullseye"/>
              <span 
                ref="popoverTarget"
                class="stats-underline"
                tabindex="0"
                data-bs-toggle="popover"
                data-bs-trigger="hover focus"
                data-bs-placement="bottom"
                :data-bs-content="popoverContent"
              >
                {{ nbScientificObjects }} ({{ deltaScientificObjects }})
                <a href="/app/scientific-objects" :title="t('DataMonitoring.redirectionToOS')" class="metricsElementTitle">
                  {{ $t("component.menu.scientificObjects") }}
                </a>
              </span>
            </span>
            
            <!-- <span v-else>
              <opensilex-Icon icon="bi#bi-bullseye"/>
              <span>
                {{ nbScientificObjects }} ({{ deltaScientificObjects }}) 
                <a href="/app/scientific-objects" :title="t('DataMonitoring.redirectionToOS')" class="metricsElementTitle">
                  {{ $t("component.menu.scientificObjects") }}
                </a>
              </span>-->
            <!-- </span>  -->

            <!-- Données -->
            <span class="data">
              <opensilex-Icon icon="bi#bi-bar-chart"/>
              <span>
                {{ nbData }} ({{ deltaData }})
                <a href="/app/data" class="metricsElementTitle" :title="t('DataMonitoring.redirectionToData')"> 
                  {{ $t("component.menu.data.label") }}
                </a>
              </span>
            </span>

          
<!-- Sélection de la période -->
<span class="button-group">
  <div class="btn-group btn-group-toggle btnsGroup" data-toggle="buttons">
    <label 
      v-for="option in periods" 
      :key="option.value"
      class="btn periodBtn btn-toggle greenThemeColor"
      :class="{ 'period-selected': period === option.value }"

    >
      <!-- :class="{ active: period === option.value }" -->
      <input 
        type="radio" 
        name="options" 
        v-model="period" 
        :value="option.value"
        style="display: none;" 
      >
      {{ option.text }}
    </label>
  </div>
</span>

          </span>
        </div>
      </template>
    </opensilex-Card>
  </opensilex-Overlay>
</template>

<script lang="ts">
import { ref, computed, onMounted, watch, getCurrentInstance, nextTick, inject } from "vue";
import { Popover } from "bootstrap";
import { MetricsService } from "opensilex-core/api/metrics.service";
import { OpenSilexResponse } from "opensilex-core/HttpResponse";
import { MetricPeriodDTO } from "opensilex-core/model/metricPeriodDTO";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { useStore } from "vuex";
import { useI18n } from "vue-i18n";

export default {
  setup() {
    const instance = getCurrentInstance();
    const $opensilex= inject<OpenSilexVuePlugin>("$opensilex");
    const store = useStore();
    const { t } = useI18n(); 

    const nbScientificObjects = ref("0");
    const nbExperiments = ref("0");
    const nbData = ref("0");
    const deltaScientificObjects = ref("0");
    const deltaExperiments = ref("0");
    const deltaData = ref("0");
    const isSearching = ref(false);
    const scientificObjectTypes = ref<string[]>([]);
    const period = ref("day");

    const popoverTarget = ref<HTMLElement | null>(null);
const popoverContent = computed(() => {
    // console.log("💬 Contenu du popover :", scientificObjectTypes.value);
  if (scientificObjectTypes.value.length === 0) {
    return `<p>${t("DataMonitoring.noScientificObjectAdded")}</p>`;
  }

  return `<p>Types</p>
  <ul style="padding-left: 10px">
    ${scientificObjectTypes.value.map(item => `<li>${item}</li>`).join("")}
  </ul>`;
});

    const periods = computed(() => [
      { text: t("DataMonitoring.day"), value: "day" },
      { text: t("DataMonitoring.week"), value: "week" },
      { text: t("DataMonitoring.month"), value: "month" },
      { text: t("DataMonitoring.year"), value: "year" },
    ]);


// Calculer la somme directement sur la base des éléments de différence
const calculateTotalDifference = (items: { difference_count: number }[]) => {
  return items.reduce((sum, item) => sum + item.difference_count, 0);
};


    const loadMetrics = () => {
      if (!$opensilex) return;

      $opensilex.disableLoader();
      isSearching.value = true;

      const service: MetricsService = $opensilex.getService("opensilex.MetricsService");
service.getSystemMetricsSummary(period.value, 0, 20)
  .then((http: OpenSilexResponse<MetricPeriodDTO>) => {
      const result = http.response.result;
    // console.log("📝 Données  de l'API :", JSON.stringify(result, null, 2));

    // console.log("✅ Résultat API :", result);


        //     // Calcul de la somme des différences
        //     const calculatedDelta = calculateTotalDifference(result.scientific_object_list.difference_items);
        //     console.log("Delta calculé côté front :", calculatedDelta);

        //   console.log("delta affiché : ", deltaScientificObjects.value)
        //   if (calculatedDelta > 0) {
        //     scientificObjectTypes.value = getAddedTypes(result.scientific_object_list.difference_items);
        //   }




    // Vérifie les valeurs reçues
    // console.log("🔍 Nombre d'objets scientifiques :", result.scientific_object_list?.total_items_count);
    // console.log("🔍 Delta d'objets scientifiques :", result.scientific_object_list?.total_difference_item_count);

    try {
      nbScientificObjects.value = result.scientific_object_list?.total_items_count?.toLocaleString() || "0";
      deltaScientificObjects.value = formatDelta(result.scientific_object_list?.total_difference_item_count);

  // Vérifier s'il y a des ajouts, sinon réinitialiser la liste des types
  // console.log(" diff item count : ", result.scientific_object_list?.total_difference_item_count)
        if (result.scientific_object_list?.total_difference_item_count > 0) {
          scientificObjectTypes.value = getAddedTypes(result.scientific_object_list.difference_items);
        } else {
          scientificObjectTypes.value = []; // Réinitialise la liste
        }
      nbExperiments.value = result.experiment_list?.total_items_count?.toLocaleString() || "0";
      deltaExperiments.value = formatDelta(result.experiment_list?.total_difference_item_count);

      nbData.value = result.data_list?.total_items_count?.toLocaleString() || "0";
      deltaData.value = formatDelta(result.data_list?.total_difference_item_count);

    // console.log("📝 Valeurs après assignation :",
    //   "nbScientificObjects: ", nbScientificObjects.value,
    //   "deltaScientificObjects :", deltaScientificObjects.value,
    // );

      isSearching.value = false;
    } catch (err) {
      console.error("🚨 Erreur lors du traitement des données :", err);
    }
  })
  .catch((err) => {
    console.error("❌ Erreur lors de la récupération des données :", err);
    nbScientificObjects.value = "N/A";
    deltaScientificObjects.value = "N/A";
    nbExperiments.value = "N/A";
    deltaExperiments.value = "N/A";
    nbData.value = "N/A";
    deltaData.value = "N/A";
    isSearching.value = false;
  });
    }


const formatDelta = (count: number | null | undefined) => {
  if (count == null || isNaN(count)) {
    return "0"; // Retourne "0" si la valeur est null ou undefined
  }
  return count > 0 ? `+${count.toLocaleString()}` : count.toLocaleString();
};



const getAddedTypes = (listItems?: { name: string; difference_count: number }[]) => {
  if (!listItems) return [];
  const filteredItems = listItems
    .filter(item => item.difference_count > 0)
    .map(item => `${item.name.toLowerCase()}: ${item.difference_count}`)
    .sort();
  
  console.log("📝 Types d'objets scientifiques ajoutés :", filteredItems);
  return filteredItems;
};




    const initPopover = () => {
  nextTick(() => {
    if (!popoverTarget.value) {
      console.warn("PopoverTarget est toujours NULL, réessai dans 100ms...");
      setTimeout(initPopover, 100); // Nouvelle tentative après 100ms
      return;
    }

    console.log("✅ Élément trouvé, initialisation du popover...");
    new Popover(popoverTarget.value, {
      trigger: "hover",
      placement: "bottom",
      html: true,
    });
  });
};


    onMounted(() => {
      loadMetrics();

     // FORCER Bootstrap à détecter les popovers
  nextTick(() => {

        initPopover(); // Première tentative à l'affichage initial
        console.log("content: ", popoverContent)
  });

    });

    watch(deltaScientificObjects, (newValue) => {
      if (newValue.includes("+")) {
        initPopover();
      }
    });

    watch(deltaScientificObjects, (newValue) => {
  if (newValue.includes("+")) {
    nextTick(() => {
      initPopover();
    });
  }
});


    watch(period, loadMetrics);

    return {
      nbScientificObjects,
      nbExperiments,
      nbData,
      deltaScientificObjects,
      deltaExperiments,
      deltaData,
      isSearching,
      period,
      periods,
      popoverTarget,
      popoverContent,
      t,
    };
  },
};
</script>


<style scoped lang="scss">

.periodBtn {
  border-color: #018371;
  background: #fff;
  color: #018371;
}

// .active {
//   background-color: #00A38D;
//   border-color: #00A38D;
//   color: #fff;
// }

.period-selected {
  background-color: #00A38D;
  border-color: #00A38D;
  color: #fff;
}


.title {
  font-size: 1.5em;
  margin-right: 10px;
}

.stats-values {
  font-size: 1.6em;
  margin-left: 5%;
  display:flex;
  justify-content: space-evenly;
  list-style-type: none;

  span {
    margin-left: 5px;
    margin-right: 10px;
  }
}

.stats-underline {
  text-decoration-line: underline;
  text-decoration-style: dotted;
}

.button-group {
  float: right;
}

.so, .expe {
  margin: 0 10px 0 10px;
}

.metricsElementTitle  {
  color: #000 !important;
  cursor: pointer;
  text-decoration: underline;
}

.metricsElementTitle:hover {
  text-decoration: none !important;
  color: #018371 !important;
}

@media only screen and (max-width: 1451px){
  .button-group {
    float: left;
    display: inline-flex;
  }

  .stats-values {
    float: right;
    font-size: 1.3em;
    justify-items: center;
    list-style-type: none;
    margin-left: 0%;
    margin-right: 0%;
  }

}

.so {
    display: block !important;
}
</style>


<i18n>
en:
  DataMonitoring:
    title: Data monitoring
    redirectionToExpe: List of experiments
    redirectionToOS: List of scientific objects
    redirectionToData: Data list
    settings: Settings
    lastUpdated: update
    scientificObjetcTypes: Types
    noScientificObjectAdded: No recent changes
    day: Day
    week: Week
    month: Month
    year: Year


fr:
  DataMonitoring:
    title: Suivi de données
    redirectionToExpe: Liste des expérimentations
    redirectionToOS: Liste des objets scientifiques
    redirectionToData:  Liste des données
    settings: Paramètres
    lastUpdated: statut
    scientificObjetcTypes: Types
    noScientificObjectAdded: Aucun changement récent
    day: Jour
    week: Semaine
    month: Mois
    year: Année
</i18n>
