<template>
  <span>
    <i v-if="type() == 'ik'" :class="ikClasses()"></i>
    <font-awesome-icon v-if="type() == 'fa'" :icon="faClass()" :size="size" />
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class Icon extends Vue {
  @Prop({
    default: "ik#ik-folder"
  })
  icon: string;

  @Prop({ default: "sm" })
  size: string;

  type() {
    let splitIcon = this.icon.split("#");
    return splitIcon[0];
  }

  faClass() {
    let splitIcon = this.icon.split("#");
    return splitIcon[1];
  }

  ikClasses() {
    let splitIcon = this.icon.split("#");
    return "ik " + splitIcon[1] + " " + this.size;
  }
}
</script>

<style scoped lang="scss">
span {
  margin: 0;
  padding: 0;
  display: inline;
  border: none;
}
</style>

