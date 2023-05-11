<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
        :uri.sync="form.uri"
        label="component.user.user-uri"
        helpMessage="component.common.uri-help-message"
        :editMode="editMode"
        :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Email -->
    <opensilex-InputForm
        :value.sync="form.email"
        label="component.user.email-address"
        type="email"
        :required="true"
        rules="email"
        placeholder="component.user.form-email-placeholder"
        autocomplete="new-password"
    ></opensilex-InputForm>

    <!-- Password -->
    <opensilex-InputForm
        :value.sync="form.password"
        label="component.user.password"
        type="password"
        :required="!this.editMode"
        placeholder="component.user.form-password-placeholder"
        autocomplete="new-password"
    ></opensilex-InputForm>

    <!-- Default language -->
    <opensilex-SelectForm
        :selected.sync="form.language"
        :options="languages"
        :required="true"
        label="component.user.default-lang"
        placeholder="component.common.select-lang"
    ></opensilex-SelectForm>

    <!-- Admin flag -->
    <opensilex-CheckboxForm
        v-if="user.admin"
        :value.sync="form.admin"
        label="component.user.admin"
        title="component.user.form-admin-option-label"
    ></opensilex-CheckboxForm>

    <!-- choices about holder of the account -->
      <b-form-group :label="$t('component.user.holderLabel')" v-slot="{ ariaDescribedby }">

        <div id="choixPerson">

          <div v-if="hasHolder" class="boutonChoix" :title="$t('component.user.choices.create-person')">
            <b-form-radio
                button
                button-variant="outline-info"
                v-model="selected"
                value="addPerson">
              <slot name="icon">
                <opensilex-Icon icon='fa#user-plus' />
              </slot>
            </b-form-radio>
          </div>

          <div class="boutonChoix" :title="$t('component.user.choices.select-person')">
            <b-form-radio
                class="boutonChoix"
                button
                button-variant="outline-info"
                v-model="selected"
                value="selectPerson">
              <slot name="icon">
                <opensilex-Icon icon='fa#link' />
              </slot>
            </b-form-radio>
          </div>

          <div class="boutonChoix" :title="$t('component.user.choices.account-only')">
            <b-form-radio
                class="boutonChoix"
                button
                button-variant="outline-info"
                v-model="selected"
                value="noOne">
              <slot name="icon">
                <opensilex-Icon icon='fa#window-close' />
              </slot>
            </b-form-radio>
          </div>
        </div>

      </b-form-group>



    <!-- persons -->
    <opensilex-PersonSelector
        v-if="selected === 'selectPerson'"
        :persons.sync="form.holderOfTheAccountURI"
        label="component.user.person-selector.label"
        helpMessage="component.user.person-selector.help-message"
        getOnlyPersonsWithoutAccount="true"
    ></opensilex-PersonSelector>

    <!-- First name -->
    <opensilex-InputForm
        v-if="selected === 'addPerson'"
        :value.sync="form.first_name"
        label="component.user.first-name"
        type="text"
        :required="true"
        placeholder="component.user.form-first-name-placeholder"
    ></opensilex-InputForm>

    <!-- Last name -->
    <opensilex-InputForm
        v-if="selected === 'addPerson'"
        :value.sync="form.last_name"
        label="component.user.last-name"
        type="text"
        :required="true"
        placeholder="component.user.form-last-name-placeholder"
    ></opensilex-InputForm>
  </b-form>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SecurityService} from "opensilex-security/api/security.service";

@Component
export default class UserForm extends Vue {
$opensilex: OpenSilexVuePlugin
$securityService : SecurityService

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
        holderOfTheAccountURI: "",
        first_name: "",
        last_name: "",
        admin: false,
        password: "",
        language: "en"
      };
    }
  })
  form;

  selected: 'selectPerson' | 'addPerson' | 'noOne' = 'selectPerson'

  hasHolder = false

  created(){
    this.$securityService = this.$opensilex.getService("opensilex.SecurityService")
  }

  reset() {
    this.uriGenerated = true;
    this.$nextTick(() => {
      this.hasHolder = !this.editMode || (this.editMode && this.form.first_name != null)
      this.selected = this.hasHolder ? 'addPerson' : 'noOne'
    });
  }

  getEmptyForm() {
    return {
      uri: null,
      email: "",
      holderOfTheAccountURI: "",
      first_name: "",
      last_name: "",
      admin: false,
      password: "",
      language: "en"
    };
  }

  create(form) {
    if (this.selected === 'addPerson') {
      return this.createWithNewPerson(form)
    } else {
      return this.createWithExistentPersonOrAccountOnly(form)
    }
  }

  createWithNewPerson(form) {
    return this.$securityService
        .createUser(form)
        .catch(error => {
          this.$opensilex.errorHandler(error);
        });
  }

  createWithExistentPersonOrAccountOnly(form) {
    return this.$securityService
        .createAccount(form)
        .catch(error => {
          this.$opensilex.errorHandler(error);
        });
  }

  update(form) {
    if (form.password === "") {
      form.password = null;
    }
    return this.$securityService
        .updateUser(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("User updated", uri);
        })
        .catch(this.$opensilex.errorHandler);
  }

}
</script>

<style scoped lang="scss">
#choixPerson {
  display: flex;
  margin-bottom: 1%;
}

.boutonChoix {
  margin-right: 4%;
}
</style>

