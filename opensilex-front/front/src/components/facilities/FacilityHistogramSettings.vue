<template>
  <Modal ref="modal">
    <template #header>
      <h4 class="modal-title">
        <Icon icon="fa#cog" />
        {{ t("component.common.graphs.settings") }}
      </h4>
    </template>

    <!-- Periods -->
    <DatePeriodPicker
      ref="periodPicker"
      v-model:selectedPeriod="period"
      @update="updateDatePeriod"
    >
    </DatePeriodPicker>

    <!-- Devices Selection -->
    <div class="mb-3">
      <label class="form-label">{{ t('component.common.graphs.filter') }}:</label>
      <div class="d-flex align-items-center">
        <input
          type="checkbox"
          class="form-check-input me-2"
          v-model="selectAll"
        />
        <span>{{ t('component.provenance.all_provenances') }}</span>
        <font-awesome-icon
          icon="question-circle"
          class="ms-2 text-muted"
          v-tooltip="t('component.common.graphs.help')"
        />
      </div>
    </div>

    <template #footer>
      <FormFooter submitLabel="component.common.apply" @cancel="hide()" @submit="validate()"></FormFooter>
    </template>
  </Modal>
</template>

<script setup lang="ts">

import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import {inject, onMounted, ref} from "vue";
import DatePeriodPicker, { Period } from "@/components/common/forms/DatePeriodPicker.vue";
import Icon from "@/components/common/views/Icon.vue";
import Modal from "@/components/common/views/Modal.vue";
import FormFooter from "@/components/common/forms/FormFooter.vue";

//#region Constant values
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const { t } = useI18n();
//#endregion

//#region Template refs
const modal = ref<InstanceType<typeof Modal> | null>(null);
const periodPicker = ref<InstanceType<typeof DatePeriodPicker> | null>(null);
//#endregion

//#region Reactive variables
const period = ref<Period>("week");
const startDate = ref<string>(null);
const endDate = ref<string>(null);
const selectAll = ref<boolean>(false);
//#endregion

//#region Emits definition
const emit = defineEmits<{
  (
    e: "update",
    period: Period,
    startDate: string,
    endDate: string,
    selectAll: boolean
  ): void
}>();
//#endregion

//#region Hooks
onMounted(() => {
  endDate.value = new Date().toISOString();
  let begin = new Date();
  begin.setDate(begin.getDate() - 7);
  startDate.value = begin.toISOString();
})
//#endregion

//#region Functions
function updateDatePeriod(begin: Date, end: Date) {
  startDate.value = begin.toISOString();
  endDate.value = end.toISOString();
}

function sendUpdate() {
  emit("update", period.value, startDate.value, endDate.value, selectAll.value);
}

const show = () => {
  modal.value?.show();
};

const hide = () => {
  modal.value?.hide();
};

const validate = () => {
  sendUpdate();
  hide();
};

//#endregion
//#region Exposed
defineExpose({ show, hide });
//#endregion
</script>

<style scoped lang="scss">

.periodBtn{
  border-color:var(--main-color-theme-hover);
  background: #fff;
  color: var(--main-color-theme-hover)
}

.active {
  background-color: var(--main-color-theme);
  border-color:var(--main-color-theme);
  color: #fff;
}

.selectAllDevicesBox {
  margin-left: 0;
}
.selectionHelp{
  margin-top: 7px;
  margin-left: 8px;
}
</style>

<i18n>
en:
  FacilityHistogramSettings:
    help: Uncheck to use the bellow selector

fr:
  FacilityHistogramSettings:
    help: Décochez pour utiliser le sélecteur ci-dessous

</i18n>
