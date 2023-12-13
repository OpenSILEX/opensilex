<template>
  <opensilex-DefaultLoginComponent>
    <template v-slot:loginMedia>
        <!-- <img
            v-bind:src="$opensilex.getResourceURI('images/opensilex-login-bg.jpg')"
        /> -->
      <img class="bottom"
           v-bind:src="$opensilex.getResourceURI('images/opensilex-login-bg.png')"
      />


      <img class="top"
           v-bind:src="$opensilex.getResourceURI('images/arenaSetup_LartigueEtAl2021.jpeg')"
      />

      <img class="top"
           v-bind:src="$opensilex.getResourceURI('images/armoiseMultiObjectTrakcingStillFrame.jpeg')"
      />

      <img class="top"
           v-bind:src="$opensilex.getResourceURI('images/ExperimentalSet-up2StudySpatialPpropagation.jpeg')"
      />

      <img class="top"
           v-bind:src="$opensilex.getResourceURI('images/Trichogramma_dendrolimi.jpg')"
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

    <slot name="loginFooter">
      <p>
        {{ $t("LoginComponent.copyright.3", {
        version: this.versionInfo.version
      }) }}
        <br />
        {{
          $t("component.login.copyright.4", {
            version: this.versionInfo.version
          })
        }}
      </p>
    </slot>
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