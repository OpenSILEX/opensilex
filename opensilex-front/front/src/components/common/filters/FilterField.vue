<template>
  <div class = "col col-12" v-bind:class="{'col-xl-3 col-sm-6' :baseWidth,'col-xl-12 col-sm-12': fullWidth,'col-xl-6 col-sm-6':halfWidth,'col-xl-3 col-sm-3':quarterWidth}" >
    <slot></slot>
  </div>
</template>

<script lang="ts">
import {
  Component,
  Prop
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

  @Prop({ default: false })
  quarterWidth: boolean;

  get baseWidth() {
    return !this.fullWidth && !this.halfWidth && !this.quarterWidth
  }
}
</script>


<style scoped lang="scss">
</style>

