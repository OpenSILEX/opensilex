<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.person.person-uri"
      helpMessage="component.common.uri-help-message"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- orcid -->
    <div>
    <opensilex-FormInputLabelHelper
        label="component.person.orcid"
        helpMessage="component.person.orcid-help-message"
        class="checkbox">
    </opensilex-FormInputLabelHelper>
    <b-form-input
        v-model="form.orcid"
        type="text"
        :disabled="disable_orcid_field"
        :placeholder="$t('component.person.orcid-placeholder')"
    ></b-form-input>
    </div>

    <!-- First name -->
    <opensilex-InputForm
      :value.sync="form.first_name"
      label="component.person.first-name"
      type="text"
      :required="true"
      placeholder="component.person.form-first-name-placeholder"
    ></opensilex-InputForm>

    <!-- Last name -->
    <opensilex-InputForm
      :value.sync="form.last_name"
      label="component.person.last-name"
      type="text"
      :required="true"
      placeholder="component.person.form-last-name-placeholder"
    ></opensilex-InputForm>

    <!-- Email -->
    <opensilex-InputForm
        :value.sync="form.email"
        label="component.person.email-address"
        type="email"
        rules="email"
        placeholder="component.person.form-email-placeholder"
        autocomplete="new-password"
    ></opensilex-InputForm>

    <!-- First name -->
    <opensilex-InputForm
        :value.sync="form.organization"
        label="component.person.organization"
        placeholder="component.person.form-organization-placeholder"
        type="text"
    ></opensilex-InputForm>

    <!-- Last name -->
    <opensilex-InputForm
        :value.sync="form.phone_number"
        label="component.person.phone_number"
        placeholder="component.person.form-phone-placeholder"
        type="text"
    ></opensilex-InputForm>

  </b-form>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {SecurityService} from "opensilex-security/api/security.service";

@Component
export default class PersonForm extends Vue {
  $opensilex: OpenSilexVuePlugin;

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
        organization: "",
        phone_number: ""
      };
    }
  })
  form;

  disable_orcid_field : boolean = false

  reset() {
    this.uriGenerated = true;
    this.$nextTick( () => {
      this.disable_orcid_field = this.editMode && this.form.orcid !== null
    })
  }

  getEmptyForm() {
    return {
      uri: null,
      email: null,
      first_name: "",
      last_name: "",
      organization: "",
      phone_number: ""
    };
  }

  create(form) {
    if (form.email === ""){
      form.email = null;
    }

    if (form.orcid === ""){
      form.orcid = null;
    }

    return this.$opensilex
      .getService<SecurityService>("opensilex.SecurityService")
      .createPerson(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Person created", uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Person already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$t("component.person.errors.person-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    return this.$opensilex
      .getService<SecurityService>("opensilex.SecurityService")
      .updatePerson(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Person updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">

</style>

