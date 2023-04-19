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
      placeholder="component.group.form-description-placeholder"
      @keydown.native.enter.stop>
    </opensilex-TextAreaForm>

    <!-- Variables -->

    <!-- We use the old VariableSelector in editMode because of the edit bug -->
    <opensilex-VariableSelector
      v-if="editMode"
      label="DataView.filter.variables"
      :variables.sync="form.variables"
      :multiple="true"
    ></opensilex-VariableSelector>

    <opensilex-VariableSelectorWithFilter
      v-else
      placeholder="VariableSelectorWithFilter.placeholder-multiple"
      :variables.sync="form.variables"
    ></opensilex-VariableSelectorWithFilter>

  </ValidationObserver>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import {VariablesGroupCreationDTO} from "opensilex-core/model/variablesGroupCreationDTO";
import {VariablesGroupUpdateDTO} from "opensilex-core/model/variablesGroupUpdateDTO";


@Component
export default class GroupVariablesForm extends Vue {
  $opensilex: OpenSilexVuePlugin;
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

  @Prop()
  form;

  static getEmptyForm(){
    return {
      uri: null,
      name: null,
      description: null,
      variables: []
    };
  }

  getEmptyForm(): VariablesGroupCreationDTO {
    return GroupVariablesForm.getEmptyForm();
  }

  create(form: VariablesGroupCreationDTO){
    return this.$opensilex
      .getService<VariablesService>("opensilex.VariablesService")
      .createVariablesGroup(form)
      .then((http: HttpResponse<OpenSilexResponse<string>>) => {
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

  update(form: VariablesGroupUpdateDTO){
    return this.$opensilex
      .getService<VariablesService>("opensilex.VariablesService")
      .updateVariablesGroup(form)
      .then((http: HttpResponse<OpenSilexResponse<string>>) => {
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