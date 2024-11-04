<template>
  <opensilex-DefaultLoginComponent>
    <template v-slot:loginMedia>
       <img

           v-bind:src="$opensilex.getResourceURI('images/T.brassicae_E.kuehniella.jpg')"
      />


    </template>

    <template v-slot:loginLogo>
        <img
            v-bind:src="
            $opensilex.getResourceURI('images/insectPlantBiosLogo.png')
            "
            alt
        />
    </template>

    <template v-slot:loginFooter>
      <p>
        OpenSILEX insect
        <br />
        {{ $t("LoginComponent.copyright.2", {
        version: getInsectModuleVersion()
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
export default class InsectLoginComponent extends Vue {

  versionInfo: VersionInfoDTO = {};
  $opensilex: OpenSilexVuePlugin;

  created() {
    this.versionInfo = this.$opensilex.versionInfo;
  }

  getInsectModuleVersion(){
    for(let module_version_index in this.versionInfo.modules_version){
      let module = this.versionInfo.modules_version[module_version_index]

      if(module.name.includes("InsectModule")){
        return module.version;
      }
    }
    return 'Version undefined'
  }

}
</script>

<style scoped lang="scss">
#imagesCarrousel img {

  position:absolute;
  width: 100%;
  height: 100%;
  -webkit-background-size: cover;
  background-size: cover;

  background-repeat: no-repeat;
  left:0;
  -webkit-transition: opacity 2s ease-in-out;
  -moz-transition: opacity 2s ease-in-out;
  -o-transition: opacity 2s ease-in-out;
  transition: opacity 2s ease-in-out;
}

#imagesCarrousel img.top {
  animation-name: LoginImageAnimation;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
  animation-duration: 17s;
  animation-direction: normal;
}
</style>

<i18n>
</i18n>