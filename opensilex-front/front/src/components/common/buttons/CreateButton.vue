<template>
  <opensilex-Button
    @click="$emit('click')"
    class="greenThemeColor"
    icon="ik#ik-plus"
    :small="small"
    :label="label"
    :disabled="disabled"
  ></opensilex-Button>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class CreateButton extends Vue {
  @Prop()
  label: string;

  @Prop({
    default: false
  })
  small: boolean;

  @Prop()
  disabled: boolean;
}
</script>

<style scoped lang="scss">
</style>

