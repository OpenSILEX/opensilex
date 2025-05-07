<template>
<div> variableCreate component</div>
  <!-- <opensilex-ModalForm
    v-if="user.value?.hasCredential(credentials.value?.CREDENTIAL_VARIABLE_MODIFICATION_ID) && loadForm.value"
    ref="variableForm"
    modalSize="lg"
    :tutorial="true"
    component="opensilex-VariableForm"
    createTitle="VariableForm.add"
    editTitle="VariableForm.edit"
    icon="fa#vials"
    :createAction="create"
    :updateAction="update"
    :successMessage="successMessage"
    :key="key.value"
  /> -->
</template>

<script setup lang="ts">
import { ref, computed, defineExpose, nextTick, inject, onMounted, defineEmits } from 'vue';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';
import DTOConverter from '../../../models/DTOConverter';
import {
  VariableCreationDTO,
  VariableDetailsDTO,
  VariableUpdateDTO,
  VariablesService,
} from 'opensilex-core/index';
import { BaseExternalReferencesForm } from '../../common/external-references/ExternalReferencesTypes';
import type OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";

const store = useStore();
const { t } = useI18n();

const $opensilex= inject<OpenSilexVuePlugin>("$opensilex");

const service = $opensilex.getService<VariablesService>('opensilex.VariablesService');

const loadForm = ref(false);
const key = ref(1);

const variableForm = ref();

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

const emit = defineEmits(['ready']);

onMounted(() => {
  emit('ready');
});

// 👇 expose la méthode showCreateForm() comme en Vue 2
function showCreateForm() {
    console.log("variableCreate showCreateForm ")
  refresh();
  loadForm.value = true;
  nextTick(() => {
    variableForm.value?.showCreateForm();
  });
}

// 👇 expose la méthode showEditForm(form)
function showEditForm(form: VariableDetailsDTO, linkedDataNb: number) {
  form.linked_data_nb = linkedDataNb;
  refresh();
  loadForm.value = true;
  nextTick(() => {
    const formCopy = DTOConverter.extractURIFromResourceProperties<VariableDetailsDTO, VariableUpdateDTO>(form);
    variableForm.value?.showEditForm(formCopy);
  });
}

// 👇 expose les fonctions à l’extérieur (ex: VariablesView.vue → ref.value.showCreateForm())
defineExpose({
  showCreateForm,
  showEditForm,
});

function refresh() {
  key.value++;
}

function create(variable: VariableCreationDTO) {
  service
    .createVariable(variable)
    .then((http) => {
      const message = t('VariableForm.variable') + ' ' + variable.name + ' ' + t('component.common.success.creation-success-message');
      $opensilex.showSuccessToast(message);
      variable.uri = http.response.result.toString();
      // @ts-ignore : Le composant parent écoutera cet événement
      emit('onCreate', variable);
    })
    .catch((error) => {
      if (error.status === 409) {
        $opensilex.errorHandler(error, `Variable ${variable.uri} : ${t('VariableForm.already-exist')}`);
      } else {
        $opensilex.errorHandler(error, error.response?.result?.message || undefined);
      }
    });
}

function update(variable: VariableUpdateDTO) {
  service
    .updateVariable(variable)
    .then(() => {
      const message = t('VariableForm.variable') + ' ' + variable.name + ' ' + t('component.common.success.update-success-message');
      $opensilex.showSuccessToast(message);
      // @ts-ignore : Le composant parent écoutera cet événement
      emit('onUpdate', variable);
    })
    .catch((error) => {
      $opensilex.errorHandler(error, error.response?.result?.message || undefined);
    });
}

function successMessage(variable: VariableCreationDTO) {
  return t('VariableView.name') + ' ' + variable.name;
}
</script>
