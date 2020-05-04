<template>
  <opensilex-Button
    @click="$emit('click', {})"
    variant="outline-success"
    :small="small"
    :label="label"
    :disabled="disabled"
  >
    <template v-slot:icon>
      <opensilex-Icon v-if="!detailVisible" icon="fa#eye" />
      <opensilex-Icon v-if="detailVisible" icon="fa#eye-slash" />
    </template>
  </opensilex-Button>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DetailButton extends Vue {
  @Prop()
  label: string;

  @Prop()
  small: boolean;

  @Prop()
  disabled: boolean;

  @Prop()
  detailVisible: boolean;
}
</script>

<style scoped lang="scss">
</style>

