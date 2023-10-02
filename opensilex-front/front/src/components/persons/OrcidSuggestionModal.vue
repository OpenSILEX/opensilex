<template>
  <b-modal
      ref="orcidModal"
      :title="$t('component.person.orcid-suggestion.title')"
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
                :placeholder="$t('component.person.orcid-suggestion.first-name-placeholder')"
            ></b-form-input>
            <b-input-group-append>
              <b-form-checkbox
                  v-model="keepFirstName"
                  :value="true"
                  :unchecked-value="false">
                <opensilex-FormInputLabelHelper
                    :label="$t('component.person.orcid-suggestion.first-name-pickup')"
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
                :placeholder="$t('component.person.orcid-suggestion.last-name-placeholder')"
            ></b-form-input>
            <b-input-group-append>
              <b-form-checkbox
                  v-model="keepLastName"
                  :value="true"
                  :unchecked-value="false">
                <opensilex-FormInputLabelHelper
                    :label="$t('component.person.orcid-suggestion.last-name-pickup')"
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
        {{$t('component.common.cancel')}}
      </b-button>
      <b-button
          class="greenThemeColor"
          @click="footer.ok()"
      >
        {{$t('component.common.ok')}}
      </b-button>
    </template>

  </b-modal>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Ref} from "vue-property-decorator";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SecurityService} from "opensilex-security/api/security.service";
import {PersonDTO} from "opensilex-security/index";
import {BModal} from "bootstrap-vue";

export type Option = { id: string, label: string }

@Component({})
export default class OrcidSuggestionModal extends Vue {
  $opensilex: OpenSilexVuePlugin
  $securityService: SecurityService

  @Ref("orcidModal") readonly modal!: BModal;

  mailOptions: Array<Option> = []
  affiliationOptions: Array<Option> = []

  keepLastName: boolean = true
  keepFirstName: boolean = true

  person: PersonDTO = this.getEmptyPerson()

  created() {
    this.$securityService = this.$opensilex.getService<SecurityService>("opensilex.SecurityService")
  }

  async startOrcidSuggestion(orcid: string) {
    this.refreshPersonAndSelectors()
    this.keepLastName = true
    this.keepFirstName = true
    this.modal.show()

    this.showLoader()
    try {

      let orcidRecordDto = (await this.$securityService.getOrcidRecord(orcid)).response.result
      this.person.last_name = orcidRecordDto.last_name

      this.person.first_name = orcidRecordDto.first_name

      this.mailOptions = this.extractOptionsFromArray(orcidRecordDto.emails)
      this.person.email = this.mailOptions[0]?.id

      this.affiliationOptions = this.extractOptionsFromArray(orcidRecordDto.organizations)
      this.person.affiliation = this.affiliationOptions[0] ? this.affiliationOptions[0].id : null

    } catch (error) {
      this.$opensilex.errorHandler(error);
    } finally {
      this.hideLoader()
    }
  }

  extractOptionsFromArray(array: Array<string>): Array<Option> {
    return  array.map(element => {
      return {id: element, label: element}
    })
  }

  sendInfos() {
      this.person.last_name = this.keepLastName ? this.person.last_name : null
      this.person.first_name = this.keepFirstName ? this.person.first_name : null
    this.$emit('selectionDone', this.person)
  }

  getEmptyPerson(): PersonDTO {
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

  refreshPersonAndSelectors(): void {
    this.person = this.getEmptyPerson()
    this.mailOptions = []
    this.affiliationOptions = []
  }

  showLoader() {
    this.$opensilex.enableLoader();
    this.$opensilex.showLoader();
  }

  hideLoader() {
    this.$opensilex.hideLoader();
    this.$opensilex.disableLoader();
  }
}
</script>

<style scoped lang="scss">

.input-group-append {
  align-items: center;
  padding-left: 1%;
}

</style>