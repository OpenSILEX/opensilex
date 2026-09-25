<template>
  <b-modal
    @hide="clearModal"
    v-model="modalShow"
    :title="t('ResultModalView.title')"
  >
    <template v-slot:modal-header>
      <b-row class="mt-1" style="width: 100%">
        <b-col cols="10">
          <i>
            <h4>
              <Icon icon="fa#list" />
              {{ $t("ResultModalView.title") }}
            </h4>
          </i>
        </b-col>
      </b-row>
    </template>
    <template>
      <p v-if="nbLinesImported != null" class="validation-confirm-container">
        {{
          nbLinesImported > 1
            ? `${nbLinesImported} ${$t("ResultModalView.data-imported")}`
            : $t("ResultModalView.datum-imported")
        }}
      </p>
      <p v-if="nbAnnotationsImported != null && nbAnnotationsImported > 0" class="validation-confirm-container">
        {{
          nbAnnotationsImported > 1
            ? `${nbAnnotationsImported} ${$t("ResultModalView.annotations-imported")}`
            : $t("ResultModalView.annotation-imported")
        }}
      </p>
      <div class="details-container">
        <ProvenanceDetails
          label="ResultModalView.provenanceLabel"
          v-if="provenance"
          :provenance="provenance"
          :dataImportResult="true"
        />

        <BatchDetails
          label="ResultModalView.batchLabel"
          v-if="batch"
          :batchUri="batch"
        />
      </div>
    </template>

    <template v-slot:modal-footer> 
      <button
        type="button"
        class="btn greenThemeColor"
        v-on:click="hide()"
      >
        {{ $t('component.common.ok') }}
      </button>
    </template>
  </b-modal>
</template>

<script setup lang="ts">

  import ProvenanceDetails from "@/components/data/ProvenanceDetails.vue";
  import BatchDetails from "@/components/data/BatchDetails.vue";
  import Icon from "@/components/common/views/Icon.vue";
  import {useI18n} from "vue-i18n";
  import {ref} from "vue";

  const { t } = useI18n()
  const modalShow = ref<boolean>(false)
  const nbLinesImported = ref<number>(null)
  const nbAnnotationsImported = ref<number>(null)
  const provenance = ref<any>(null)
  const batch = ref<string>(null)

  const emit = defineEmits<{
    onHide: []
  }>()

  function setNbLinesImported(value: number) {
    nbLinesImported.value = value;
  }

  function setNbAnnotationsImported(value: number) {
      nbAnnotationsImported.value = value;
  }

  function setProvenance(value) {
    provenance.value = value;
  }

  function setBatch(value) {
    batch.value = value;
  }

  function show() {
    modalShow.value = true;
  }

  function hide() {
    modalShow.value = false;
  }


 function clearModal() {
    nbLinesImported.value = null;
    nbAnnotationsImported.value = null;
    provenance.value = null;
    emit("onHide");
  }

  defineExpose({
    setBatch,
    setNbAnnotationsImported,
    setNbLinesImported,
    setProvenance,
    show
  })

</script>

<style scoped>
.validation-confirm-container {
  color: rgb(40, 167, 69);
  font-weight: bold;
}

.details-container {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}
</style>
<i18n>
  fr: 
    ResultModalView:
      data-imported: observations ont été importées avec succès
      datum-imported: Observation importée avec succès
      title : Rapport de l'insertion des données 
      provenanceLabel : Provenance
      annotations-imported: annotations ont été importées avec succès
      annotation-imported: Annotation importée avec succès
      batchLabel : Batch

  en: 
    ResultModalView:
      data-imported: Observations have been imported successfully
      datum-imported: Observation imported successfully
      title : Data insertion report
      provenanceLabel : Provenance
      annotations-imported: annotations have been imported successfully
      annotation-imported: Annotation imported successfully
      batchLabel : Batch
</i18n>