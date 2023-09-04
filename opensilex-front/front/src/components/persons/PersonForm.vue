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
        :value.sync="form.affiliation"
        label="component.person.affiliation"
        placeholder="component.person.form-affiliation-placeholder"
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
  $securityService: SecurityService;

  uriGenerated = true;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        email: null,
        first_name: null,
        last_name: null,
        affiliation: null,
        phone_number: null
      };
    }
  })
  form;

  disable_orcid_field : boolean = false

  created(){
    this.$securityService = this.$opensilex.getService<SecurityService>("opensilex.SecurityService")
  }

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
      first_name: null,
      last_name: null,
      affiliation: null,
      phone_number: null
    };
  }

  async create(form) {
    this.replaceEmptyStringByNull(form)
    form.orcid = this.getCompleteUrlOrcid(form.orcid)

    try {
      let response = this.$securityService.createPerson(form)
      this.$emit("onCreate", form)
      return response
    } catch(error) {
        if (error.status == 409) {
          this.$opensilex.errorHandler(
            error,
            this.$t("component.person.errors.person-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      }
  }

  update(form) {
    this.replaceEmptyStringByNull(form)
    form.orcid = this.getCompleteUrlOrcid(form.orcid)

    return this.$opensilex
      .getService<SecurityService>("opensilex.SecurityService")
      .updatePerson(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Person updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }

  private getCompleteUrlOrcid(orcid) {
    if (orcid === ""){
      return  null;
    }
    //regex : 3 séquences de 4 chiffres séparées par un tiret puis une séquence de 4 chiffres ou 3 chiffres et un X
    //exemples validés : 0009-0006-6636-4714 ou 0009-0006-6636-471X
    let regexOrcidWithoutCompleteUrl = /^([0-9]{4}-){3}[0-9]{3}[0-9X]$/
    if (regexOrcidWithoutCompleteUrl.test(orcid)){
      return  "https://orcid.org/"+orcid
    }

    return orcid
  }

  private replaceEmptyStringByNull(form){
    if (form.email === ""){
      form.email = null;
    }

    if (form.phone_number === ""){
      form.phone_number = null;
    }
  }

}
</script>

<style scoped lang="scss">

</style>

