<template>
  <opensilex-DefaultMenuComponent>
  </opensilex-DefaultMenuComponent>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../../../opensilex-front/front/src/models/OpenSilexVuePlugin";

@Component
export default class SiduriMenuComponent extends Vue {
  $opensilex: OpenSilexVuePlugin;

  static async asyncInit($opensilex: any) {
    await $opensilex.loadModule("opensilex-data-analysis");
  }

}
</script>
<style lang="scss">
.hamburger-container {
  background-color: #F4F2EE !important;
}

.hamburger .hamburger-inner,
.hamburger .hamburger-inner::after,
.hamburger .hamburger-inner::before,
.hamburger.is-active .hamburger-inner,
.hamburger.is-active .hamburger-inner::after,
.hamburger.is-active .hamburger-inner::before {
  background-color: #E9173A !important;
}

.hamburger:hover .hamburger-inner,
.hamburger:hover .hamburger-inner::after,
.hamburger:hover .hamburger-inner::before,
.hamburger.is-active:hover .hamburger-inner,
.hamburger.is-active:hover .hamburger-inner::after,
.hamburger.is-active:hover .hamburger-inner::before {
  background-color: rgba(233, 23, 58, 0.6) !important;
}

</style>
