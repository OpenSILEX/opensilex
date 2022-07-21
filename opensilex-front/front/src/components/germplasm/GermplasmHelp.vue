<template>
  <div>
    <div v-if="lang === 'fr'">
      <GermplasmHelpFR></GermplasmHelpFR>
    </div>
    <div v-else>
      <GermplasmHelpEN></GermplasmHelpEN> 
    </div>
    <div class="GermplasmHelpOkButton">
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

  hideBtnClick(){
    this.$emit("hideBtnIsClicked");
  }
}
</script>

<style scoped lang="scss">

.GermplasmHelpOkButton {
  float: right;
  height: 30px;
}
</style>

