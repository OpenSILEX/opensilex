<template>
  <opensilex-ModalForm
    v-if="loadForm"
    ref="variableForm"
    modal-size="lg"
    :tutorial="true"
    :component="formComponent"
    :createTitle="'VariableForm.add'"
    :editTitle="'VariableForm.edit'"
    icon="fa#vials"
    :create-action="create"
    :update-action="update"
    :success-message="successMessage"
    :key="key"
    @onCreate="onCreate"
    @onUpdate="onUpdate"
  ></opensilex-ModalForm>
</template>


<script setup lang="ts">
import { ref, computed, nextTick, inject, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';
import DTOConverter from './../../../models/DTOConverter';
import { VariableCreationDTO, VariableDetailsDTO, VariableUpdateDTO, VariablesService } from 'opensilex-core/index';
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import ModalForm from '@/components/common/forms/ModalForm.vue';
import VariableForm from './VariableForm.vue'; 

const emit = defineEmits<{
  (e: 'ready'): void;
  (e: 'onCreate', variable: VariableCreationDTO): void;
  (e: 'onUpdate', variable: VariableUpdateDTO): void;
}>();

const store = useStore();
const { t } = useI18n();
const $opensilex = inject<OpenSilexVuePlugin>("$opensilex");

const service = $opensilex?.getService<VariablesService>('opensilex.VariablesService');

const variableForm = ref<InstanceType<typeof ModalForm>>();
const formComponent = VariableForm;

const loadForm = ref(false);
const key = ref(1);

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);
// const canEdit = computed(() => user.value?.hasCredential(credentials.value?.CREDENTIAL_VARIABLE_MODIFICATION_ID));

onMounted(() => emit('ready'));

function showCreateForm() {
  console.log("variableCreate showCreateForm")
  refresh();
  loadForm.value = true;
  nextTick(() => {
    variableForm.value?.showCreateForm();
  });
}

function showEditForm(form: VariableDetailsDTO, linkedDataNb: number) {
  console.log("variableCreate - showEditForm")
  form.linked_data_nb = linkedDataNb;
  refresh();
  loadForm.value = true;
  nextTick(() => {
    const formCopy = DTOConverter.extractURIFromResourceProperties<VariableDetailsDTO, VariableUpdateDTO>(form);
    variableForm.value?.showEditForm(formCopy);
  });
}

function refresh() {
  key.value++;
}

function create(variable: VariableCreationDTO) {
  return service?.createVariable(variable)
    .then((http) => {
      const message = t('VariableForm.variable') + ' ' + variable.name + ' ' + t('component.common.success.creation-success-message');
      $opensilex?.showSuccessToast(message);
      variable.uri = http.response.result.toString();
      emit('onCreate', variable);
      return variable;
    })
    .catch((error) => {
      if (error.status === 409) {
        $opensilex?.errorHandler(error, `Variable ${variable.uri} : ${t('VariableForm.already-exist')}`);
      } else {
        $opensilex?.errorHandler(error, error.response?.result?.message);
      }
      return false;
    });
}

function update(variable: VariableUpdateDTO) {
  return service?.updateVariable(variable)
    .then(() => {
      const message = t('VariableForm.variable') + ' ' + variable.name + ' ' + t('component.common.success.update-success-message');
      $opensilex?.showSuccessToast(message);
      emit('onUpdate', variable);
      return variable;
    })
    .catch((error) => {
      $opensilex?.errorHandler(error, error.response?.result?.message);
      return false;
    });
}

function successMessage(variable: VariableCreationDTO) {
  return t('VariableView.name') + ' ' + variable.name;
}

// Expose
defineExpose({
  showCreateForm,
  showEditForm,
});
</script>
