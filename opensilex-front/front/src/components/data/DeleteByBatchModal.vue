<template>
  <n-form
      ref="validatorRef"
      :model="form"
      :rules="rules">
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
      <n-form-item path="batchUri">
        <InputForm
            v-model:value="form.batchUri"
            label="component.data.deleteByBatchModal.batch-uri"
            type="text"
            :required="true"
            :placeholder="t('component.data.deleteByBatchModal.batch-placeholder')"
        ></InputForm>
      </n-form-item>
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
import {NForm, NFormItem} from "naive-ui";
import Icon from "@/components/common/views/Icon.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import {useStore} from "vuex";
import {computed, inject, ref, useTemplateRef} from "vue";
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
const form = ref({batchUri: ''})
//#endregion
//#region: Rules
const rules = computed(() => ({
  batchUri: {
    required: true,
    message: t('validations.required_if', {_field_: t('component.data.deleteByBatchModal.batch-uri')}),
    trigger: ['change', 'blur']
  }
}))
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

  let result = (await dataService.deleteDataOnSearch(
      props.experimentUri,
      undefined,
      undefined,
      undefined,
      form.value.batchUri
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
