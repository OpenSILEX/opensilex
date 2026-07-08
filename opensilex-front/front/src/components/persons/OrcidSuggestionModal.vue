<template>
  <n-modal
      v-model:show="displayModal"
      :title="t('component.person.orcid-suggestion.title')"
      @ok="sendInfos"
  >
    <template #header>
      <h3>header</h3>
    </template>
    <div>aaaaaaaaaaaaaaaaa</div>

  </n-modal>
</template>

<script setup lang="ts">
import {inject, watch} from 'vue';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SecurityService} from "opensilex-security/api/security.service";
import {PersonDTO} from "opensilex-security/index";
import {useI18n} from "vue-i18n";

export type Option = { id: string, label: string }

//#region Plugins and services
const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const securityService: SecurityService = $opensilex.getService<SecurityService>("opensilex-core.SecurityService");
const {t} = useI18n();
//#endregion

const props = defineProps({
      orcid: String,
    })


//#region Data and computed
const displayModal = defineModel<boolean>("displayModal", {default: false, required: true})

let mailOptions: Array<Option> = []
let affiliationOptions: Array<Option> = []

let keepLastName: boolean = true
let keepFirstName: boolean = true

let person: PersonDTO = getEmptyPerson()
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
      }
    }
);

async function startOrcidSuggestion(): Promise<void> {
  refreshPersonAndSelectors()
  keepLastName = true
  keepFirstName = true
  displayModal.value = true

  showLoader()
  try {

    let orcidRecordDto = (await securityService.getOrcidRecord(props.orcid)).response.result
    person.last_name = orcidRecordDto.last_name

    person.first_name = orcidRecordDto.first_name

    mailOptions = extractOptionsFromArray(orcidRecordDto.emails)
    person.email = mailOptions[0]?.id

    affiliationOptions = extractOptionsFromArray(orcidRecordDto.organizations)
    person.affiliation = affiliationOptions[0] ? affiliationOptions[0].id : null

  } catch (error) {
    $opensilex.errorHandler(error);
  } finally {
    hideLoader()
  }
}

function extractOptionsFromArray(array: Array<string>): Array<Option> {
  return array.map(element => {
    return {id: element, label: element}
  })
}

function sendInfos(): void {
  person.last_name = keepLastName ? person.last_name : null
  person.first_name = keepFirstName ? person.first_name : null
  emit("selectionDone", person)
}

function getEmptyPerson():PersonDTO {
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

function refreshPersonAndSelectors():void {
  person = getEmptyPerson()
  mailOptions = []
  affiliationOptions = []
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

<style scoped lang="scss">

.input-group-append {
  align-items: center;
  padding-left: 1%;
}

</style>