<template>


  <div class="modal fade" ref="modal" tabindex="-1">
    <div class="modal-dialog modal-md modal-dialog-centered">
      <div class="modal-content">

        <!-- Header -->
        <div class="modal-header">
          <h4 class="modal-title">
            <Icon icon="fa#cog" />
            {{ t("component.common.graphs.settings") }}
          </h4>
          <button type="button" class="btn-close" @click="hide"></button>
        </div>

        <!-- Body -->
        <div class="modal-body">
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

        </div>

        <!-- Footer -->
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="hide">
            {{ t('component.common.cancel') }}
          </button>
          <button class="btn greenThemeColor" @click="validate">
            {{ t('component.common.apply') }}
          </button>
        </div>

      </div>
    </div>
  </div>

</template>

<script setup lang="ts">

import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import {inject, onMounted, ref} from "vue";
import DatePeriodPicker, { Period } from "@/components/common/forms/DatePeriodPicker.vue";
import Icon from "@/components/common/views/Icon.vue";

//#region Constant values
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const { t } = useI18n();
//#endregion

//#region Template refs
const modal = ref<HTMLDivElement | null>(null);
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

//#endregion
//#region Exposed
defineExpose({ show, hide });
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
