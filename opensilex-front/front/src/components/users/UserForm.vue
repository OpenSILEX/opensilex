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

    <!-- First name -->
    <opensilex-InputForm
      :value.sync="form.first_name"
      label="component.user.first-name"
      type="text"
      :required="true"
      placeholder="component.user.form-first-name-placeholder"
    ></opensilex-InputForm>

    <!-- Last name -->
    <opensilex-InputForm
      :value.sync="form.last_name"
      label="component.user.last-name"
      type="text"
      :required="true"
      placeholder="component.user.form-last-name-placeholder"
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
  </b-form>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class UserForm extends Vue {
  $opensilex: any;
  $i18n: any;
  $store: any;

  get user() {
    return this.$store.state.user;
  }

  get languages() {
    let langs = [];
    Object.keys(this.$i18n.messages).forEach(key => {
      langs.push({
        id: key,
        label: this.$i18n.t("component.header.language." + key)
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
        first_name: "",
        last_name: "",
        admin: false,
        password: "",
        language: "en"
      };
    }
  })
  form;

  reset() {
    this.uriGenerated = true;
  }

  getEmptyForm() {
    return {
      uri: null,
      email: "",
      first_name: "",
      last_name: "",
      admin: false,
      password: "",
      language: "en"
    };
  }

  create(form) {
    return this.$opensilex
      .getService("opensilex.SecurityService")
      .createUser(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("User created", uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("User already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$i18n.t("component.user.errors.user-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    if (form.password == "") {
      form.password = null;
    }
    return this.$opensilex
      .getService("opensilex.SecurityService")
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
</style>

