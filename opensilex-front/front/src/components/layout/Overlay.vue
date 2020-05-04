<template>
  <b-overlay :show="show" :no-fade="noFade" :z-index="zIndex" opacity="0.7">
    <template v-slot:overlay>
      <div class="lds-ripple">
        <div></div>
        <div></div>
      </div>
    </template>
    <slot></slot>
  </b-overlay>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class Overlay extends Vue {
  @Prop({
    default: false
  })
  show;

  @Prop({
    default: true
  })
  noFade;

  @Prop({
    default: 10
  })
  zIndex;

}
</script>

<style scoped lang="scss">
.lds-ripple {
  display: inline-block;
  position: relative;
  width: 80px;
  height: 80px;
  margin: auto;
}

.lds-ripple div {
  position: absolute;
  border: 4px solid #000;
  opacity: 1;
  border-radius: 50%;
  animation: lds-ripple 1s cubic-bezier(0, 0.2, 0.8, 1) infinite;
}
.lds-ripple div:nth-child(2) {
  animation-delay: -0.5s;
}
@keyframes lds-ripple {
  0% {
    top: 36px;
    left: 36px;
    width: 0;
    height: 0;
    opacity: 1;
  }
  100% {
    top: 0px;
    left: 0px;
    width: 72px;
    height: 72px;
    opacity: 0;
  }
}
</style>
