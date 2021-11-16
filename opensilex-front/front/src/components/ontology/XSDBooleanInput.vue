<template>
    <opensilex-CheckboxForm
      :value.sync="internalValue"
      :label="property.name"
      :title="internalValue ? $t('component.common.yes') : $t('component.common.no')"
      :helpMessage="property.comment"
    ></opensilex-CheckboxForm>
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
export default class XSDBooleanInput extends Vue {
  $opensilex: any;

  @Prop()
  property;

  @PropSync("value")
  internalValue;

}
</script>

<style scoped lang="scss">
</style>

