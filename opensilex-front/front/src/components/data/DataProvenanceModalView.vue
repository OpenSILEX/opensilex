<template>
  <Modal ref="modal" modal-size="lg">
    <template #header>
      <i>
        <h4>
          <Icon icon="fa#eye" />
        </h4>
      </i>
    </template>

    <div class="modalBody">
      <h3 v-if="datafile">{{ t('component.datafile.provenance-modal.datafile') }}</h3>
      <h3 v-else>{{ t('component.datafile.provenance-modal.data') }}</h3>
      <pre>{{ data }}</pre>

      <h3>Provenance</h3>
      <pre>{{ provenance }}</pre>

      <h3 v-if="batch">Batch</h3>
      <pre v-if="batch">{{ batch }}</pre>
    </div>

    <template #footer>
      <button
        type="button"
        class="btn greenThemeColor"
        @click="hide()"
      >{{ t('component.common.close') }}</button>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import Icon from "@/components/common/views/Icon.vue";
import Modal from "@/components/common/views/Modal.vue";

const { t } = useI18n()

const props = withDefaults(defineProps<{
  datafile?: boolean
}>(), {
  datafile: false
})

const modal = ref<InstanceType<typeof Modal> | null>(null)

const data = ref<string | null>(null)
const provenance = ref<string | null>(null)
const batch = ref<string | null>(null)

function setProvenanceAndBatch(value: any) {
  data.value = JSON.stringify(value.data, null, 2)
  provenance.value = JSON.stringify(value.provenance, null, 2)
  batch.value = value.batch ? JSON.stringify(value.batch, null, 2) : null
}

function show() {
  modal.value?.show()
}

function hide() {
  modal.value?.hide()
}

defineExpose({
  setProvenanceAndBatch,
  show,
  hide
})
</script>

<style scoped>
.modalBody pre {
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow: auto;
}
</style>
