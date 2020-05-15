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
    <opensilex-NameInputForm
      :value.sync="form.names"
      label="component.factor.names.en"
      helpMessage="component.factor.names.en-help"
      type="text"
      :required="true"
      placeholder="component.factor.names.en-placeholder"
    ></opensilex-NameInputForm>

    <!-- Local name -->
    <opensilex-NameInputForm
      :value.sync="form.names"
      label="component.factor.names.local-name"
      type="text"
      helpMessage="component.factor.names.local-name-help"
      placeholder="component.factor.names.local-name-placeholder"
      :localName="true"
    ></opensilex-NameInputForm>

    <!-- Comment -->
    <opensilex-TextAreaForm
      :value.sync="form.comment"
      label="component.factor.comment"
      helpMessage="component.factor.comment-help"
      placeholder="component.factor.comment-placeholder"
    ></opensilex-TextAreaForm>

    <opensilex-FactorLevelTable :editMode="editMode" :factorLevels.sync="form.factorLevels"></opensilex-FactorLevelTable>
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
      let names = {};
      let defaultLang = "en";
      names[defaultLang] = "";
      return {
        uri: null,
        names: names,
        comment: "",
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
    let names = {};
    let lang = this.languageCode;
    let defaultLang = "en";
    names[lang] = "";
    names[defaultLang] = "";
    return {
      uri: null,
      names: names,
      comment: "",
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

  updatefactor() {
    return new Promise((resolve, reject) => {
      console.log("update", this.form);
      return this.$emit("onUpdate", this.form, result => {
        if (result instanceof Promise) {
          return result
            .then(resolve => {
              console.log("resolve", resolve);
            })
            .catch(reject => {
              console.log("resolve", reject);
            });
        } else {
          return resolve(result);
        }
      });
    });
  }

  createFactor() {
    return new Promise((resolve, reject) => {
      return this.$emit("onCreate", this.form, result => {
        if (result instanceof Promise) {
          return result
            .then(uri => {
              this.afterCreate(uri);
              return uri;
            })
            .catch(reject);
        } else {
          return resolve(result);
        }
      });
    });
  }

  afterCreate(uri: string) {
    this.setUri(uri);
    this.$emit("onDetails", uri);
  }

  create(form) {
    console.log("factor", form);
    return this.$opensilex
      .getService("opensilex.FactorsService")
      .createFactor(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Factor created", uri);
        form.uri = uri;
        return uri;
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

  get languageCode(): string {
    return this.$opensilex.getLocalLangCode();
  }
}
</script>

<style scoped lang="scss">
@import "~vue-tabulator/dist/scss/bootstrap/tabulator_bootstrap4";
</style>
