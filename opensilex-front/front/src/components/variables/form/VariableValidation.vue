<template>
  <b-modal ref="validatorRef" title="Valider la variable">
    <opensilex-CheckboxForm
        :value.sync="form.admin"
        label="component.user.admin"
        title="component.user.admin"
    ></opensilex-CheckboxForm>
    <opensilex-TextAreaForm
        :value.sync="form.description"
        label="component.common.description"
        :required="false"
        placeholder="component.group.form-description-placeholder">
    </opensilex-TextAreaForm>
  </b-modal>
</template>

<script lang="ts">

import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class VariableValidation extends Vue {
  @Ref("validatorRef") readonly validatorRef!: any;

  form = {
    admin: undefined,
    description: undefined
  };

  showValidationForm() {
    this.validatorRef.showValidationForm();
  }
}
</script>

<style scoped>

</style>