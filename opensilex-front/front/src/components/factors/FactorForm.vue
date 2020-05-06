<template>
  <b-form>
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.factor.uri"
      helpMessage="component.common.uri.help-message"
      :editMode="editMode"
      :generated.sync="uriGenerated"
    ></opensilex-UriForm>

    <!-- Name en -->
    <opensilex-InputForm
      :value.sync="form.names.en"
      label="component.factor.names.en"
      helpMessage="component.factor.names.en-help"
      type="text"
      :required="true"
      placeholder="component.factor.names.en-placeholder"
    ></opensilex-InputForm>

    <!-- Local name -->
    <opensilex-LocalNameInputForm
      :value.sync="form.names"
      label="component.factor.names.local-name"
      helpMessage="component.factor.names.local-name.help"
      type="text"
      placeholder="component.factor.names.local-name-placeholder"
    ></opensilex-LocalNameInputForm>

    <!-- Local name -->
    <opensilex-InputForm
      :value.sync="form.comment"
      label="form.comment"
      helpMessage="component.factor.comment-help"
      type="textarea"
      size="lg"
      rules="required|min:10"
      :required="true"
      placeholder="component.factor.names.en-placeholder"
    ></opensilex-InputForm>

    <opensilex-FactorLevelTable :factorLevels.sync="form.factorLevels"></opensilex-FactorLevelTable>
  </b-form>
</template>


<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import { FactorsService } from "opensilex-core/index";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class FactorForm extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;

  get user() {
    return this.$store.state.user;
  }

  uriGenerated = true;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        names: { en: null, fr: null },
        comment: null,
        exactMatch: [],
        closeMatch: [],
        broader: [],
        narrower: [],
        factorLevels: []
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
      names: { en: null, fr: null },
      comment: null,
      exactMatch: [],
      closeMatch: [],
      broader: [],
      narrower: [],
      factorLevels: []
    };
  }

  setUri(uri: string) {
    this.form.uri = uri;
  }

  afterCreate(uri: string) {
    this.setUri(uri);
    this.$emit("onDetails", uri);
  }

  create(form) {
    console.log(form);
    return this.$opensilex
      .getService("opensilex.FactorsService")
      .createFactor(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Factor created", uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Factor already exists", error);
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
    return this.$opensilex
      .getService("opensilex.FactorsService")
      .updateFactor(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Factor updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
@import "~vue-tabulator/dist/scss/bootstrap/tabulator_bootstrap4";
</style>
