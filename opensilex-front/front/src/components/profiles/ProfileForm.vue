<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="modalFormLogic.formTitle.value" icon="ik#ik-settings"/>
    </template>

    <n-form
        ref="formRef"
        :rules="rules"
        :model="modalFormLogic.form.value"
        label-placement="top"
        :show-require-mark="true"
        size="large"
    >
      <!-- URI -->
      <n-form-item>
        <UriForm
            v-model:uri="modalFormLogic.form.value.uri"
            label="component.profile.profile-uri"
            helpMessage="component.common.uri-help-message"
            :editMode="modalFormLogic.isEditMode.value"
            v-model:generated="uriGenerated"
        ></UriForm>
      </n-form-item>

      <!-- Name -->
      <n-form-item path="name">
        <InputForm
            v-model:value="modalFormLogic.form.value.name"
            label="component.common.name"
            type="text"
            :required="true"
            :placeholder="t('component.profile.form-name-placeholder')"
        ></InputForm>
      </n-form-item>

      <n-grid cols="2" responsive="screen" item-responsive :x-gap="16" :y-gap="8">
        <n-grid-item v-for="credentialsGroup in credentialsGroups" span="1 m:1" :key="credentialsGroup.group_id">
          <div class="credential-group-title">{{ t(credentialsGroup.group_key_name) }}</div>
          <n-checkbox-group
              v-model:value="selectedCredentials[credentialsGroup.group_id]"
          >
            <n-space vertical>
              <n-checkbox
                  v-for="credential in credentialsGroup.credentials"
                  :key="credential.id"
                  :value="credential.id"
                  :label="t(credential.name)"
              ></n-checkbox>
            </n-space>
          </n-checkbox-group>
        </n-grid-item>
      </n-grid>
    </n-form>

    <template #footer>
      <FormFooter @cancel="modalFormLogic.hide" @submit="modalFormLogic.submit"/>
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {computed, inject, onMounted, ref, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {SecurityService} from "opensilex-security/index";
import {CredentialsGroupDTO} from "opensilex-security/model/credentialsGroupDTO";
import UriForm from "@/components/common/forms/UriForm.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import {NForm, NFormItem, NGrid, NGridItem, NCheckbox, NCheckboxGroup, NSpace} from "naive-ui";
import {requiredTrimmed} from "@/models/FormFieldsFormatter";
import FormHeader from "@/components/common/forms/FormHeader.vue";
import FormFooter from "@/components/common/forms/FormFooter.vue";
import useModalFormLogic, {ModalFormEmits, ModalFormProps} from "@/composables/useModalFormLogic";
import Modal from "@/components/common/views/Modal.vue";
import {ProfileUpdateDTO} from "opensilex-security/model/profileUpdateDTO";
import {useI18n} from "vue-i18n";

//#region Public
const emit = defineEmits<ModalFormEmits>();
const props = defineProps<ModalFormProps>();
//#endregion

//#region Private

//#region Plugin and services
const opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const securityService: SecurityService = opensilex.getService<SecurityService>("opensilex-core.SecurityService");
const {t} = useI18n();
//#endregion

const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef')
const nFormRef = useTemplateRef<InstanceType<typeof NForm>>('formRef')

//#region datas
let uriGenerated = ref<boolean>(true);
const credentialsGroups = ref<Array<CredentialsGroupDTO>>([]);
const selectedCredentials = ref<{ [groupId: string]: Array<string> }>({});
//#endregion

//#region Computed / rules
const rules = computed(() => ({
  'name': requiredTrimmed('component.common.name'),
}))
//#endregion

//#region modalFormLogic composable
const modalFormLogic = useModalFormLogic<ProfileUpdateDTO>({
  modalRef,
  nFormRef,
  getEmptyForm,
  create,
  update,
  reset,
  props,
  emit
})
//#endregion

//#region Methods
function getEmptyForm(): ProfileUpdateDTO {
  return {
    uri: null,
    name: "",
    credentials: []
  };
}

function reset(): void {
  uriGenerated.value = true;
  initSelectedCredentials();
}

function flattenSelectedCredentials(): Array<string> {
  let credentials = [];
  for (let groupId in selectedCredentials.value) {
    credentials = credentials.concat(selectedCredentials.value[groupId]);
  }
  return credentials;
}

async function create(form: ProfileUpdateDTO) {
  form.credentials = flattenSelectedCredentials();
  return await securityService.createProfile(form)
}

async function update(form: ProfileUpdateDTO) {
  form.credentials = flattenSelectedCredentials();
  return await securityService.updateProfile(form)
}

function initSelectedCredentials() {
  let def: any = {};
  for (let i = 0; i < credentialsGroups.value.length; i++) {
    def[credentialsGroups.value[i].group_id] = [];

    for (let j = 0; j < credentialsGroups.value[i].credentials.length; j++) {
      let credentialId = credentialsGroups.value[i].credentials[j].id;
      if (modalFormLogic.form.value.credentials && modalFormLogic.form.value.credentials.indexOf(credentialId) >= 0) {
        def[credentialsGroups.value[i].group_id].push(credentialId);
      }
    }
  }
  selectedCredentials.value = def;
}

//#endregion

//#region Lifecycle
onMounted(() => {
  opensilex.getCredentials().then((credentials: Array<CredentialsGroupDTO>) => {
    credentialsGroups.value = credentials;
    initSelectedCredentials();
  });
});
//#endregion

defineExpose({
  showCreateForm: modalFormLogic.showCreateForm,
  showEditForm: modalFormLogic.showEditForm,
})
</script>

<style scoped lang="scss">
.credential-group-title {
  font-weight: bold;
  margin-bottom: 8px;
}
</style>

<i18n>
en:
  ProfileForm:
    credentialGroups: Credential groups
    credentials: Credentials

fr:
  ProfileForm:
    credentialGroups: Groupes d'autorisation
    credentials: Autorisations
</i18n>
