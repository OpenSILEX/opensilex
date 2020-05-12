<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <b-input-group class="mt-3">
        <b-input-group-prepend>
          <country-flag country="en" size="normal" v-b-tooltip.hover title="en" />
        </b-input-group-prepend>
        <b-form-input
          :id="field.id"
          v-model="value"
          :disabled="disabled"
          :type="type"
          :required="required"
          :placeholder="$t(placeholder)"
          :autocomplete="autocomplete"
        ></b-form-input>
      </b-input-group>
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DefaultNameInputForm extends Vue {
  $opensilex: any;
  $i18n: any;

  @PropSync("value")
  value: string;

  @Prop({
    default: "text"
  })
  type: string;

  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop()
  placeholder: string;

  @Prop()
  required: boolean;

  @Prop()
  disabled: boolean;

  @Prop()
  rules: string | Function;

  @Prop()
  autocomplete: string;

 

}
</script>

<style scoped lang="scss">
</style>

