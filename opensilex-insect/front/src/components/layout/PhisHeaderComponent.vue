<template>
  <opensilex-DefaultHeaderComponent>
    <template v-slot:headerLogo>
        <img
            v-bind:src="$opensilex.getResourceURI('images/logo-phis.svg')"
            class="header-brand-img"
            alt="lavalite"
        />
    </template>
  </opensilex-DefaultHeaderComponent>
  
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class PhisHeaderComponent extends Vue {

}
</script>

<style scoped lang="scss">
</style>

<i18n>    
</i18n>