<template>
  <div class="col col-xl-3 col-sm-6 col-12" v-bind:class="{'col-xl-12 col-sm-12': fullWidth,'col-xl-6 col-sm-6':halfWidth}" >
    <slot></slot>
  </div>
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
export default class FilterField extends Vue {
  @Prop({default:"component.search.lab"})
  label: string;

  @Prop({ default: false })
  fullWidth: boolean;
  
  @Prop({ default: false })
  halfWidth: boolean;
}
</script>


<style scoped lang="scss">
</style>

