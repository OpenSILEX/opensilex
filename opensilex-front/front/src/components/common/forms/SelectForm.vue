<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <treeselect
        :id="field.id"
        :options="options"
        :placeholder="$t(placeholder)"
        :isDisabled="disabled"
        v-model="selection"
      />
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
export default class SelectForm extends Vue {
  $opensilex: any;

  @PropSync("selected")
  selection: string;

  @Prop()
  options: string;

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
}
</script>

<style scoped lang="scss">
</style>

