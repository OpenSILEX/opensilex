<template>
  <n-modal
      v-model:show="displayModal"
      :title="t('component.person.orcid-suggestion.title')"
      @ok="sendInfos"
      no-close-on-backdrop
      no-close-on-esc
  >
    <b-form>

      <!--  firstName -->
      <b-form-group>
        <div>
          <opensilex-FormInputLabelHelper
              label="component.person.first-name"
              helpMessage="component.person.orcid-suggestion.checkbox-help-message"
          />
          <b-input-group>
            <b-form-input
                v-model="person.first_name"
                :disabled="!keepFirstName"
                type="text"
                :placeholder="t('component.person.orcid-suggestion.first-name-placeholder')"
            ></b-form-input>
            <b-input-group-append>
              <b-form-checkbox
                  v-model="keepFirstName"
                  :value="true"
                  :unchecked-value="false">
                <opensilex-FormInputLabelHelper
                    :label="t('component.person.orcid-suggestion.first-name-pickup')"
                />
              </b-form-checkbox>
            </b-input-group-append>
          </b-input-group>
        </div>
      </b-form-group>

      <!--  lastName-->
      <b-form-group>
        <div>
          <opensilex-FormInputLabelHelper
              label="component.person.last-name"
              helpMessage="component.person.orcid-suggestion.checkbox-help-message"
          />
          <b-input-group>
            <b-form-input
                v-model="person.last_name"
                :disabled="!keepLastName"
                type="text"
                :placeholder="t('component.person.orcid-suggestion.last-name-placeholder')"
            ></b-form-input>
            <b-input-group-append>
              <b-form-checkbox
                  v-model="keepLastName"
                  :value="true"
                  :unchecked-value="false">
                <opensilex-FormInputLabelHelper
                    :label="t('component.person.orcid-suggestion.last-name-pickup')"
                />
              </b-form-checkbox>
            </b-input-group-append>
          </b-input-group>
        </div>
      </b-form-group>

      <!--  mail -->
      <b-form-group>
        <opensilex-FormInputLabelHelper
            label="component.person.email-address"
            helpMessage="component.person.orcid-suggestion.selector-help-message"
        />
        <treeselect
            :options="mailOptions"
            v-model="person.email"
        />
      </b-form-group>

      <!--  affiliation -->
      <b-form-group>
        <opensilex-FormInputLabelHelper
            label="component.person.affiliation"
            helpMessage="component.person.orcid-suggestion.selector-help-message"
        />
        <treeselect
            :options="affiliationOptions"
            v-model="person.affiliation"
        />
      </b-form-group>

    </b-form>

    <template v-slot:modal-footer="footer">
      <b-button
          variant="secondary"
          @click="footer.cancel()"
      >
        {{ t('component.common.cancel') }}
      </b-button>
      <b-button
          class="greenThemeColor"
          @click="footer.ok()"
      >
        {{ t('component.common.ok') }}
      </b-button>
    </template>

  </n-modal>
</template>

<script setup lang="ts">
import {inject} from 'vue';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SecurityService} from "opensilex-security/api/security.service";
import {PersonDTO} from "opensilex-security/index";
import {useI18n} from "vue-i18n";

export type Option = { id: string, label: string }

const $opensilex: OpenSilexVuePlugin = inject<OpenSilexVuePlugin>("$opensilex")!;
const securityService: SecurityService = $opensilex.getService<SecurityService>("opensilex-core.SecurityService");
const {t} = useI18n();

let displayModal: boolean = false

let mailOptions: Array<Option> = []
let affiliationOptions: Array<Option> = []

let keepLastName: boolean = true
let keepFirstName: boolean = true

let person: PersonDTO = getEmptyPerson()

//#region Emits
const emit = defineEmits<{
  (e: "selectionDone", payload: PersonDTO): void
}>()
//#endregion


async function startOrcidSuggestion(orcid: string): void {
  refreshPersonAndSelectors()
  keepLastName = true
  keepFirstName = true
  displayModal = true

  showLoader()
  try {

    let orcidRecordDto = (await securityService.getOrcidRecord(orcid)).response.result
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