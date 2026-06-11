<template>
  <div>
    <div class="btn-group btn-group-toggle btnsGroup" data-toggle="buttons" :options="periods">
      <!-- day -->
      <label class="btn periodBtn btn-toggle greenThemeColor"
             :class="{
            active: selectedPeriodComputed == 'day'
          }"
      >
        <input
          type="radio"
          name="options"
          id="option1"
          value="day"
          checked
          v-model="selectedPeriodComputed"
        >
        {{t('component.common.date-time-stuff.day')}}
      </label>

      <!-- week -->
      <label class="btn periodBtn btn-toggle greenThemeColor"
             :class="{
            active: selectedPeriodComputed == 'week'
          }"
      >
        <input
          type="radio"
          name="options"
          id="option2"
          value="week"
          v-model="selectedPeriodComputed"
        >
        {{t('component.common.date-time-stuff.Week')}}
      </label>

      <!-- month -->
      <label class="btn periodBtn btn-toggle greenThemeColor"
             :class="{
            active: selectedPeriodComputed == 'month'
          }"
      >
        <input
          type="radio"
          name="options"
          id="option3"
          value="month"
          v-model="selectedPeriodComputed"
        >
        {{t('component.common.date-time-stuff.month')}}
      </label>

      <!-- 6 month -->
      <label class="btn periodBtn btn-toggle greenThemeColor"
             :class="{
            active: selectedPeriodComputed == '6month'
          }"
      >
        <input
          type="radio"
          name="options"
          id="option4"
          value="6month"
          v-model="selectedPeriodComputed"
        >
        {{t('component.common.date-time-stuff.period-descriptions.6month')}}
      </label>
    </div>
  </div>
</template>

<script setup lang="ts">

import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {computed, inject, onMounted, ref, watch} from "vue";
import {useI18n} from "vue-i18n";

//#region Constant declarations
export type Period = "day" | "week" | "month" | "6month" | "year";

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;

const { t } = useI18n()
//#endregion

//#region Props

interface Props{
  selectedPeriod: Period
}

const props = defineProps<Props>();

//#endregion

//#region Emits
const emit = defineEmits<{
  (e: 'update:selectedPeriod', value: Period): void,
  (e: 'update', value1: Date, value2: Date)
}>();
//#endregion

//#region Reactive Data
const startDate = ref<Date>(null);
const endDate = ref<Date>(null);
//#endregion

//#region Computed
const selectedPeriodComputed = computed({
  get: () => props.selectedPeriod,
  set: (value: Period) => emit('update:selectedPeriod', value)
});

const periods = computed(() => {
  return [
    { text: t("component.common.date-time-stuff.day"), value: 'day'},
    { text: t("component.common.date-time-stuff.Week"), value: 'week'},
    { text: t("component.common.date-time-stuff.month"), value: 'month'},
    { text: t("component.common.date-time-stuff.period-descriptions.6month"), value: '6month'},
    { text: t("component.common.date-time-stuff.year"), value: 'year'},
  ];
})
//#endregion

//#region Hooks
onMounted(() => {
  endDate.value = new Date();
  updateDatePeriod();
})
//#endregion

//#region Watchers

watch(selectedPeriodComputed, async (newSelectedPeriod, old) => {
  emit("update", startDate.value, endDate.value);
})
//#endregion

//#region functions
function updateDatePeriod() {
  startDate.value = new Date();

  if (selectedPeriodComputed.value === "day") {
    startDate.value.setDate(endDate.value.getDate() - 1);
  }
  else if (selectedPeriodComputed.value === "week") {
    startDate.value.setDate(endDate.value.getDate() - 7);
  }
  else if (selectedPeriodComputed.value === "month") {
    startDate.value.setMonth(endDate.value.getMonth() - 1);
  }
  else if (selectedPeriodComputed.value === "6month") {
    startDate.value.setMonth(endDate.value.getMonth() - 6);
  }
  else if (selectedPeriodComputed.value === "year") {
    startDate.value.setMonth(endDate.value.getMonth() - 12);
  }
}
//#endregion

</script>

<style scoped lang="scss">

.periodBtn{
  border-color:#018371;
  background: #fff;
  color: #018371
}

.active {
  background-color: #00A38D;
  border-color:#00A38D;
  color: #fff;
}


</style>

