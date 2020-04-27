<template>
  <b-modal ref="modalRef" @ok.prevent="validate">
    <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
    <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

    <template v-slot:modal-title>{{title}}</template>
    <ValidationObserver ref="validatorRef">
      <b-form>
        <!-- URI -->
        <opensilex-UriForm
          :uri.sync="form.uri"
          label="component.user.user-uri"
          helpMessage="component.common.uri.help-message"
          :editMode="editMode"
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
          :rules="passwordValidationRule()"
          placeholder="component.user.form-password-placeholder"
          autocomplete="new-password"
        ></opensilex-InputForm>

        <!-- First name -->
        <opensilex-InputForm
          :value.sync="form.firstName"
          label="component.user.first-name"
          type="text"
          :required="true"
          placeholder="component.user.form-first-name-placeholder"
        ></opensilex-InputForm>

        <!-- Last name -->
        <opensilex-InputForm
          :value.sync="form.lastName"
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
          v-model="form.admin"
          :options="languages"
          label="component.user.admin"
          title="component.user.form-admin-option-label"
        ></opensilex-CheckboxForm>
      </b-form>
    </ValidationObserver>
  </b-modal>
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
        firstName: "",
        lastName: "",
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
      firstName: "",
      lastName: "",
      admin: false,
      password: "",
      language: "en"
    };
  }

  showCreateForm() {
    this.clearForm();
    this.editMode = false;
    this.title = this.$t("component.user.add").toString();
    this.uriGenerated = true;
    let modalRef: any = this.modalRef;
    modalRef.show();
  }

  showEditForm(form: UserCreationDTO) {
    this.form = form;

    this.editMode = true;
    this.title = this.$t("component.user.update").toString();
    this.uriGenerated = true;
    let modalRef: any = this.modalRef;
    modalRef.show();
  }

  hideForm() {
    let modalRef: any = this.modalRef;
    modalRef.hide();
  }

  onValidate() {
    return new Promise((resolve, reject) => {
      if (this.form.password == "") {
        this.form.password = null;
      }
      if (this.editMode) {
        this.$emit("onUpdate", this.form, result => {
          if (result instanceof Promise) {
            result.then(resolve).catch(reject);
          } else {
            resolve(result);
          }
        });
      } else {
        return this.$emit("onCreate", this.form, result => {
          if (result instanceof Promise) {
            result.then(resolve).catch(reject);
          } else {
            resolve(result);
          }
        });
      }
    });
  }

  validate() {
    let validatorRef: any = this.validatorRef;
    validatorRef.validate().then(isValid => {
      if (isValid) {
        if (this.uriGenerated && !this.editMode) {
          this.form.uri = null;
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

