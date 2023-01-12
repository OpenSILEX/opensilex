<template>
  <opensilex-DefaultLoginComponent>
    <template v-slot:loginMedia>
        <img
            v-bind:src="$opensilex.getResourceURI('images/opensilex-login-bg.jpg')"
        />
    </template>

    <template v-slot:loginLogo>
        <img
            v-bind:src="
            $opensilex.getResourceURI('images/logo-phis-lg.png')
            "
            alt
        />
    </template>

    <template v-slot:loginFooter>   
        <p>
          {{ $t("LoginComponent.copyright.1" ) }}
          <br />
          {{ $t("LoginComponent.copyright.2", {
          version: getPHISModuleVersion()
          }) }}
          <br />
          {{ $t("LoginComponent.copyright.3", {
          version: versionInfo.version
          }) }}
          <br />
          {{
          $t("LoginComponent.copyright.4", {
          version: versionInfo.version
          })
          }}
        </p>
    </template>
  </opensilex-DefaultLoginComponent>
  
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { VersionInfoDTO } from "opensilex-core/index";
import OpenSilexVuePlugin from "../../../../../opensilex-front/front/src/models/OpenSilexVuePlugin";

@Component
export default class PhisLoginComponent extends Vue {

  versionInfo: VersionInfoDTO = {};
  $opensilex: OpenSilexVuePlugin;

  created() {
    this.versionInfo = this.$opensilex.versionInfo;
  }

  getPHISModuleVersion(){
    for(let module_version_index in this.versionInfo.modules_version){
      let module = this.versionInfo.modules_version[module_version_index]

      if(module.name.includes("PhisWsModule")){
        return module.version;
      }
    }
    return 'Version undefined'
  }

}
</script>

<style scoped lang="scss">
</style>

<i18n>
</i18n>