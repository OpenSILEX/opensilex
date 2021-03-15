<template>
  <opensilex-FormField
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <b-form-tags
        :input-id="field.id"
        v-model="tags"
        :disabled="disabled"
        :required="required"
        :placeholder="$t(placeholder)"
        :tag-variant="variant"
      ></b-form-tags>
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
export default class TagInputForm extends Vue {
  $opensilex: any;

  @PropSync("value")
  tags: Array<string>;
  
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
  variant: string;

}
</script>

<style scoped lang="scss">
</style>

