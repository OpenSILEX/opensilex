<template>
  <n-form :rules="rules">
    <!-- URI -->
    <UriForm
        :uri.sync="form.uri"
        label="component.person.person-uri"
        helpMessage="component.common.uri-help-message"
        :editMode="editMode"
        :generated.sync="uriGenerated"
    ></UriForm>


    <FormInputLabelHelper
        label="component.person.orcid"
        helpMessage="component.person.orcid-help-message"
        class="checkbox">
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
          @click="startOrcidSuggestion()"
      />
    </div>
    <OrcidSuggestionModal
        :orcid="props.form.orcid"
        v-model:display-modal="displayOrcidModal"
        @selectionDone="fillFormWithNoNull"
    />
    <!-- orcid -->

    <!-- First name -->
    <InputForm
        :value.sync="form.first_name"
        label="component.person.first-name"
        type="text"
        :required="true"
        placeholder="component.person.form-first-name-placeholder"
    ></InputForm>

    <!-- Last name -->
    <InputForm
        :value.sync="form.last_name"
        label="component.person.last-name"
        type="text"
        :required="true"
        placeholder="component.person.form-last-name-placeholder"
    ></InputForm>

    <!-- Email -->
    <InputForm
        :value.sync="form.email"
        label="component.person.email-address"
        type="email"
        rules="email"
        placeholder="component.person.form-email-placeholder"
        autocomplete="new-password"
    ></InputForm>

    <!-- affiliation -->
    <InputForm
        :value.sync="form.affiliation"
        label="component.person.affiliation"
        placeholder="component.person.form-affiliation-placeholder"
        type="text"
    ></InputForm>

    <!-- phone number -->
    <FormField
        :rules="phoneIsValid ? '' : 'falsy' "
        label="component.person.phone_number"
    >
      <template v-slot:field="field">
        <vue-tel-input
            v-model="phone_number"
            defaultCountry="FR"
            :onlyCountries="['FR']"
            validCharactersOnly
            @validate="validatePhone"
            @input="updatePhoneNumber"
        ></vue-tel-input>
      </template>
    </FormField>

  </n-form>
</template>

<script setup lang="ts">
import {computed, ComputedRef, inject, nextTick, ref, WritableComputedRef} from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SecurityService} from "opensilex-security/api/security.service";
import {PersonDTO} from "opensilex-security/index";
import UriForm from "@/components/common/forms/UriForm.vue";
import {useI18n} from "vue-i18n";
import InputForm from "@/components/common/forms/InputForm.vue";
import FormField from "@/components/common/forms/FormField.vue";
import {NForm} from "naive-ui";
import FormInputLabelHelper from "@/components/common/forms/FormInputLabelHelper.vue";
import Button from "@/components/common/buttons/Button.vue";
import OrcidSuggestionModal from "@/components/persons/OrcidSuggestionModal.vue";

const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const securityService: SecurityService = $opensilex.getService<SecurityService>("opensilex-core.SecurityService");
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

const rules = {
  uri: {required: true},
  parent: {required: true}
}

let uriGenerated = true;

let displayOrcidModal = ref(false)
let disable_orcid_field: boolean = false
let phoneIsValid: boolean = true
let formattedPhoneNumber = ref("")


//#region Emits
const emit = defineEmits<{
  (e: "onCreate", payload: PersonDTO): void
}>()
//#endregion

function reset() {
  uriGenerated = true;
  nextTick(() => {
    disable_orcid_field = props.editMode && props.form.orcid !== null
    formattedPhoneNumber.value = props.form.phone_number
  })
}

// necessary because vue-tel-input crash if its v-model value is null
const phone_number: WritableComputedRef<string, String> = computed({
  get: () => formattedPhoneNumber.value ? formattedPhoneNumber.value : "",
  set: (number: string) => formattedPhoneNumber.value = number != '' ? number : null
})

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
    $opensilex.errorHandler(error);
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
    $opensilex.errorHandler
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

function fillFormWithNoNull(person: PersonDTO) {
  for (const [key, value] of Object.entries(person)) {
    if (value) {
      props.form[key] = value
    }
  }
}

function startOrcidSuggestion(): void {
  displayOrcidModal.value = true
}


function prepareFormBeforeSending(form: PersonDTO) {
  replaceEmptyStringByNull(form)
  props.form.orcid = getCompleteUrlOrcid(form.orcid)
}

function validatePhone(phoneNumber): void {
  phoneIsValid = phoneNumber?.valid
}

function updatePhoneNumber(number: string, phoneObject: any): void {
  props.form.phone_number = phoneObject.number != "" ? phoneObject.number : null
}

function showLoader() {
  $opensilex.enableLoader();
  $opensilex.showLoader();
}

function hideLoader() {
  $opensilex.hideLoader();
  $opensilex.disableLoader();
}

</script>

<style lang="scss">
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

