<template>
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
          label="component.person.person-uri"
          helpMessage="component.common.uri-help-message"
          :editMode="editMode"
          :generated.sync="uriGenerated"
      ></UriForm>
    </n-form-item>

    <n-form-item>
      <div class="item-and-label">
        <FormInputLabelHelper
            label="component.person.orcid"
            helpMessage="component.person.orcid-help-message"
        >
        </FormInputLabelHelper>
        <div class="row">
          <input-form class="orcid-field"
                      v-model:value="form.orcid"
                      type="text"
                      :disabled="disable_orcid_field"
                      :placeholder="t('component.person.orcid-placeholder')"
          ></input-form>

          <Button
              label="component.person.load-orcid-infos"
              :disabled="! validOrcid"
              :class=" 'orcid-button ' + (validOrcid ? 'greenThemeColor' : 'btn-secondary') "
              @click="onShowOrcidModalButtonClick()"
          />
        </div>
        <OrcidSuggestionModal
            :form="orcidForm"
            v-model:display-modal="displayOrcidModal"
            @selectionDone="onOrcidModalSelectionDone"
        />
      </div>
    </n-form-item>
    <!-- orcid -->

    <!-- First name -->
    <n-form-item path="first_name">
      <InputForm
          v-model:value="form.first_name"
          :label="t('component.person.first-name')"
          type="text"
          :required="true"
          :placeholder="t('component.person.form-first-name-placeholder')"
      ></InputForm>
    </n-form-item>

    <!-- Last name -->
    <n-form-item path="last_name">
      <InputForm
          v-model:value="form.last_name"
          :label="t('component.person.last-name')"
          type="text"
          :required="true"
          :placeholder="t('component.person.form-last-name-placeholder')"
      ></InputForm>
    </n-form-item>

    <!-- Email -->
    <n-form-item path="email">
      <InputForm
          v-model:value="form.email"
          label="component.person.email-address"
          type="email"
          :placeholder="t('component.person.form-email-placeholder')"
          autocomplete="new-password"
      ></InputForm>
    </n-form-item>

    <!-- affiliation -->
    <n-form-item>
      <InputForm
          v-model:value="form.affiliation"
          label="component.person.affiliation"
          :placeholder="t('component.person.form-affiliation-placeholder')"
          type="text"
      ></InputForm>
    </n-form-item>

    <!-- phone number -->
    <n-form-item
        ref="phoneNumberNFormItemRef"
        path="phone_number"
    >
      <FormField
          label="component.person.phone_number"
      >
        <template v-slot:field>
          <vue-tel-input
              v-model="form.phone_number"
              defaultCountry="FR"
              :onlyCountries="['FR']"
              validCharactersOnly
              @validate="onVueTelInputValidate"
              @blur="onVueTelInputBlur"
          ></vue-tel-input>
        </template>
      </FormField>
    </n-form-item>

  </n-form>
</template>

<script setup lang="ts">
import {computed, ComputedRef, inject, nextTick, ref, useTemplateRef, WritableComputedRef} from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SecurityService} from "opensilex-security/api/security.service";
import {PersonDTO} from "opensilex-security/index";
import UriForm from "@/components/common/forms/UriForm.vue";
import {useI18n} from "vue-i18n";
import InputForm from "@/components/common/forms/InputForm.vue";
import FormField from "@/components/common/forms/FormField.vue";
import {NForm, NFormItem} from "naive-ui";
import FormInputLabelHelper from "@/components/common/forms/FormInputLabelHelper.vue";
import Button from "@/components/common/buttons/Button.vue";
import OrcidSuggestionModal, {Option, orcidSuggestionForm} from "@/components/persons/OrcidSuggestionModal.vue";
import {requiredTrimmed, validEmail} from "@/models/FormFieldsFormatter";
import {VueTelInput} from "vue-tel-input";

const opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const securityService: SecurityService = opensilex.getService<SecurityService>("opensilex-core.SecurityService");
const {t} = useI18n();


const props = withDefaults(
    defineProps<{
      editMode?: boolean;
      form: PersonDTO;
    }>(),
    {
      editMode: false,
      form: () => ({
        uri: null,
        email: null,
        first_name: null,
        last_name: null,
        affiliation: null,
        phone_number: null,
        orcid: null
      }),
    }
);

const rules = computed(() => ({
  "first_name": requiredTrimmed('component.person.first-name'),
  'last_name': requiredTrimmed('component.person.last-name'),
  'email': validEmail(),
  'phone_number': {
    validator(_rule, value) {
      if (value == undefined || value.trim().length === 0){
        return true
      }
      if (! phoneIsValid) {
        return new Error(t('validations.phoneNumberError'))
      }
      return true
    },
    trigger: ['phone-number-valid-trigger']
  },
}))

let uriGenerated = true;

const displayOrcidModal = ref(false)
const orcidForm = ref<orcidSuggestionForm>({
  orcid: "",
  first_name: "",
  last_name: "",
  mailOptions: [],
  affiliationOptions: []
})
let disable_orcid_field = ref<boolean>(false)
let phoneIsValid = ref<boolean>(true)

