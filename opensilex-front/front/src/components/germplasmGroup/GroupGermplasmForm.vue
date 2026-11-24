<template>
  <Modal ref="modalRef" modalSize="lg">
    <template #header>
      <FormHeader :title="formTitle" icon="fa#folder" />
    </template>

    <n-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-placement="top"
    >
      <n-form-item>
        <UriForm
          v-model:uri="form.uri"
          label="component.group.group-uri"
          helpMessage="component.common.uri-help-message"
          :editMode="isEditMode"
          v-model:generated="uriGenerated"
        />
      </n-form-item>

      <n-form-item path="name">
        <InputForm
          v-model:value="form.name"
          label="component.common.name"
          type="text"
          :required="true"
          :placeholder="t('component.group.form-name-placeholder')"
        />
      </n-form-item>

      <n-form-item path="description">
        <TextAreaForm
          v-model:value="form.description"
          label="component.common.description"
          :placeholder="t('component.group.form-description-placeholder')"
        />
      </n-form-item>

      <n-form-item>
        <GermplasmSelectorWithFilter
          v-model:germplasmUris="form.germplasm_list"
          v-model="germplasmsWithLabels"
          :editMode="isEditMode"
        />
      </n-form-item>
    </n-form>

    <template #footer>
      <FormFooter
        @cancel="hide"
        @submit="submit"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import { computed, inject, ref, useTemplateRef } from 'vue';
import { useI18n } from 'vue-i18n';
import { NForm, NFormItem } from 'naive-ui';
import { GermplasmService } from 'opensilex-core/lib';
import type { GermplasmGroupUpdateDTO } from 'opensilex-core/index';
import OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import Modal from '@/components/common/views/Modal.vue';
import FormHeader from '@/components/common/forms/FormHeader.vue';
import FormFooter from '@/components/common/forms/FormFooter.vue';
import InputForm from '@/components/common/forms/InputForm.vue';
import UriForm from '@/components/common/forms/UriForm.vue';
import TextAreaForm from '@/components/common/forms/TextAreaForm.vue';
import GermplasmSelectorWithFilter from '../germplasm/GermplasmSelectorWithFilter.vue';

//#region Public
const { t } = useI18n();
const emit = defineEmits<{
  (event: 'onCreate'): void;
  (event: 'onUpdate', uri: string): void;
}>();
//#endregion

//#region Private
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const service = opensilex.getService<GermplasmService>('opensilex.GermplasmService');
const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef');
const formRef = useTemplateRef<InstanceType<typeof NForm>>('formRef');
const uriGenerated = ref(true);
const isEditMode = ref(false);
const germplasmsWithLabels = ref<Array<{ uri: string; name?: string }>>([]);
const form = ref({
  uri: undefined as string | undefined,
  name: '',
  description: '',
  germplasm_list: [] as string[],
});
const formTitle = computed(() =>
  t(isEditMode.value ? 'GermplasmGroupForm.edit' : 'GermplasmGroupForm.add'),
);
const rules = computed(() => ({
  name: {
    required: true,
    message: t('component.common.required'),
    trigger: ['input', 'blur'],
  },
}));

function showCreateForm() {
  isEditMode.value = false;
  form.value = {
    uri: undefined,
    name: '',
    description: '',
    germplasm_list: [],
  };
  germplasmsWithLabels.value = [];
  uriGenerated.value = true;
  modalRef.value?.show();
}

function showEditForm(
  group: GermplasmGroupUpdateDTO,
  germplasms: Array<{ uri: string; name?: string }>,
) {
  isEditMode.value = true;
  germplasmsWithLabels.value = germplasms;
  form.value = {
    uri: group.uri,
    name: group.name ?? '',
    description: group.description ?? '',
    germplasm_list: group.germplasm_list ?? [],
  };
  uriGenerated.value = false;
  modalRef.value?.show();
}

function hide() {
  modalRef.value?.hide();
}

async function submit() {
  try {
    await formRef.value?.validate();
    if (isEditMode.value) {
      await service.updateGermplasmGroup(form.value);
      opensilex.showSuccessToast(form.value.name + t('component.common.success.update-success-message'));
      emit('onUpdate', form.value.uri!);
    } else {
      await service.createGermplasmGroup(form.value);
      opensilex.showSuccessToast(form.value.name + t('component.common.success.creation-success-message'));
      emit('onCreate');
    }
    hide();
  } catch (error) {
    opensilex.errorHandler(error);
  }
}

//#endregion

defineExpose({
  showCreateForm,
  showEditForm,
  hide,
});
</script>

<i18n>
en:
  GermplasmGroupForm:
    add: Add germplasm group
    edit: Edit germplasm group
fr:
  GermplasmGroupForm:
    add: Ajouter un groupe de ressources génétiques
    edit: Éditer un groupe de ressources génétiques
</i18n>
