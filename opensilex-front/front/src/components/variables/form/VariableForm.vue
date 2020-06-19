<template>
  <opensilex-WizardForm
    ref="wizardRef"
    :steps="steps"
    createTitle="component.variable.form.add.variable"
    editTitle="component.variable.form.add.variable"
    icon="fa#vials"
    modalSize="lg"
    :initForm="getEmptyForm"
    :createAction="create"
    :updateAction="update"
  >
    <template v-slot:icon></template>
  </opensilex-WizardForm>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";
import { VariablesService } from "opensilex-core/index";

@Component
export default class VariableCreate extends Vue {
  $opensilex: any;
  service: VariablesService;

  @Ref("wizardRef") readonly wizardRef!: any;

  created() {
    this.service = this.$opensilex.getService("opensilex.VariablesService");
  }

  showCreateForm() {
    this.wizardRef.showCreateForm();
  }

  showEditForm(form) {
    this.wizardRef.showEditForm(form);
  }

  steps = [
    {
      component: "opensilex-VariableForm1"
    },
    {
      component: "opensilex-VariableForm2"
    }
  ];

  @Prop()
  editMode;

  getEmptyForm() {
    return {
      uri: undefined,
      entity: undefined,
      quality: undefined,
      longName: "",
      synonym: "",
      label: "",
      comment: "",
      dimension: undefined,
      trait: {
        traitUri: undefined,
        traitLabel: "",
        traitClass: undefined
      },
      method: undefined,
      unit: undefined,
      lowerBound: undefined,
      upperBound: undefined
    };
  }

  create(form) {
    if (form.entity) {
      form.entity = form.entity.uri;
    }
    if (form.quality) {
      form.quality = form.quality.uri;
    }
    if (form.method) {
      form.method = form.method.uri;
    }
    if (form.unit) {
      form.unit = form.unit.uri;
    }
    if (form.trait.traitClass) {
      form.trait.traitClass = form.trait.traitClass.uri;
    }

    return this.service
      .createVariable(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        form.uri = http.response.result;
        this.$emit("onCreate", form);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Variable already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$i18n.t("component.project.errors.project-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    return this.service
      .updateVariable(form.uri, form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Variable updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>
