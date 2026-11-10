<template>
  <Modal ref="modal">
    <template #header>
      <h4>
        <Icon icon="fa#cog" />
        {{ t("component.common.graphs.settings") }}
      </h4>
    </template>

    <!-- Periods -->
    <DatePeriodPicker
      v-model:selectedPeriod="period"
      @update="onPeriodPickerUpdate"
    >
    </DatePeriodPicker>

    <!-- Provenances selection -->
    <div class="mt-3">
      <label>{{ t('component.common.graphs.filter') }}:</label>
      <div class="d-flex align-items-center">
        <n-checkbox v-model:checked="selectAll">
          {{ t('component.provenance.all_provenances') }}
        </n-checkbox>
        <n-tooltip trigger="hover" placement="top">
          <template #trigger>
            <font-awesome-icon icon="question-circle" class="ms-2 text-muted"/>
          </template>
          {{ t('component.common.graphs.help') }}
        </n-tooltip>
      </div>
    </div>

    <template #footer>
      <FormFooter submitLabel="component.common.apply" @cancel="hide()" @submit="onFooterSubmit()"></FormFooter>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {onMounted, ref, Ref, useTemplateRef} from "vue";
import {useI18n} from "vue-i18n";
import {NCheckbox, NTooltip} from "naive-ui";
import DatePeriodPicker, {Period} from "@/components/common/forms/DatePeriodPicker.vue";
import Icon from "@/components/common/views/Icon.vue";
import Modal from "@/components/common/views/Modal.vue";
import FormFooter from "@/components/common/forms/FormFooter.vue";

/**
 * Settings of the graphic of a Monitoring tile: the displayed period, and whether the series of every provenance are
 * displayed. Open it with show(), the settings are only applied (update event) when the user clicks "Apply".
 */

//#region Public
const emit = defineEmits<{
  /**
   * The user applied the settings
   *
   * @param startDate start of the period, ISO date
   * @param endDate end of the period, ISO date
   * @param selectAll true to display the series of every provenance
   */
  update: [period: Period, startDate: string, endDate: string, selectAll: boolean]
}>();
//#endregion

//#region Private
//#region Plugins & services
const {t} = useI18n();
//#endregion

//#region Template refs
const modal = useTemplateRef<InstanceType<typeof Modal>>("modal");
//#endregion

//#region Data & Computed
const period: Ref<Period> = ref<Period>("week");
const startDate: Ref<string | undefined> = ref<string | undefined>(undefined);
const endDate: Ref<string | undefined> = ref<string | undefined>(undefined);
const selectAll: Ref<boolean> = ref<boolean>(false);
//#endregion

//#region Hooks & watchers
/**
 * Default period: the last week, until the period picker sends its dates
 */
onMounted(() => {
  const begin: Date = new Date();
  begin.setDate(begin.getDate() - 7);
  startDate.value = begin.toISOString();
  endDate.value = new Date().toISOString();
});
//#endregion

//#region Event handlers
function onPeriodPickerUpdate(begin: Date, end: Date): void {
  startDate.value = begin.toISOString();
  endDate.value = end.toISOString();
}

function onFooterSubmit(): void {
  emit("update", period.value, startDate.value, endDate.value, selectAll.value);
  hide();
}
//#endregion

//#region Methods
function show(): void {
  modal.value?.show();
}

function hide(): void {
  modal.value?.hide();
}
//#endregion
//#endregion

defineExpose({
  show,
  hide
});
</script>
