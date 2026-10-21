<template>
  <n-modal v-model:show="showModal" :mask-closable="true">
    <n-card
      style="width: 700px; max-width: 95vw"
      :bordered="false"
      role="dialog"
      aria-modal="true"
    >
      <template #header>
        <div class="d-flex align-items-center gap-2">
          <opensilex-Icon icon="bi#bi-download" />
          <h4 class="mb-0">{{ t('component.datafile.export.export') }}</h4>
        </div>
      </template>

      <div class="container-fluid px-0">
        <div class="row">
          <div class="col-md-4">
            <n-form-item :label="t('component.datafile.export.format')">
              <n-radio-group v-model:value="format">
                <n-space vertical>
                  <n-radio value="dx">DX</n-radio>
                  <n-radio value="tsv">TSV</n-radio>
                  <n-radio value="csv">CSV</n-radio>
                  <n-radio value="img">Image</n-radio>
                </n-space>
              </n-radio-group>
            </n-form-item>
          </div>

          <div class="col-md-4">
            <opensilex-CheckboxForm
              v-if="format === 'tsv'"
              :value="includeAverage"
              :helpMessage="t('component.datafile.export.includeAverage-help')"
              :label="t('component.datafile.export.includeAverage')"
              :title="t('component.datafile.export.includeAverage-title')"
              @update:value="includeAverage = $event"
            />
          </div>

          <div class="col-md-4">
            <opensilex-CheckboxForm
              v-if="format === 'tsv'"
              :value="includeSampleDatetime"
              :helpMessage="t('component.datafile.export.includeSampleDatetime-help')"
              :label="t('component.datafile.export.includeSampleDatetime')"
              :title="t('component.datafile.export.includeSampleDatetime-title')"
              @update:value="includeSampleDatetime = $event"
            />
          </div>
        </div>
      </div>

      <template #footer>
        <div class="d-flex justify-content-end">
          <button
            type="button"
            class="btn btn-secondary"
            data-bs-dismiss="modal"
            @click="$emit(hide())"
          >
            {{ t('component.common.cancel') }}
          </button>
          <n-button type="info" ghost @click="handleExport">
            Exporter
          </n-button>
        </div>
      </template>
    </n-card>
  </n-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  NModal,
  NCard,
  NButton,
  NFormItem,
  NRadioGroup,
  NRadio,
  NSpace
} from 'naive-ui'

const emit = defineEmits<{
  (e: 'onCreate', format: string, includeAverage: boolean, includeSampleDatetime: boolean): void
}>()

const { t } = useI18n()
const showModal = ref(false)
const format = ref('tsv')
const includeAverage = ref(false)
const includeSampleDatetime = ref(false)

function show() {
  showModal.value = true
}

function hide() {
  showModal.value = false
}

function handleExport() {
  emit('onCreate', format.value, includeAverage.value, includeSampleDatetime.value)
  hide()
}

defineExpose({
  show,
  hide
})
</script>

<style scoped>
.validation-confirm-container {
  color: rgb(40, 167, 69);
  font-weight: bold;
}
</style>

