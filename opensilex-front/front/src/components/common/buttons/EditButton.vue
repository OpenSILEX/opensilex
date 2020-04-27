<template>
  <opensilex-Button
    @click="$emit('click')"
    variant="outline-primary"
    :small="small"
    :label="label"
    :disabled="disabled"
  >
    <template v-slot:icon>
      <font-awesome-icon icon="edit" size="sm" />
    </template>
  </opensilex-Button>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class EditButton extends Vue {
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

