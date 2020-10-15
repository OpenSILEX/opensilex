<template>
  <div v-if="lang === 'fr'">
    <GermplasmHelpFR></GermplasmHelpFR>
  </div>
  <div v-else>
    <GermplasmHelpEN></GermplasmHelpEN> 
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import GermplasmHelpFR from "./doc/GermplasmHelpFR.md";
import GermplasmHelpEN from "./doc/GermplasmHelpEN.md";  

Vue.component("GermplasmHelpEN", GermplasmHelpEN);
Vue.component("GermplasmHelpFR", GermplasmHelpFR); 

@Component
export default class GermplasmHelp extends Vue {
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
}
</script>

<style scoped lang="scss">
</style>

