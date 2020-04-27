<template>
  <opensilex-Button
    @click="$emit('click')"
    variant="outline-success"
    :small="small"
    :label="label"
    :disabled="disabled"
  >
    <template v-slot:icon>
      <font-awesome-icon icon="plus" size="sm" />
    </template>
  </opensilex-Button>
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

