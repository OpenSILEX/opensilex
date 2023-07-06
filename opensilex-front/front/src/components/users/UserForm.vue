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
        v-if="user.admin"
        :value.sync="form.admin"
        label="component.account.admin"
        title="component.account.form-admin-option-label"
    ></opensilex-CheckboxForm>

    <!-- choices about holder of the account -->
    <b-form-group :label="$t('component.account.linked-person')" v-slot="{ ariaDescribedby }">

      <div id="choixPerson">

        <div v-if="hasHolder" class="boutonChoix" :title="$t('component.account.choices.create-person')">
          <b-form-radio
              button
              button-variant="outline-info"
              v-model="selected"
              value="addPerson">
            <slot name="icon">
              <opensilex-Icon icon='fa#user-plus'/>
            </slot>
          </b-form-radio>
        </div>

        <div class="boutonChoix" :title="$t('component.account.choices.select-person')">
          <b-form-radio
              class="boutonChoix"
              button
              button-variant="outline-info"
              v-model="selected"
              value="selectPerson">
            <slot name="icon">
              <opensilex-Icon icon='fa#link'/>
            </slot>
          </b-form-radio>
        </div>

        <div class="boutonChoix" :title="$t('component.account.choices.account-only')">
          <b-form-radio
              class="boutonChoix"
              button
              button-variant="outline-info"
              v-model="selected"
              value="noOne">
            <slot name="icon">
              <opensilex-Icon icon='fa#window-close'/>
            </slot>
          </b-form-radio>
        </div>
      </div>

    </b-form-group>


    <!-- persons -->
    <opensilex-PersonSelector
        v-if="selected === 'selectPerson'"
        :persons.sync="form.holderOfTheAccountURI"
        label="component.account.linked-person"
        helpMessage="component.account.person-selector.help-message"
        getOnlyPersonsWithoutAccount="true"
    ></opensilex-PersonSelector>

    <!-- First name -->
    <opensilex-InputForm
        v-if="selected === 'addPerson'"
        :value.sync="form.first_name"
        label="component.account.first-name"
        type="text"
        :required="true"
        placeholder="component.account.form-first-name-placeholder"
    ></opensilex-InputForm>

    <!-- Last name -->
    <opensilex-InputForm
        v-if="selected === 'addPerson'"
        :value.sync="form.last_name"
        label="component.account.last-name"
        type="text"
        :required="true"
        placeholder="component.account.form-last-name-placeholder"
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
import {OpenSilexStore} from "../../models/Store";

@Component
export default class UserForm extends Vue {
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

  originalFirstName: string
  originalLastName: string

  created() {
    this.$securityService = this.$opensilex.getService("opensilex.SecurityService")
  }

  reset() {
    this.uriGenerated = true;
    this.$nextTick(() => {
      this.hasHolder = !this.editMode || (this.editMode && this.form.first_name != null)
      this.selected = this.hasHolder ? 'addPerson' : 'noOne'
      this.originalFirstName = this.form.first_name
      this.originalLastName = this.form.last_name
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
    this.showLoader()
    if (this.selected === 'addPerson') {
      return this.createWithNewPerson(form)
    } else {
      return this.createWithExistentPersonOrAccountOnly(form)
    }
  }

  async createWithNewPerson(form) {
    try {
      let response = await this.$securityService.createUser(form)
      return response
    } catch (error) {
      this.$opensilex.errorHandler(error);
    } finally {
      this.hideLoader()
    }
  }

  async createWithExistentPersonOrAccountOnly(form) {
    try {
      let response = await this.$securityService.createAccount(form)
      return response
    } catch (error) {
      this.$opensilex.errorHandler(error);
    } finally {
      this.hideLoader()
    }
  }

  update(form) {
    this.showLoader()

    if (form.password === "") {
      form.password = null;
    }
    if (form.first_name === "" || form.first_name === null) {
      form.first_name = this.originalFirstName
    }
    if (form.last_name === "" || form.last_name === null) {
      form.last_name = this.originalLastName
    }
    return this.$securityService
        .updateUser(form)
        .then((http: HttpResponse<OpenSilexResponse<any>>) => {
          let uri = http.response.result;
          console.debug("User updated", uri);
        })
        .catch(error => {
          this.$opensilex.errorHandler(error);
        })
        .finally( () => { this.hideLoader()} )
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
#choixPerson {
  display: flex;
  margin-bottom: 1%;
}

.boutonChoix {
  margin-right: 4%;
}
</style>

