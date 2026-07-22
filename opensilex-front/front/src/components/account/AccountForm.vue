<template>
  <Modal ref="modalRef">
    <template #header>
      <FormHeader :title="computedTitle" :tutorial="!editMode" :icon="null" />
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
            :uri.sync="form.uri"
            label="component.account.account-uri"
            helpMessage="component.common.uri-help-message"
            :editMode="editMode"
            :generated.sync="uriGenerated"
        ></UriForm>
      </n-form-item>

      <!-- Email -->
      <n-form-item path="email">
        <InputForm
            v-model:value="form.email"
            label="component.account.email-address"
            type="email"
            :placeholder="t('component.account.form-email-placeholder')"
            autocomplete="email"
        ></InputForm>
      </n-form-item>

      <!-- Password -->
      <n-form-item path="password">
        <InputForm
            v-model:value="form.password"
            label="component.account.password"
            type="password"
            :required="!editMode"
            :placeholder="t('component.account.form-password-placeholder')"
            autocomplete="new-password"
        ></InputForm>
      </n-form-item>

      <!-- Default language -->
        <FormSelector
            v-model:selected="form.language"
            :options="languages"
            :required="true"
            label="component.account.default-lang"
            :placeholder="t('component.common.select-lang')"
            path="language"
        ></FormSelector>

      <!-- Admin flag -->
      <n-form-item v-if="isUserAdmin">
        <CheckboxForm
            v-model:value="form.admin"
            label="component.account.admin"
            title="component.account.form-admin-option-label"
        ></CheckboxForm>
      </n-form-item>

      <!-- linked person -->
        <PersonSelector
            v-if="canSelectAPerson"
            v-model:persons="form.linked_person"
            label="component.account.linked-person"
            helpMessage="component.account.person-selector.help-message"
            :getOnlyPersonsWithoutAccount="true"
            :allowAddPerson="true"
        ></PersonSelector>
        <InputForm
            v-else
            :value="linkedPersonString"
            label="component.account.linked-person"
            disabled
        ></InputForm>

    </n-form>

    <template #footer>
      <FormFooter @cancel="hide" @submit="submitModal" />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {computed, ComputedRef, inject, ref, useTemplateRef} from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SecurityService} from "opensilex-security/api/security.service";
import {PersonDTO} from "opensilex-security/index";
import UriForm from "@/components/common/forms/UriForm.vue";
import {useI18n} from "vue-i18n";
import InputForm from "@/components/common/forms/InputForm.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import CheckboxForm from "@/components/common/forms/CheckboxForm.vue";
import PersonSelector from "@/components/persons/PersonSelector.vue";
import FormHeader from "@/components/common/forms/FormHeader.vue";
import FormFooter from "@/components/common/forms/FormFooter.vue";
import {NForm, NFormItem} from "naive-ui";
import {requiredTrimmed, validEmail} from "@/models/FormFieldsFormatter";
import {useStore} from "vuex";
import {OpenSilexStore} from "@/models/Store";
import useModalFormLogic from "@/composables/useModalFormLogic";
import Modal from "@/components/common/views/Modal.vue";

const opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const securityService: SecurityService = opensilex.getService<SecurityService>("opensilex-core.SecurityService");
const store = useStore() as OpenSilexStore;
const {t, availableLocales} = useI18n();

//#region Types
interface AccountFormDTO {
  uri: string | null;
  email: string;
  password: string | null;
  language: string;
  admin: boolean;
  linked_person: string | null;
}
//#endregion

const emit = defineEmits(['hide','onCreate','onUpdate','onSuccess'])


const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modalRef')
const nFormRef = useTemplateRef<InstanceType<typeof NForm>>('formRef')

//#region datas
let uriGenerated = ref<boolean>(true);
const linkedPerson = ref<PersonDTO | null>(null);
const canSelectAPerson = ref(false);
//#endregion

//#region Computed
const rules = computed(() => ({
  "email": [validEmail(), requiredTrimmed('component.account.email-address')],
  'password': {
    validator(_rule, value) {
      // composable exposes editMode, use that in rule resolution below by referencing the reactive returned editMode
      if (!editMode.value && (!value || value.toString().trim().length === 0)) {
        return new Error(t('validations.required_if', {_field_: t('component.account.password')}));
      }
      return true;
    },
    trigger: ['blur', 'input']
  },
  'language': requiredTrimmed('component.account.default-lang'),
}));

const languages: ComputedRef<Array<{id: string; label: string}>> = computed(() => {
  const langs: Array<{id: string; label: string}> = [];
  availableLocales.forEach( locale => {
    langs.push({
      id: locale,
      label: t("component.header.language." + locale)
    });
  });
  return langs;
});

const isUserAdmin: ComputedRef<boolean> = computed(() => {
  const user = store.state.user;
  return user && typeof user.isAdmin === 'function' ? user.isAdmin() : false;
});

const linkedPersonString: ComputedRef<string> = computed(() => {
  if (linkedPerson.value) {
    let personLabel = linkedPerson.value.first_name + " " + linkedPerson.value.last_name;
    if (linkedPerson.value.email !== null) {
      personLabel += " <" + linkedPerson.value.email + ">";
    }
    return personLabel;
  }
  return modalFormLogic.form.value.linked_person || "";
});
//#endregion

//#region modalFormLogic composable
const modalFormLogic = useModalFormLogic<AccountFormDTO>({
  modalRef,
  nFormRef,
  getEmptyForm,
  create,
  update,
  reset,
  onCreate: (res) => emit('onCreate', res),
  onUpdate: (res) => emit('onUpdate', res),
  onSuccess: () => emit('onSuccess'),
  onHide: () => emit('hide')
})

const form = modalFormLogic.form
const editMode = modalFormLogic.editMode
const submitModal = modalFormLogic.submit
const hide = modalFormLogic.hide
const computedTitle = computed(() => t(editMode.value ? 'component.account.edit-title' : 'component.account.create-title'))
//#endregion

//#region Methods
function getEmptyForm(): AccountFormDTO {
  return {
    uri: null,
    email: "",
    linked_person: null,
    admin: false,
    password: "",
    language: "en"
  };
}

async function reset(): Promise<void> {
  if (modalFormLogic.form.value.linked_person) {
    try {
      const response = await securityService.getPerson(modalFormLogic.form.value.linked_person);
      linkedPerson.value = response.response.result;
    } catch (error) {
      opensilex.errorHandler(error);
    }
  } else {
    linkedPerson.value = null;
  }

  const isCreationForm: boolean = !modalFormLogic.editMode.value;
  const canAddAPerson: boolean = !modalFormLogic.form.value.linked_person;
  canSelectAPerson.value = isCreationForm || canAddAPerson;
}

async function create(formData: AccountFormDTO) {
    return await securityService.createAccount(formData);
}

async function update(formData: AccountFormDTO) {
  if (formData.password === "") {
    formData.password = null;
  }
  return await securityService.updateAccount(formData);
}
//#endregion

//#region Expose
defineExpose({
  showCreateForm: modalFormLogic.showCreateForm,
  showEditForm: modalFormLogic.showEditForm,
  hide: modalFormLogic.hide
});
//#endregion
</script>

<style scoped lang="scss">

</style>

