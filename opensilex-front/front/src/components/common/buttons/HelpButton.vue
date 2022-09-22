<template>
  <opensilex-Button
    @click="$emit('click')"
    :label="label"
    class="helpButton"
    :variant="variant"
    :disabled="disabled"
    icon="fa#question"
  ></opensilex-Button>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class HelpButton extends Vue {
  @Prop()
  label: string;

  @Prop()
  small: boolean;

  @Prop()
  disabled: boolean;

  @Prop({ default: "outline-info" })
  variant: string;
}
</script>

<style scoped lang="scss">
</style>

