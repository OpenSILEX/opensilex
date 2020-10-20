<template>
  <div v-if="lang === 'fr'">
    <InfrastructureHelpFR></InfrastructureHelpFR>
  </div>
  <div v-else>
    <InfrastructureHelpEN></InfrastructureHelpEN> 
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import InfrastructureFacilityHelpFR from "./doc/InfrastructureFacilityHelpFR.md";
import InfrastructureFacilityHelpEN from "./doc/InfrastructureFacilityHelpEN.md";  

Vue.component("InfrastructureFacilityHelpFR", InfrastructureFacilityHelpFR);
Vue.component("InfrastructureFacilityHelpEN", InfrastructureFacilityHelpEN); 

@Component
export default class InfrastructureFacilityHelp extends Vue {
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
}
</script>

<style scoped lang="scss">
</style>

