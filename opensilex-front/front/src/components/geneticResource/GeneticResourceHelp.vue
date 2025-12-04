<template>
  <div>
    <div v-if="lang === 'fr'">
      <GeneticResourceHelpFR></GeneticResourceHelpFR>
    </div>
    <div v-else>
      <GeneticResourceHelpEN></GeneticResourceHelpEN> 
    </div>
    <div class="GeneticResourceHelpOkButton">
      <button
        type="button"
        class="btn greenThemeColor"
        v-on:click="hideBtnClick"
      >
        {{ $t('component.common.ok') }}
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import GeneticResourceHelpFR from "./doc/GeneticResourceHelpFR.md";
import GeneticResourceHelpEN from "./doc/GeneticResourceHelpEN.md";  

Vue.component("GeneticResourceHelpEN", GeneticResourceHelpEN);
Vue.component("GeneticResourceHelpFR", GeneticResourceHelpFR); 

@Component
export default class GeneticResourceHelp extends Vue {
  $opensilex: any;
  $store: any;
  $t: any;
  $i18n: any;
  $router: any;

  get lang() {
    console.debug("Active language : " + this.$store.getters.language);
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

.GeneticResourceHelpOkButton {
  float: right;
  height: 30px;
}
</style>

