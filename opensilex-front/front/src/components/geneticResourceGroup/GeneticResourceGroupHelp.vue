<template>
  <div>
    <div v-if="lang === 'fr'">
      <GroupGeneticResourceHelpFR></GroupGeneticResourceHelpFR>
    </div>
    <div v-else>
      <GroupGeneticResourceHelpEN></GroupGeneticResourceHelpEN>
    </div>
    <div class="GroupGeneticResourceHelpOkButton">
      <button
          type="button"
          class="btn greenThemeColor"
          v-on:click="hideButtonClick"
      >
        {{ $t('component.common.ok') }}
      </button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import GroupGeneticResourceHelpFR from "./doc/GroupGeneticResourceHelpFR.md";
import GroupGeneticResourceHelpEN from "./doc/GroupGeneticResourceHelpEN.md";
import OpenSilexVuePlugin from 'src/models/OpenSilexVuePlugin';


Vue.component("GroupGeneticResourceHelpEN", GroupGeneticResourceHelpEN);
Vue.component("GroupGeneticResourceHelpFR", GroupGeneticResourceHelpFR);

@Component
export default class GroupGeneticResourceHelp extends Vue {
  $opensilex: OpenSilexVuePlugin;
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

  hideButtonClick(){
    this.$emit("hideButtonIsClicked");
  }
}
</script>

<style scoped lang="scss">
.GroupGeneticResourceHelpOkButton {
  float: right;
  height: 30px;
}
</style>

