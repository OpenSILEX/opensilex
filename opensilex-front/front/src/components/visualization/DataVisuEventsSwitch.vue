<template>
  <n-form-item :label="t('DataVisualisation.showEvents')" class="compact-form-item">
    <n-switch v-model:value="showEvents"></n-switch>
    <n-spin v-if="loading" :size="14" class="eventsCount"></n-spin>
    <n-badge v-else :value="count" :show-zero="true" type="info" class="eventsCount"></n-badge>
  </n-form-item>
</template>

<script setup lang="ts">
import {inject, ref, Ref, watch} from "vue";
import {useI18n} from "vue-i18n";
import {NBadge, NFormItem, NSpin, NSwitch} from "naive-ui";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import useDataVisualisation from "@/composables/useDataVisualisation";

/**
 * "Show events" switch of the visualization filters, with the number of events of the targets between the dates next
 * to it. The number is counted again whenever the targets or the dates change, or when refresh() is called (e.g.
 * after an event was created from the graphic).
 */

//#region Public
const props = defineProps<{
  /** URIs of the elements whose events are counted (scientific objects, devices...) */
  targets: string[]
  startDate?: string
  endDate?: string
}>();

/** true to draw the events on the graphic */
const showEvents = defineModel<boolean>({required: true});
//#endregion

//#region Private
//#region Plugins & services
const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const {t} = useI18n();
const {countEvents} = useDataVisualisation();
//#endregion

//#region Data & Computed
const count: Ref<number> = ref<number>(0);
const loading: Ref<boolean> = ref<boolean>(false);
//#endregion

//#region Hooks & watchers
// The targets are compared by value: the parent may pass a new array with the same URIs at each render
watch([() => props.targets.join(","), () => props.startDate, () => props.endDate], refresh, {immediate: true});
//#endregion

//#region Methods
async function refresh(): Promise<void> {
  loading.value = true;
  try {
    count.value = await countEvents(props.targets, props.startDate, props.endDate);
  } catch (error) {
    $opensilex.errorHandler(error);
  } finally {
    loading.value = false;
  }
}
//#endregion
//#endregion

defineExpose({
  refresh
});
</script>

<style scoped lang="scss">
.eventsCount {
  margin-left: 8px;
}
</style>
