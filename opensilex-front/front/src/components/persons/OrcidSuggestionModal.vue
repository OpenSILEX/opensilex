<template>
  <Modal ref="modal">
    <template #header>
      <div class="flex justify-between items-center">
        <h4>
          <slot name="icon">
            <Icon icon="bi#bi-card-checklist" class="icon-title"/>
          </slot>
          {{ t('component.person.orcid-suggestion.title') }}
        </h4>
      </div>
    </template>

    <!--  person fields -->
    <div>
      <div class="input-checkbox-wrapper">
        <input-form
            class="input"
            v-model:value="form.first_name"
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
            v-model:value="form.last_name"
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
          :label="t('component.person.email-address')"
          :helpMessage="t('component.person.orcid-suggestion.selector-help-message')"
          v-model:selected="selectedMail"
          :multiple="false"
          :options="form.mailOptions"
          :placeholder="t('component.person.orcid-suggestion.selector-help-message')"
          noResultsText="component.person.filter-search-no-result"
      />

      <!--  affiliation -->
      <form-selector
          :label="t('component.person.affiliation')"
          :helpMessage="t('component.person.orcid-suggestion.selector-help-message')"
          v-model:selected="selectedAffiliation"
          :multiple="false"
          :options="form.affiliationOptions"
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
import {ref, useTemplateRef, watch} from 'vue';
import {NCheckbox} from "naive-ui";
import {PersonDTO} from "opensilex-security/index";
import {useI18n} from "vue-i18n";
import Modal from "@/components/common/views/Modal.vue";
import Button from "@/components/common/buttons/Button.vue";
import InputForm from "@/components/common/forms/InputForm.vue";
import FormInputLabelHelper from "@/components/common/forms/FormInputLabelHelper.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import Icon from "@/components/common/views/Icon.vue";

export type Option = { id: string, label: string }
export type orcidSuggestionForm = {
  orcid: string,
  first_name: string,
  last_name: string,
  mailOptions: Array<Option>,
  affiliationOptions: Array<Option>,
}

//#region Plugins and services
const {t} = useI18n();
//#endregion

const props = defineProps<{
  form: orcidSuggestionForm
}>()

const modalRef = useTemplateRef<InstanceType<typeof Modal>>('modal');

//#region Data and computed
const displayModal = defineModel<boolean>("displayModal", {default: false, required: true})

const keepLastName = ref<boolean>(true)
const keepFirstName = ref<boolean>(true)

const selectedMail = ref<string>(null)
const selectedAffiliation = ref<string>(null)
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
        refreshValuesAndShow()
      }
    }
);

function refreshValuesAndShow(){
  keepLastName.value = true
  keepFirstName.value = true
  selectedMail.value = props.form.mailOptions?.[0]?.label || null
  selectedAffiliation.value = props.form.affiliationOptions?.[0]?.label || null

  modalRef.value.show()
}

function sendInfosThenHideModal(): void {
  const person = {
    affiliation: selectedAffiliation.value,
    email: selectedMail.value,
    first_name: keepFirstName.value ? props.form.first_name : null,
    last_name:keepLastName.value ? props.form.last_name : null,
  }
  emit("selectionDone", person)
  modalRef.value.hide()
  displayModal.value = false
}

function cancelAndHideModal(): void {
  modalRef.value.hide()
  displayModal.value = false
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