const formRef = useTemplateRef<InstanceType<typeof NForm>>('formRef');
const phoneNumberNFormItemRef = useTemplateRef<InstanceType<typeof NFormItem>>('phoneNumberNFormItemRef');


//#region Emits
const emit = defineEmits<{
  (e: "onCreate", payload: PersonDTO): void
}>()
//#endregion

//#region methods
function reset() {
  uriGenerated = true;
  nextTick(() => {
    disable_orcid_field.value = props.editMode && props.form.orcid !== null
  })
}

const validOrcid: ComputedRef<boolean> = computed(() => {
  //regex : 3 séquences de 4 chiffres séparées par un tiret puis une séquence de 4 chiffres ou 3 chiffres et un X. Le tout précédé ou non du nom de domain de orcid
  //exemples validés : 0009-0006-6636-4714 ou 0009-0006-6636-471X ou https://orcid.org/0009-0006-6636-4714 ou https://orcid.org/0009-0006-6636-471X
  let regexOrcid = /^(https:\/\/orcid.org\/)?([0-9]{4}-){3}[0-9]{3}[0-9X]$/
  return regexOrcid.test(props.form.orcid)
})


function getEmptyForm() {
  return {
    uri: null,
    email: null,
    first_name: null,
    last_name: null,
    affiliation: null,
    phone_number: null,
    orcid: null
  };
}

async function create(form: PersonDTO) {
  showLoader()
  prepareFormBeforeSending(form)

  try {
    let response = await securityService.createPerson(form)
    emit("onCreate", form)
    return response
  } catch (error) {
    opensilex.errorHandler(error);
  } finally {
    hideLoader()
  }

}

async function update(form: PersonDTO) {
  try {
    showLoader()
    prepareFormBeforeSending(form)

    return await securityService.updatePerson(form)
  } catch {
    opensilex.errorHandler
  } finally {
    hideLoader()
  }

}

function getCompleteUrlOrcid(orcid): string {
  if (orcid === "") {
    return null;
  }
  //regex : 3 séquences de 4 chiffres séparées par un tiret puis une séquence de 4 chiffres ou 3 chiffres et un X
  //exemples validés : 0009-0006-6636-4714 ou 0009-0006-6636-471X
  let regexOrcidWithoutCompleteUrl = /^([0-9]{4}-){3}[0-9]{3}[0-9X]$/
  if (regexOrcidWithoutCompleteUrl.test(orcid)) {
    return "https://orcid.org/" + orcid
  }

  return orcid
}

function replaceEmptyStringByNull(form): void {
  if (form.email === "") {
    form.email = null;
  }
}

function formatPhoneNumber(phoneNumber: string): string{
  if ( ! phoneNumber || phoneNumber.trim().length === 0) {
    return null
  }
  return phoneNumber?.replace(/\s/g, '')
}

function prepareFormBeforeSending(form: PersonDTO) {
  replaceEmptyStringByNull(form)
  props.form.orcid = getCompleteUrlOrcid(form.orcid)
  props.form.phone_number = formatPhoneNumber(form.phone_number)
}

function showLoader() {
  opensilex.enableLoader();
  opensilex.showLoader();
}

function hideLoader() {
  opensilex.hideLoader();
  opensilex.disableLoader();
}

async function validate() {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}

function extractOptionsFromArray(array: Array<string>): Array<Option> {
  return array.map(element => {
    return {id: element, label: element}
  })
}
//#endregion

//#region event handler

/** Fills the form with non-null values from the person object */
function onOrcidModalSelectionDone(person: PersonDTO) {
  for (const [key, value] of Object.entries(person)) {
    if (value) {
      props.form[key] = value
    }
  }
}

async function onShowOrcidModalButtonClick(): Promise<void> {
  orcidForm.value = {
    orcid: props.form.orcid,
    last_name: "",
    first_name: "",
    mailOptions: [],
    affiliationOptions: []
  }

  showLoader()
  try {

    let orcidRecordDto = (await securityService.getOrcidRecord(orcidForm.value.orcid)).response.result
    orcidForm.value.last_name = orcidRecordDto.last_name
    orcidForm.value.first_name = orcidRecordDto.first_name

    orcidForm.value.mailOptions = extractOptionsFromArray(orcidRecordDto.emails)
    orcidForm.value.affiliationOptions = extractOptionsFromArray(orcidRecordDto.organizations)

    displayOrcidModal.value = true
  } catch (error) {
    opensilex.errorHandler(error);
  } finally {
    hideLoader()
  }
}


function onVueTelInputValidate(phoneNumber): void {
  phoneIsValid = phoneNumber?.valid
  phoneNumberNFormItemRef.value?.validate({ trigger: 'phone-number-valid-trigger' }) //trigger validation
}

function onVueTelInputBlur() {
    phoneNumberNFormItemRef.value?.validate({ trigger: 'phone-number-valid-trigger' })
}
//#endregion

defineExpose({
  reset,
  create,
  update,
  validate,
  getEmptyForm
})
</script>

<style lang="scss">
.item-and-label {
  display: block;
  width: 100%;
}

.orcid-field {
  max-width: 60%;
}

.orcid-button {
  max-width: 10%;
}

.orcid-button .button-label {
  margin-left: 0;
}
</style>

