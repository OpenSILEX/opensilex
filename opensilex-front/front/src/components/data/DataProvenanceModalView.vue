<template>
  <n-modal
    v-model:show="modalShow"
    preset="card"
    size="huge"
    :title="t('DataProvenanceModalView.title')"
    style="width: 900px; max-width: 95vw"
  >
    <template #header>
      <div class="modalHeader">
        <div class="modalHeaderLeft">
          <i>
            <h4 class="modalTitle">
              <opensilex-Icon icon="fa#eye" />
            </h4>
          </i>
        </div>
        <div class="modalHeaderRight">
          <button
            type="button"
            class="closeButton"
            @click="hide"
            aria-label="Close"
          >
            <span aria-hidden="true">&times;</span>
          </button>
        </div>
      </div>
    </template>

    <div class="modalBody">
      <h3 v-if="datafile">{{ t('DataProvenanceModalView.datafile') }}</h3>
      <h3 v-else>{{ t('DataProvenanceModalView.data') }}</h3>
      <pre>{{ data }}</pre>

      <h3>Provenance</h3>
      <pre>{{ provenance }}</pre>

      <h3 v-if="batch">Batch</h3>
      <pre v-if="batch">{{ batch }}</pre>
    </div>

    <template #action>
      <div class="modal-footer-right">
        <n-button class="greenThemeColor" @click="hide">
          {{ t('component.common.close') }}
        </n-button>
      </div>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NModal, NButton } from 'naive-ui'

const { t } = useI18n()

const props = withDefaults(defineProps<{
  datafile?: boolean
}>(), {
  datafile: false
})

const modalShow = ref(false)

const info = ref<any>(null)
const data = ref<string | null>(null)
const provenance = ref<string | null>(null)
const batch = ref<string | null>(null)

function setProvenanceAndBatch(value: any) {
  data.value = JSON.stringify(value.data, null, 2)
  provenance.value = JSON.stringify(value.provenance, null, 2)
  batch.value = value.batch ? JSON.stringify(value.batch, null, 2) : null
}

function show() {
  modalShow.value = true
}

function hide() {
  modalShow.value = false
}

defineExpose({
  setProvenanceAndBatch,
  show,
  hide
})
</script>

<style scoped>
.modalHeader {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modalHeaderLeft {
  display: flex;
  align-items: center;
}

.modalHeaderRight {
  display: flex;
  align-items: center;
}

.modalTitle {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.modalBody pre {
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow: auto;
}

.closeButton {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
  padding: 0;
}

.modal-footer-right {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}
</style>

<i18n>
fr:
  DataProvenanceModalView:
    title: Métadonnées du fichier
    data: Donnée
    datafile: Fichier de données

en:
  DataProvenanceModalView:
    title: File metadata
    data: Data
    datafile: Datafile
</i18n>