<template>
  <b-form-group :required="required">
    <opensilex-FormInputLabelHelper
      v-if="label"
      :label="label"
      :helpMessage="helpMessage"
      :labelFor="id"
    ></opensilex-FormInputLabelHelper>
    <ValidationProvider
      ref="validatorRef"
      v-if="required || rules"
      :name="$t(label)"
      :rules="getRules()"
      v-slot="{ errors }"
      :vid="vid"
      :disabled="validationDisabled"
    >
      <div v-bind:class="{ errors: errors.length > 0 }">
        <slot name="field" v-bind:id="id" v-bind:validator="validatorRef"></slot>
      </div>
      <div class="error-message alert alert-danger">{{ errors[0] }}</div>
    </ValidationProvider>
    <slot v-else name="field" v-bind:id="id"></slot>
  </b-form-group>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
  Ref
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class FormField extends Vue {
  $opensilex: any;

  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop({
    default: false
  })
  required: boolean;

  @Prop({
    default: false
  })
  validationDisabled: boolean;

  @Prop()
  rules: string | Function;

  @Ref("validatorRef") readonly validatorRef!: any;

  id: string;

  @Prop()
  vid: string;

  created() {
    this.id = this.$opensilex.generateID();
  }

  getRules() {
    let rules = "";

    if (this.rules) {
      let rulesValue: string;
      if (this.rules instanceof Function) {
        rulesValue = this.rules();
      } else {
        rulesValue = "" + this.rules;
      }

      if (this.required) {
        rules = "required|" + rulesValue;
      } else {
        rules = rulesValue;
      }
    } else if (this.required) {
      rules = "required";
    }

    return rules;
  }
}
</script>

<style scoped lang="scss">
</style>

