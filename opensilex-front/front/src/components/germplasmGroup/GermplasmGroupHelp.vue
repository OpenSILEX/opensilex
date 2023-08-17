<template>
  <div>
    <div v-if="lang === 'fr'">
      <GroupGermplasmHelpFR></GroupGermplasmHelpFR>
    </div>
    <div v-else>
      <GroupGermplasmHelpEN></GroupGermplasmHelpEN>
    </div>
    <div class="GroupGermplasmHelpOkButton">
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
import GroupGermplasmHelpFR from "./doc/GroupGermplasmHelpFR.md";
import GroupGermplasmHelpEN from "./doc/GroupGermplasmHelpEN.md";
import OpenSilexVuePlugin from 'src/models/OpenSilexVuePlugin';


Vue.component("GroupGermplasmHelpEN", GroupGermplasmHelpEN);
Vue.component("GroupGermplasmHelpFR", GroupGermplasmHelpFR);

@Component
export default class GroupGermplasmHelp extends Vue {
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
.GroupGermplasmHelpOkButton {
  float: right;
  height: 30px;
}
</style>

