<template>
  <ValidationObserver ref="validatorRef">
    <!-- URI -->
    <opensilex-UriForm
      :uri.sync="form.uri"
      label="component.group.group-uri"
      helpMessage="component.common.uri-help-message"
      :editMode="editMode"
      :generated.sync="uriGenerated">
    </opensilex-UriForm>

    <!-- Name -->
    <opensilex-InputForm
      :value.sync="form.name"
      label="component.common.name"
      type="text"
      :required="true"
      placeholder="component.group.form-name-placeholder">
    </opensilex-InputForm>

    <!-- Description -->
    <opensilex-TextAreaForm
      :value.sync="form.description"
      label="component.common.description"
      :required="false"
      placeholder="component.group.form-description-placeholder">
    </opensilex-TextAreaForm>

    <!-- Variables -->
    <opensilex-VariableSelectorWithFilter
      placeholder="VariableSelectorWithFilter.placeholder-multiple"
      :variables.sync="form.variables"
    ></opensilex-VariableSelectorWithFilter>

  </ValidationObserver>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";


@Component
export default class GroupVariablesForm extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;

  @Prop()
  editMode;

  @Prop({ default: true })
  uriGenerated;

  @Ref("validatorRef") readonly validatorRef!: any;

  get user() {
    return this.$store.state.user;
  }

  @Prop({
    default: () => {
      return {
        uri: null,
        name: null,
        description: null,
        variables: []
      };
    }
  })
  form;

  static getEmptyForm(){
    return {
      uri: null,
      name: null,
      description: null,
      variables: []
    };
  }

  getEmptyForm(){
    return GroupVariablesForm.getEmptyForm();
  }

  create(form){
    return this.$opensilex
      .getService("opensilex.VariablesService")
      .createVariablesGroup(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let message = this.$i18n.t(this.form.name) + this.$i18n.t("component.common.success.creation-success-message");
        this.$opensilex.showSuccessToast(message);
        let uri = http.response.result;
        this.$emit("onCreate", uri);
        this.$router.push({path: "/variables?elementType=VariableGroup&selected=" + encodeURIComponent(uri)});
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Variables group already exists", error);
          this.$opensilex.errorHandler(error, "Variables group already exists");
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form){
    return this.$opensilex
      .getService("opensilex.VariablesService")
      .updateVariablesGroup(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let message = this.$i18n.t(this.form.name) + this.$i18n.t("component.common.success.update-success-message");
        this.$opensilex.showSuccessToast(message);
        let uri = http.response.result;
        this.$emit("onUpdate", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    GroupVariablesForm:
        add: Add variables group
        edit: Edit variables group
fr:
    GroupVariablesForm:
        add: Ajouter un groupe de variables
        edit: Éditer un groupe de variables
</i18n>