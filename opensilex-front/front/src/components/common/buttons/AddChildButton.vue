<template>
  <opensilex-Button
    @click="$emit('click')"
    variant="outline-success"
    icon="fa#plus"
    :small="small"
    :label="label"
    :disabled="disabled"
  ></opensilex-Button>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class AddChildButton extends Vue {
  @Prop()
  label: string;

  @Prop()
  small: boolean;

  @Prop()
  disabled: boolean;
}
</script>

<style scoped lang="scss">
</style>

