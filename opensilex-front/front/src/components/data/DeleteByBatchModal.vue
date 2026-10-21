<template>
  <n-form
      ref="validatorRef">
    <Modal
        ref="modal"
    >
      <template #header>
        <i>
          <h4>
            <Icon icon="fa#trash-alt"/>
            {{ t("component.data.deleteByBatchModal.title") }}
          </h4>
        </i>
      </template>
      <!-- Batch URI -->
      <InputForm
          v-model:value="batchUri"
          label="component.data.deleteByBatchModal.batch-uri"
          type="text"
          :required="true"
          :placeholder="t('component.data.deleteByBatchModal.batch-placeholder')"
      ></InputForm>
      <template #footer>
        <button
            type="button"
            class="btn btn-secondary"
            @click="hide"
        >{{ t('component.common.close') }}
        </button>
        <button
            type="button"
            class="btn btn-danger"
            @click="deleteData()"
        >{{ t('component.common.delete') }}
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
const dataService = opensilex.getService<DataService>("opensilex.DataService")
const batchUri = ref<string>('')
//#endregion
//#region: Refs
const modal = useTemplateRef<InstanceType<typeof Modal>>('modal')
const validatorRef = useTemplateRef<InstanceType<typeof NForm>>('validatorRef')
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
  try {
    await validatorRef.value.validate();
  } catch {
    return;
  }
  if (!batchUri.value) {
    return;
  }

  let result = (await dataService.deleteDataOnSearch(
      props.experimentUri,
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
    opensilex.showSuccessToast(deletedCount + "  " + t("component.data.deleteByBatchModal.success-message"));
    emitDeleted();
  } else {
    opensilex.showErrorToast(t("component.data.deleteByBatchModal.error-message"));
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
