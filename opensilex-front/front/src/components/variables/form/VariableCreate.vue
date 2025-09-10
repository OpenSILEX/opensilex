<template>
  <opensilex-ModalForm
    v-if="loadForm"
    ref="variableForm"
    modal-size="lg"
    :tutorial="true"
    :component="formComponent"
    :createTitle="'component.variable.add'"
    :editTitle="'component.variable.edit'"
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

async function create(variable: VariableCreationDTO) {
  try {
    console.log('[VariableCreate] payload envoyé à createVariable:', JSON.parse(JSON.stringify(variable)))

    if (!service) {
      throw new Error('VariablesService indisponible (service == undefined)')
    }

    const http = await service.createVariable(variable)
    console.log('[VariableCreate] réponse createVariable:', http)

    const createdUri = (http as any)?.response?.result?.toString?.() ?? (http as any)?.response?.result
    if (!createdUri) {
      throw new Error('Réponse sans URI créée (response.result manquant)')
    }


    try {
      const check = await service.getVariable(createdUri)
      console.log('[VariableCreate] vérification getVariable:', check)
    } catch (e) {
      console.warn('[VariableCreate] getVariable a raté  ', e)
    }

    // on met à jour l’URI + message succès
    variable.uri = createdUri
    const message = t('component.variable.name') + ' ' + variable.name + ' ' + t('component.common.success.creation-success-message')
    $opensilex?.showSuccessToast(message)

    emit('onCreate', variable)
    return variable
  } 
catch (error: any) {
  if (error?.status === 409) {
    $opensilex?.errorHandler(error, `Variable ${variable.uri} : ${t('VariableForm.already-exist')}`)
  } else {
    $opensilex?.errorHandler(error)
  }
  return false
}

function update(variable: VariableUpdateDTO) {
  return service?.updateVariable(variable)
    .then(() => {
      const message = t('component.variable.name') + ' ' + variable.name + ' ' + t('component.common.success.update-success-message');
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
