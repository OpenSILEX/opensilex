<template>
  <n-form
      ref="validatorRef">
    <Modal
        ref="modal"
        :title="$t('DeleteByBatchModal.title')"
        no-close-on-backdrop
        no-close-on-esc
    >
      <template v-slot:modal-header>
        <b-row class="mt-1" style="width: 100%">
          <b-col cols="10">
            <i>
              <h4>
                <Icon icon="fa#trash-alt"/>
                {{ $t("DeleteByBatchModal.title") }}
              </h4>
            </i>
          </b-col>
        </b-row>
      </template>
      <template>
        <!-- Batch URI -->
        <InputForm
            :value.sync="batchUri"
            label="DeleteByBatchModal.batch-uri"
            type="text"
            :required="true"
            placeholder="DeleteByBatchModal.batch-placeholder"
        ></InputForm>
      </template>
      <template v-slot:modal-footer>
        <button
            type="button"
            class="btn btn-secondary"
            v-on:click="hide"
        >{{ $t('component.common.close') }}
        </button>
        <button
            type="button"
            class="btn btn-danger"
            v-on:click="deleteData()"
        >{{ $t('component.common.delete') }}
        </button>
      </template>
    </Modal>
  </n-form>
</template>

<script setup lang="ts">
import {DataService} from "opensilex-core/api/data.service";
import {NForm} from "naive-ui";
import Icon from "@/components/common/views/Icon.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import {useStore} from "vuex";
import {inject, ref, useTemplateRef} from "vue";
import Modal from "@/components/common/views/Modal.vue";

//#region: Props

const props = defineProps<{
  experimentUri: string
}>()

//#endregion

//#region: Data
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const {t} = useI18n()
const store = useStore()
const dataService = ref<DataService>('dataService')
const batchUri = ref<string>('')
//#endregion
//#region: Refs
const modal = useTemplateRef<InstanceType<typeof Modal>>('modal')
const validatorRef = useTemplateRef<InstanceType<typeof NForm>>('validatorRef')
//#endregion
//#region: hooks

function created()
{
  dataService.value = opensilex.getService<DataService>("opensilex.DataService");
}

//#endregion
//#region: Methods

function show()
{
  modal.value.show();
}

function hide()
{
  modal.value.hide();
}

 async function deleteData()
{
  const formIsValid = await validatorRef.value.validate();
  if (!formIsValid) {
    return;
  }

  let result = (await dataService.value.deleteDataOnSearch(
      this.experimentUri,
      undefined,
      undefined,
      undefined,
      batchUri.value
  )).response.result as any;
  hide();
  let deletedCount: number = 0;
  if (result.deletedCount) {
    deletedCount = result.deletedCount;
  }
  if (deletedCount > 0) {
    opensilex.showSuccessToast(deletedCount + "  " + t("DeleteByBatchModal.success-message"));
    emitDeleted();
  } else {
    opensilex.showErrorToast(t("DeleteByBatchModal.error-message"));
  }
}
//#endregion
//#region event emits
const emit = defineEmits<{
  deleted: []
}>()

function emitDeleted()
{
  emit("deleted");
}

//#endregion

defineExpose({
  show
})
</script>

<i18n>
en:
  DeleteByBatchModal:
    title: Delete by Batch
    batch-uri: Batch URI
    batch-placeholder: Enter URI
    success-message: data have been deleted.
    error-message: No data found.

fr:
  DeleteByBatchModal:
    title: Suppression par batch
    batch-uri: URI de Batch
    batch-placeholder: Entrez URI
    success-message: données ont étés supprimés.
    error-message: Pas de données trouvées
</i18n>