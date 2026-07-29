<template>
  <VariableForm
    ref="variableForm"
    :createTitle="'component.variable.add'"
    :editTitle="'component.variable.edit'"
    @onCreate="onCreate"
    @onUpdate="onUpdate"
  />
</template>

<script setup lang="ts">
import { ref, inject } from 'vue';
import DTOConverter from '../../../models/DTOConverter';
import { VariableCreationDTO, VariableDetailsDTO, VariableUpdateDTO } from 'opensilex-core/index';
import VariableForm from './VariableForm.vue';

const emit = defineEmits<{
  (e: 'onCreate', variable: VariableCreationDTO): void;
  (e: 'onUpdate', variable: VariableUpdateDTO): void;
}>();

const variableForm = ref<InstanceType<typeof VariableForm>>();

function showCreateForm() {
  variableForm.value?.showCreateForm();
}

function showEditForm(form: VariableDetailsDTO, linkedDataNb?: number) {
  if (linkedDataNb !== undefined) {
    form.linked_data_nb = linkedDataNb;
  }
  const formCopy = DTOConverter.extractURIFromResourceProperties<VariableDetailsDTO, VariableUpdateDTO>(form);
  variableForm.value?.showEditForm(formCopy);
}

function onCreate(variable: VariableCreationDTO) {
  emit('onCreate', variable);
}

function onUpdate(variable: VariableUpdateDTO) {
  emit('onUpdate', variable);
}

defineExpose({
  showCreateForm,
  showEditForm,
});
</script>
