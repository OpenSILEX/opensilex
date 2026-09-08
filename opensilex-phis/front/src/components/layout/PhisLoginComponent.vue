<template>
  <opensilex-DefaultLoginComponent>
    <template v-slot:loginMedia>
        <img
            v-bind:src="opensilex.getResourceURI('images/opensilex-login-bg.jpg')"
        />
    </template>

    <template v-slot:loginLogo>
        <img
            v-bind:src="
            opensilex.getResourceURI('images/logo-phis-lg.png')
            "
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
          {{ t("LoginComponent.copyright.3", {
          version: opensilex.versionInfo.version
          }) }}
          <br />
          {{
          $t("LoginComponent.copyright.4", {
          version: opensilex.versionInfo.version
          })
          }}
        </p>
    </template>
  </opensilex-DefaultLoginComponent>
  
</template>

<script setup lang="ts">
import OpenSilexVuePlugin from "../../../../../opensilex-front/front/src/models/OpenSilexVuePlugin";
import {inject} from "vue";
import {useI18n} from "vue-i18n";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const {t} = useI18n();


  function getPHISModuleVersion(){
    for(let module_version_index in opensilex.versionInfo.modules_version){
      let module = opensilex.versionInfo.modules_version[module_version_index]

      if(module.name.includes("PhisWsModule")){
        return module.version;
      }
    }
    return 'Version undefined'
  }

</script>

<style scoped lang="scss">
</style>

<i18n>
</i18n>