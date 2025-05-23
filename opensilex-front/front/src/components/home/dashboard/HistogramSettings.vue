<template>
  <div class="modal fade" ref="modal" tabindex="-1">
    <div class="modal-dialog modal-md modal-dialog-centered">
      <div class="modal-content">
        
        <!-- Header -->
        <div class="modal-header">
          <h4 class="modal-title">
            <opensilex-Icon icon="fa#cog" />
            {{ t("HistogramSettings.title") }}
          </h4>
          <button type="button" class="btn-close" @click="hide"></button>
        </div>

        <!-- Body -->
        <div class="modal-body">
          <!-- Periods -->
          <div class="mb-3">
            <label class="form-label">{{ t('HistogramSettings.display_by') }}:</label>
            <div class="btn-group w-100">
              <button
                v-for="(period, index) in periods"
                :key="index"
                type="button"
                class="btn periodBtn"
                :class="{ active: selectedPeriod === period.value }"
                @click="selectedPeriod = period.value"
              >
                {{ period.text }}
              </button>
            </div>
          </div>

          <!-- Devices Selection -->
          <div class="mb-3">
            <label class="form-label">{{ t('HistogramSettings.filter') }}:</label>
            <div class="d-flex align-items-center">
              <input 
                type="checkbox" 
                class="form-check-input me-2"
                v-model="selectAll"
                @change="changeSelectorAccess"
              />
              <span>{{ t('HistogramSettings.all_devices') }}</span>
              <font-awesome-icon 
                icon="question-circle" 
                class="ms-2 text-muted"
                v-tooltip="t('HistogramSettings.help')"
              />
            </div>
          </div>

          <!-- Devices selector -->
          <!-- <opensilex-FormSelector
            :multiple="true"
            v-model="selectedDevices"
            :disabled="selectorAccess"
            :options="devicesLoaded"
            placeholder="HistogramSettings.select_devices"
          /> -->
        </div>

        <!-- Footer -->
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="hide">
            {{ t('HistogramSettings.cancel') }}
          </button>
          <button class="btn greenThemeColor" @click="validate">
            {{ t('HistogramSettings.apply') }}
          </button>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed  } from "vue";
import { useI18n } from "vue-i18n";

// i18n
const { t } = useI18n();


// Props & Emits
const props = defineProps<{
  period: string;
  devicesLoaded: Array<{ id: string; label: string }>;
}>();

const emit = defineEmits(["update"]);


// Modal Control
const modal = ref<HTMLDivElement | null>(null);

// State
const selectedPeriod = ref(props.period);
const selectAll = ref(true);
const selectorAccess = ref(true);
const selectedDevices = ref<string[]>([]);
const devicesToDisplay = ref<Array<{ label: string; id: string }>>([]);

// Periods List
const periods = computed(() => [
  { text: t("HistogramSettings.day"), value: "day" },
  { text: t("HistogramSettings.week"), value: "week" },
  { text: t("HistogramSettings.month"), value: "month" },
  { text: t("HistogramSettings.year"), value: "year" },
]);

// Methods
const changeSelectorAccess = () => {
  selectorAccess.value = !selectorAccess.value;
};


const sendUpdate = () => {
  if (selectAll.value) {
    selectedDevices.value = [];
    emit("update", props.devicesLoaded, selectedPeriod.value);
  } else {
    devicesToDisplay.value = props.devicesLoaded
      .filter(device => selectedDevices.value.includes(device.id))
      .map(device => ({ label: device.label, id: device.id }));

    emit("update", devicesToDisplay.value.length ? devicesToDisplay.value : props.devicesLoaded, selectedPeriod.value);
  }
  console.log("selected devices to display ", selectedDevices)
};

const show = () => {
  modal.value?.classList.add("show");
  modal.value?.setAttribute("style", "display: block;");
};

const hide = () => {
  modal.value?.classList.remove("show");
  modal.value?.setAttribute("style", "display: none;");
};

const validate = () => {
  sendUpdate();
  hide();
};

// important ! avec script setup les methodes ne sont pas exposées automatiquement au parent via ref()
defineExpose({ show, hide });

</script>

<style scoped>
.periodBtn {
  border-color: #018371;
  background: #fff;
  color: #018371;
  flex: 1;
}

.periodBtn.active {
  background-color: #00A38D;
  border-color: #00A38D;
  color: #fff;
}
</style>


<i18n>
en:
  HistogramSettings:
    title : Graphic settings
    apply: Apply
    cancel: Cancel
    display_by: Display by
    filter: Filter selection
    all_devices: All Devices
    select_devices: Select Devices
    hour: Hour
    day: Day
    week: Week
    month: Month
    year: Year
    help: Uncheck to use bellow selector

fr:
  HistogramSettings:
    title : Paramètres du graphique
    apply: Appliquer
    cancel: Annuler
    display_by: Afficher par
    filter: Filtrer la selection
    all_devices: Tout les appareils
    select_devices: Choisir des appareils
    hour: Heure
    day: Jour
    week: Semaine
    month: Mois
    year: année
    help: Décochez pour utiliser le sélecteur ci-dessous

</i18n>

