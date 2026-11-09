<template>
  <Modal ref="modal">
    <template #header>
      <h4>
        <Icon icon="fa#cog"/>
        {{ t("component.common.graphs.settings") }}
      </h4>
    </template>

    <n-form label-placement="top">
      <!-- Period -->
      <n-form-item :label="t('component.common.graphs.display_by')">
        <DatePeriodPicker
            v-model:selectedPeriod="selectedPeriod"
            :doInclude6MonthOption="false"
            :doIncludeYearOption="true"
        ></DatePeriodPicker>
      </n-form-item>

      <!-- Devices -->
      <n-form-item :label="t('component.common.graphs.filter')">
        <div class="devicesFilter">
          <n-checkbox v-model:checked="allDevices" :title="t('component.common.graphs.help')">
            {{ t("component.device.all_devices") }}
          </n-checkbox>
          <n-select
              v-model:value="selectedDevices"
              :options="deviceOptions"
              :disabled="allDevices"
              :placeholder="t('component.device.select_devices')"
              multiple
              filterable
          ></n-select>
        </div>
      </n-form-item>
    </n-form>

    <template #footer>
      <FormFooter submitLabel="component.common.apply" @cancel="hide()" @submit="onApply()"></FormFooter>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {computed, ComputedRef, ref, Ref, useTemplateRef} from "vue";
import {useI18n} from "vue-i18n";
import {NCheckbox, NForm, NFormItem, NSelect, SelectOption} from "naive-ui";
import {DeviceGetDTO} from "opensilex-core/index";
import Modal from "@/components/common/views/Modal.vue";
import FormFooter from "@/components/common/forms/FormFooter.vue";
import DatePeriodPicker, {Period} from "@/components/common/forms/DatePeriodPicker.vue";
import Icon from "@/components/common/views/Icon.vue";

/**
 * Settings of the dashboard graphic: the displayed period, and the displayed devices (all of them by default).
 * Open it with show(), the settings are only applied (validate event) when the user clicks "Apply".
 */

//#region Public
const emit = defineEmits<{
  /**
   * The user applied the settings
   *
   * @param devices URIs of the selected devices, undefined for all the devices
   */
  validate: [period: Period, devices: string[] | undefined]
}>();

const props = defineProps<{
  /** Devices that can be selected */
  devices: DeviceGetDTO[]
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
const selectedPeriod: Ref<Period> = ref<Period>("day");
/** true to display all the devices, the selector is then disabled */
const allDevices: Ref<boolean> = ref<boolean>(true);
const selectedDevices: Ref<string[]> = ref<string[]>([]);

const deviceOptions: ComputedRef<SelectOption[]> = computed<SelectOption[]>(() =>
    props.devices.map((device: DeviceGetDTO) => ({value: device.uri, label: device.name}))
);
//#endregion

//#region Event handlers
/**
 * Without any selected device, all the devices are displayed
 */
function onApply(): void {
  const devices: string[] | undefined = allDevices.value || selectedDevices.value.length === 0
      ? undefined
      : selectedDevices.value;
  emit("validate", selectedPeriod.value, devices);
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

<style scoped lang="scss">
.devicesFilter {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
</style>
