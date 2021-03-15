<template> 
      <div v-if="lang === 'fr'"  >
        <DataVisuHelpFR></DataVisuHelpFR>
      </div>
      <div v-else >
        <DataVisuHelpEN></DataVisuHelpEN>
      </div>
     
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import DataVisuHelpFR from "./doc/DataVisuHelpFR.md";
import DataVisuHelpEN from "./doc/DataVisuHelpEN.md";

Vue.component("DataVisuHelpEN", DataVisuHelpEN);
Vue.component("DataVisuHelpFR", DataVisuHelpFR);

@Component
export default class DataVisuHelp extends Vue {
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

