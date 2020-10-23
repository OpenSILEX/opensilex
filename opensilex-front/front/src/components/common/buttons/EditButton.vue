<template>
  <opensilex-Button
    @click="$emit('click')"
    variant="outline-primary"
    :small="small"
    :nosize="nosize"
    :label="label"
    :disabled="disabled"
    icon="fa#pencil-alt"
  ></opensilex-Button>
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
  nosize: boolean;
  @Prop()
  disabled: boolean;
}
</script>

<style scoped lang="scss">
</style>

