<template>
  <b-modal ref="modalRef" @ok.prevent="validate">
    <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
    <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

    <template v-slot:modal-title>{{title}}</template>
    <ValidationObserver ref="validatorRef">
      <b-form>
        <!-- URI -->
        <opensilex-FormAutogenerateURI
          label="component.user.user-uri"
          :uri="form.uri"
          :editMode="editMode"
          :uriGenerated="uriGenerated"
        ></opensilex-FormAutogenerateURI>
        <!-- Email -->
        <b-form-group :label="$t('component.user.email-address') + ':'" label-for="email" required>
          <ValidationProvider
            :name="$t('component.user.email-address')"
            rules="required|email"
            v-slot="{ errors }"
          >
            <b-form-input
              id="email"
              v-model="form.email"
              type="email"
              required
              :placeholder="$t('component.user.form-email-placeholder')"
              autocomplete="new-password"
            ></b-form-input>
            <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>
        <!-- Password -->
        <b-form-group :label="$t('component.user.password') + ':'" label-for="password">
          <ValidationProvider
            :name="$t('component.user.password')"
            :rules="passwordValidationRule()"
            v-slot="{ errors }"
          >
            <b-form-input
              id="password"
              v-model="form.password"
              type="password"
              :placeholder="$t('component.user.form-password-placeholder')"
              autocomplete="new-password"
            ></b-form-input>
            <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>
        <!-- First name -->
        <b-form-group :label="$t('component.user.first-name') + ':'" label-for="firstName" required>
          <ValidationProvider
            :name="$t('component.user.first-name')"
            rules="required"
            v-slot="{ errors }"
          >
            <b-form-input
              id="firstName"
              v-model="form.firstName"
              type="text"
              required
              :placeholder="$t('component.user.form-first-name-placeholder')"
            ></b-form-input>
            <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>
        <!-- Last name -->
        <b-form-group :label="$t('component.user.last-name') + ':'" label-for="lastName" required>
          <ValidationProvider
            :name="$t('component.user.last-name')"
            rules="required"
            v-slot="{ errors }"
          >
            <b-form-input
              id="lastName"
              v-model="form.lastName"
              type="text"
              required
              :placeholder="$t('component.user.form-last-name-placeholder')"
            ></b-form-input>
            <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>
        <!-- Default language -->
        <b-form-group
          :label="$t('component.user.default-lang') + ':'"
          label-for="language"
          required
        >
          <ValidationProvider
            :name="$t('component.user.default-lang')"
            rules="required"
            v-slot="{ errors }"
          >
            <multiselect
              v-model="selectedLang"
              track-by="id"
              label="label"
              :options="languages"
              :show-labels="false"
              :allow-empty="false"
              :placeholder="$t('component.common.select-lang') + ':'"
            />
            <div class="error-message alert alert-danger">{{ errors[0] }}</div>
          </ValidationProvider>
        </b-form-group>
        <!-- Admin flag -->
        <b-form-group
          v-if="user.admin"
          :label="$t('component.user.admin') + ':'"
          label-for="isAdmin"
        >
          <b-form-checkbox
            v-model="form.admin"
            value="true"
            unchecked-value="false"
            switches
          >{{$t('component.user.form-admin-option-label')}}</b-form-checkbox>
        </b-form-group>
      </b-form>
    </ValidationObserver>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import VueRouter from "vue-router";
import { UserCreationDTO } from "opensilex-security/index";

@Component
export default class UserForm extends Vue {
  $opensilex: any;
  $store: any;
  $router: VueRouter;
  $i18n: any;

  selectedLang: any = {};

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

  @Ref("modalRef") readonly modalRef!: any;

  @Ref("validatorRef") readonly validatorRef!: any;

  get user() {
    return this.$store.state.user;
  }

  uriGenerated = true;

  form: any = {
    uri: "",
    email: "",
    firstName: "",
    lastName: "",
    admin: false,
    password: "",
    language: "en"
  };

  title = "";

  editMode = false;

  clearForm() {
    this.form = {
      uri: "",
      email: "",
      firstName: "",
      lastName: "",
      admin: false,
      password: "",
      language: "en"
    };

    this.selectedLang = {
      id: this.$i18n.locale,
      label: this.$i18n.t("component.header.language." + this.$i18n.locale)
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
    this.selectedLang = {
      id: form.language,
      label: this.$i18n.t("component.header.language." + form.language)
    };
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
      this.form.language = this.selectedLang.id;
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

        this.onValidate()
          .then(() => {
            this.$nextTick(() => {
              let modalRef: any = this.modalRef;
              modalRef.hide();
            });
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
    });
  }

  passwordValidationRule() {
    if (this.editMode) {
      return "";
    } else {
      return "required";
    }
  }
}
</script>

<style scoped lang="scss">
</style>

