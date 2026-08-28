<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="formTitle" icon="ik#ik-settings"/>
    </template>

    <n-form
        ref="formRef"
        :rules="rules"
        :model="form"
        label-placement="top"
        :show-require-mark="true"
        size="large"
    >
      <!-- URI -->
      <n-form-item>
        <UriForm
            v-model:uri="form.uri"
            label="component.profile.profile-uri"
            helpMessage="component.common.uri-help-message"
            :editMode="isEditMode"
            v-model:generated="uriGenerated"
        ></UriForm>
      </n-form-item>

      <!-- Name -->
      <n-form-item path="name">
        <InputForm
            v-model:value="form.name"
            label="component.common.name"
            type="text"
            :required="true"
            :placeholder="t('component.profile.form-name-placeholder')"
        ></InputForm>
      </n-form-item>

      <n-table class="os-table" striped>
        <thead>
          <tr>
            <th>{{ t('ProfileForm.credentialGroups') }}</th>
            <th>{{ t('ProfileForm.credentials') }}</th>
          </tr>
        </thead>
        <tbody>
        <tr v-for="credentialsGroup in credentialsGroups" :key="credentialsGroup.group_id">
          <td>{{$t(credentialsGroup.group_key_name)}}</td>
          <td>
            <n-checkbox-group
                v-model:value="selectedCredentials[credentialsGroup.group_id]"
            >
              <n-checkbox
                  v-for="credential in credentialsGroup.credentials"
                  :key="credential.id"
                  :value="credential.id"
                  :label="t(credential.name)"
              ></n-checkbox>
            </n-checkbox-group>
          </td>
        </tr>
        </tbody>
      </n-table>
    </n-form>

    <template #footer>
      <FormFooter @cancel="hide" @submit="submit"/>
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
import {NForm, NFormItem, NGrid, NGridItem, NCheckbox, NCheckboxGroup, NSpace, NTable, NSwitch} from "naive-ui";
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
const { form, formTitle, isEditMode, exposed, hide, submit } = useModalFormLogic<ProfileUpdateDTO>({
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
      if (form.value.credentials && form.value.credentials.indexOf(credentialId) >= 0) {
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

defineExpose(exposed)
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
