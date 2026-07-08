<template>
  <Modal ref="modal">

    <!--  person fields -->
    <div>
      <div class="input-checkbox-wrapper">
        <input-form
            class="input"
            v-model:value="person.first_name"
            label="component.person.first-name"
            helpMessage="component.person.orcid-suggestion.checkbox-help-message"
            type="text"
            :disabled="!keepFirstName"
            :placeholder="t('component.person.orcid-suggestion.first-name-placeholder')"
        ></input-form>
        <n-checkbox
            class="checkbox"
            v-model:checked="keepFirstName">
          <FormInputLabelHelper
              :label="t('component.person.orcid-suggestion.first-name-pickup')"
          />
        </n-checkbox>
      </div>

      <div class="input-checkbox-wrapper">
        <input-form
            class="input"
            v-model:value="person.last_name"
            label="component.person.last-name"
            helpMessage="component.person.orcid-suggestion.checkbox-help-message"
            type="text"
            :disabled="!keepLastName"
            :placeholder="t('component.person.orcid-suggestion.last-name-placeholder')"
        ></input-form>
        <n-checkbox
            class="checkbox"
            v-model:checked="keepLastName">
          <FormInputLabelHelper
              :label="t('component.person.orcid-suggestion.last-name-pickup')"
          />
        </n-checkbox>
      </div>

      <!--  mail -->
      <form-selector
          ref="personSelector"
          :label="t('component.person.email-address')"
          :helpMessage="t('component.person.orcid-suggestion.selector-help-message')"
          v-model:selected="person.email"
          :multiple="false"
          :options="mailOptions"
          :placeholder="t('component.person.orcid-suggestion.selector-help-message')"
          noResultsText="component.person.filter-search-no-result"
      />

      <!--  affiliation -->
      <form-selector
          ref="personSelector"
          :label="t('component.person.affiliation')"
          :helpMessage="t('component.person.orcid-suggestion.selector-help-message')"
          v-model:selected="person.affiliation"
          :multiple="false"
          :options="affiliationOptions"
          :placeholder="t('component.person.orcid-suggestion.selector-help-message')"
          noResultsText="component.person.filter-search-no-result"
      />
    </div>


    <template #footer>
      <Button
          label="component.common.close"
          class="btn-secondary"
          @click="cancelAndHideModal()"
      />

      <Button
          label="component.common.validateSelection"
          class="greenThemeColor"
          @click="sendInfosThenHideModal()"
      />
    </template>
  </Modal>
</template>

<script setup lang="ts">
import {inject, useTemplateRef, watch, ref} from 'vue';
import {NCheckbox} from "naive-ui";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SecurityService} from "opensilex-security/api/security.service";
import {PersonDTO} from "opensilex-security/index";
import {useI18n} from "vue-i18n";
import Modal from "@/components/common/views/Modal.vue";
import Button from "@/components/common/buttons/Button.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import FormInputLabelHelper from "@/components/common/forms/FormInputLabelHelper.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";

export type Option = { id: string, label: string }

//#region Plugins and services
const opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const securityService: SecurityService = opensilex.getService<SecurityService>("opensilex-core.SecurityService");
const {t} = useI18n();
//#endregion

const props = defineProps({
  orcid: String,
})

const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modal');

//#region Data and computed
const displayModal = defineModel<boolean>("displayModal", {default: false, required: true})

const mailOptions = ref<Array<Option>>([])
const affiliationOptions = ref<Array<Option>>([])

const keepLastName = ref<boolean>(true)
const keepFirstName = ref<boolean>(true)

const person = ref<PersonDTO>(getEmptyPerson())
//#endregion

//#region Emits
const emit = defineEmits<{
  (e: "selectionDone", payload: PersonDTO): void
}>()
//#endregion

/**
 * When the modal is opened, we start the suggestion process
 */
watch(
    () => displayModal.value,
    (display) => {
      if (display) {
        startOrcidSuggestion()
        modalRef.value.show()
      }
    }
);

async function startOrcidSuggestion(): Promise<void> {
  refreshPersonAndSelectors()
  keepLastName.value = true
  keepFirstName.value = true
  displayModal.value = true

  showLoader()
  try {

    let orcidRecordDto = (await securityService.getOrcidRecord(props.orcid)).response.result
    person.value.last_name = orcidRecordDto.last_name

    person.value.first_name = orcidRecordDto.first_name

    mailOptions.value = extractOptionsFromArray(orcidRecordDto.emails)
    person.value.email = mailOptions.value[0]?.id

    affiliationOptions.value = extractOptionsFromArray(orcidRecordDto.organizations)
    person.value.affiliation = affiliationOptions.value[0] ? affiliationOptions.value[0].id : null

  } catch (error) {
    opensilex.errorHandler(error);
  } finally {
    hideLoader()
  }
}

function extractOptionsFromArray(array: Array<string>): Array<Option> {
  return array.map(element => {
    return {id: element, label: element}
  })
}

function sendInfosThenHideModal(): void {
  person.value.last_name = keepLastName.value ? person.value.last_name : null
  person.value.first_name = keepFirstName.value ? person.value.first_name : null
  emit("selectionDone", person.value)
  modalRef.value.hide()
  displayModal.value = false
}

function getEmptyPerson(): PersonDTO {
  return {
    account: null,
    affiliation: null,
    email: null,
    first_name: null,
    last_name: null,
    orcid: null,
    phone_number: null,
    uri: null
  }
}

function cancelAndHideModal(): void {
  modalRef.value.hide()
  displayModal.value = false
  refreshPersonAndSelectors()
}

function refreshPersonAndSelectors(): void {
  person.value = getEmptyPerson()
  mailOptions.value = []
  affiliationOptions.value = []
  keepLastName.value = true
  keepFirstName.value = true
}

function showLoader() {
  opensilex.enableLoader();
  opensilex.showLoader();
}

function hideLoader() {
  opensilex.hideLoader();
  opensilex.disableLoader();
}

</script>

<style scoped lang="scss">

.input-checkbox-wrapper {
  display: flex;
  align-items: flex-end;
}

.input {
  max-width: 50%;
}

.checkbox {
  margin-left: 1%;
}

</style>