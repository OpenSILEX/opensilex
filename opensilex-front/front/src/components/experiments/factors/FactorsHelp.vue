<template>
<div>
  <div v-if="lang === 'fr'">
    <FactorHelpFR></FactorHelpFR>
  </div>
  <div v-else>
    <FactorHelpEN></FactorHelpEN> 
  </div>
    <button
      type="button"
      class="btn greenThemeColor dataVisuHelpOkButton"
      v-on:click="hideBtnClick"
    >
      {{ $t('component.common.ok') }}
    </button>
</div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import FactorHelpFR from "./doc/FactorHelpFR.md";
import FactorHelpEN from "./doc/FactorHelpEN.md";  

Vue.component("FactorHelpEN", FactorHelpEN);
Vue.component("FactorHelpFR", FactorHelpFR); 

@Component
export default class FactorsHelp extends Vue {
  $opensilex: any;
  $store: any;
  $t: any;
  $i18n: any;
  $router: any;

  get lang() {
    return this.$store.getters.language;
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  source = new Date().toLocaleTimeString();

   hideBtnClick(){
    this.$emit("hideBtnIsClicked");
  }
}
</script>

<style scoped lang="scss">
.dataVisuHelpOkButton {
  float:right;
  height: 35px;
}
</style>

