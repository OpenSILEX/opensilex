<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
    :validationDisabled="validationDisabled"
  >
    <template v-slot:field="field">
      <b-form-file
        :id="field.id"
        :required="required"
        v-model="fileValue"
        :state="Boolean(fileValue)"
        :placeholder="$t(placeholder)"
        :drop-placeholder="$t(dropPlaceholder)"
        :browse-text="$t(browseText)"
      ></b-form-file>
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
export default class FileInputForm extends Vue {
  $opensilex: any;

  @PropSync("file")
  fileValue: string;

  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop()
  placeholder: string;

  @Prop()
  dropPlaceholder: string;

  @Prop()
  browseText: string;

  @Prop()
  required: boolean;

  @Prop()
  disabled: boolean;

  @Prop()
  rules: string | Function;

  @Prop({
    default: false
  })
  validationDisabled: boolean;
}
</script>

<style scoped lang="scss">
</style>

