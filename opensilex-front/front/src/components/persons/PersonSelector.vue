<template>
  <div>
    <opensilex-FormSelector
        ref="personSelector"
        :label="label"
        :helpMessage="helpMessage"
        :selected.sync="personsURI"
        :multiple="multiple"
        :itemLoadingMethod="loadPersons"
        :required="required"
        :searchMethod="searchPersons"
        :conversionMethod="personToSelectNode"
        placeholder="component.person.filter-placeholder"
        noResultsText="component.person.filter-search-no-result"
        :actionHandler="allowAddPerson && user.hasCredential(credentials.CREDENTIAL_PERSON_MODIFICATION_ID) ? showCreateForm : null"
        @select="select"
        @deselect="deselect"
    ></opensilex-FormSelector>

    <opensilex-ModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_PERSON_MODIFICATION_ID)"
        :static="false"
        ref="PersonForm"
        component="opensilex-PersonForm"
        createTitle="PersonView.create"
        editTitle="PersonView.update"
        icon="ik#ik-user"
        @onCreate="setCreatedPerson"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import {SecurityService, PersonDTO} from "opensilex-security/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import FormSelector from "../common/forms/FormSelector.vue";
import {OpenSilexStore} from "../../models/Store";
import ModalForm from "../common/forms/ModalForm.vue";
import PersonForm from "./PersonForm.vue";

@Component
export default class PersonSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: OpenSilexStore;

  service: SecurityService;

  @PropSync("persons")
  personsURI;

  @Prop()
  label;

  @Prop()
  required;

  @Prop()
  multiple;

  @Prop()
  helpMessage: string;

  @Prop()
  getOnlyPersonsWithoutAccount: boolean;

  @Prop()
  personPropertyExistsCondition: string;


  @Prop({default: false})
  allowAddPerson: boolean;

  @Ref("personSelector") personSelector!: any;
  @Ref("PersonForm") readonly personForm!: ModalForm<PersonForm, PersonDTO, PersonDTO>;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.service = this.$opensilex.getService<SecurityService>("opensilex.SecurityService")
  }

  loadPersons(personsURI) {
    return this.service
        .getPersonsByURI(personsURI)
        .then(
            (http: HttpResponse<OpenSilexResponse<Array<PersonDTO>>>) =>
                http.response.result
        );
  }

  async searchPersons(searchQuery, page) {
    let searchResponse = await this.service
        .searchPersons(searchQuery, this.getOnlyPersonsWithoutAccount, undefined, page, 0)
    return searchResponse
  }

  personToSelectNode(dto: PersonDTO) {
    let personLabel = dto.first_name + " " + dto.last_name;
    if (dto.email !== null) {
      personLabel += " <" + dto.email + ">";
    }
    let disabled: boolean = false;
    if (this.personPropertyExistsCondition && !dto[this.personPropertyExistsCondition]) {
      disabled = true
    }
    return {
      label: personLabel,
      id: dto.uri,
      isDisabled: disabled
    };
  }

  async setCreatedPerson(createdPersonUri: HttpResponse<OpenSilexResponse<string>>) {
    let createdPerson = ( await this.service.getPerson(createdPersonUri.response.result) ).response.result
    this.personSelector.select(this.personToSelectNode(createdPerson));
    this.$emit("onCreate")
  }

  showCreateForm(){
    this.personForm.showCreateForm()
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }
}
</script>

<style scoped lang="scss">
</style>
