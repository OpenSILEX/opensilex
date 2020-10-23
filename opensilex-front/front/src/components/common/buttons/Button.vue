<template>
  <b-button
    @click.prevent="$emit('click')"
    :title="$t(label)"
    :variant="variant"
    :disabled="disabled"
  >
    <slot name="icon">
      <opensilex-Icon :icon="icon" />
    </slot>
    <span class="button-label" v-if="size==='md'">{{$t(label)}}</span>
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

  @Prop({
    default: false,
  })
  disabled: boolean;

  @Prop()
  icon: string;

  get size(){
   if(this.small == undefined ||this.small == null){
     return undefined;
   }
   if(this.small == true){
  return 'sm';
   }else{
     return 'md';
   } }
}
</script>

<style scoped lang="scss">
.button-label {
  margin-left: 5px;
  padding-right: 13px;
}
</style>

