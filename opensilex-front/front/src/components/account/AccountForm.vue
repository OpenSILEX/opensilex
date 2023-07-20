<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
        :uri.sync="form.uri"
        label="component.account.account-uri"
        helpMessage="component.common.uri-help-message"
        :editMode="editMode"
        :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Email -->
    <opensilex-InputForm
        :value.sync="form.email"
        label="component.account.email-address"
        type="email"
        :required="true"
        rules="email"
        placeholder="component.account.form-email-placeholder"
        autocomplete="new-password"
    ></opensilex-InputForm>

    <!-- Password -->
    <opensilex-InputForm
        :value.sync="form.password"
        label="component.account.password"
        type="password"
        :required="!this.editMode"
        placeholder="component.account.form-password-placeholder"
        autocomplete="new-password"
    ></opensilex-InputForm>

    <!-- Default language -->
    <opensilex-SelectForm
        :selected.sync="form.language"
        :options="languages"
        :required="true"
        label="component.account.default-lang"
        placeholder="component.common.select-lang"
    ></opensilex-SelectForm>

    <!-- Admin flag -->
    <opensilex-CheckboxForm
        v-if="user.isAdmin()"
        :value.sync="form.admin"
        label="component.account.admin"
        title="component.account.form-admin-option-label"
    ></opensilex-CheckboxForm>

    <!-- persons -->
    <opensilex-PersonSelector
        :key="changeToReloadPersonSelector"
        v-if="canSelectAPerson"
        :persons.sync="form.linked_person"
        label="component.account.linked-person"
        helpMessage="component.account.person-selector.help-message"
        getOnlyPersonsWithoutAccount="true"
        :allowAddPerson="true"
        @onCreate="changeToReloadPersonSelector++"
        :disabled="true"
    ></opensilex-PersonSelector>
    <opensilex-InputForm
        v-else
        :value.sync="linkedPersonString"
        label="component.account.linked-person"
        disabled
    ></opensilex-InputForm>


  </b-form>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SecurityService} from "opensilex-security/api/security.service";
import {OpenSilexStore} from "../../models/Store";
import {PersonDTO} from "opensilex-security/model/personDTO";

@Component
export default class AccountForm extends Vue {
  $opensilex: OpenSilexVuePlugin
  $securityService: SecurityService
  $store: OpenSilexStore

  get user() {
    return this.$store.state.user;
  }

  get languages() {
    let langs = [];
    Object.keys(this.$i18n.messages).forEach(key => {
      langs.push({
        id: key,
        label: this.$t("component.header.language." + key)
      });
    });
    return langs;
  }

  uriGenerated = true;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        email: "",
        linked_person: "",
        admin: false,
        password: "",
        language: "en"
      };
    }
  })
  form;

  changeToReloadPersonSelector: number = 0;

  linkedPerson: PersonDTO = null;

  canSelectAPerson: boolean;

  reset() {
    this.changeToReloadPersonSelector++
    this.$nextTick( () => {
      if (this.form.linked_person){
        this.$securityService.getPerson(this.form.linked_person)
            .then( response => {
              this.linkedPerson = response.response.result
            })
      }

      let isCreationForm: boolean = ! this.editMode
      let canAddAPerson: boolean = ! this.form.linked_person
      this.canSelectAPerson = isCreationForm || canAddAPerson
    })
  }

  created() {
    this.$securityService = this.$opensilex.getService("opensilex.SecurityService")
  }

  get linkedPersonString(): string{
    if (this.linkedPerson){
      let personLabel = this.linkedPerson.first_name + " " + this.linkedPerson.last_name;
      if (this.linkedPerson.email !== null) {
        personLabel += " <" + this.linkedPerson.email + ">";
      }
      return personLabel
    }

    return this.form.linked_person
  }

  getEmptyForm() {
    return {
      uri: null,
      email: "",
      linked_person: null,
      admin: false,
      password: "",
      language: "en"
    };
  }

  async create(form) {
    this.showLoader()
    try {
      return await this.$securityService.createAccount(form)
    } catch(error){
      this.$opensilex.errorHandler(error);
    } finally {
      this.hideLoader()
    }
  }

  async update(form) {
    this.showLoader()

    if (form.password === "") {
      form.password = null;
    }
    try {
      return await this.$securityService.updateAccount(form)
    } catch(error){
      this.$opensilex.errorHandler(error);
    } finally {
      this.hideLoader()
    }

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

</style>

