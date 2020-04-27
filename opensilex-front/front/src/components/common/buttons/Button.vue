<template>
  <b-button @click.prevent="$emit('click')" :title="$t(label)" :size="small ? 'sm' : 'md'" :variant="variant" :disabled="disabled">
    <slot name="icon"><i :class="iconClass"></i></slot>
    <span v-if="!small">{{$t(label)}}</span>
  </b-button>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class Button extends Vue {
  @Prop()
  label: string;

  @Prop()
  variant: string;

  @Prop()
  small: boolean;

  @Prop()
  disabled: boolean;

  @Prop()
  iconClass: string;
}
</script>

<style scoped lang="scss">

span {
    margin-left: 5px;
}
</style>

