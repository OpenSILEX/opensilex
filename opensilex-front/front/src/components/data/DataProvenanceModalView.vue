<template>
  <Modal ref="modalRef">
    <template #header>
      <div class="container-fluid w-100">
        <div class="row w-100 mt-1">
          <div class="col-10">
            <h4><opensilex-Icon icon="bi#bi-eye" /></h4>
          </div>
        </div>
      </div>
    </template>

    <h3>{{ datafile ? t('DataProvenanceModalView.datafile') : t('DataProvenanceModalView.data') }}</h3>
    <pre>{{ data }}</pre>
    <h3>Provenance</h3>
    <pre>{{ provenance }}</pre>
  </Modal>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import Modal from './../common/views/Modal.vue'

const props = defineProps({
  datafile: {
    type: Boolean,
    default: false
  }
});

// i18n
const { t } = useI18n();

// const modalRef = ref<InstanceType<typeof Modal> | null>(null);
const modalRef = ref<any>(null);
const data = ref('');
const provenance = ref('');

function show() {
  modalRef.value?.show();
}
function hide() {
  modalRef.value?.hide();
}

function setProvenance(value: { data: any; provenance: any }) {
  data.value = JSON.stringify(value.data, null, 2);
  provenance.value = JSON.stringify(value.provenance, null, 2);
}

defineExpose({
  show,
  hide,
  setProvenance
});
</script>

<i18n>
fr: 
  DataProvenanceModalView:
    data : Donnée
    datafile : Fichier de données

en: 
  DataProvenanceModalView:
    data : Data
    datafile : Datafile
</i18n>

<style scoped>
.modal.show {
  display: block;
  background-color: rgba(0, 0, 0, 0.5);
}
</style>
