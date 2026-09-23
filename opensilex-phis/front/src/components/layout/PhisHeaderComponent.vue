<template>
  <DefaultHeaderComponent>
    <template v-slot:headerLogo>
        <img
            v-bind:src="opensilex.getResourceURI('images/logo-phis.svg')"
            class="header-brand-img"
            alt="lavalite"
        />
    </template>
  </DefaultHeaderComponent>
  
</template>

<script setup lang="ts">
import { inject } from "vue";
import OpenSilexVuePlugin from "opensilex-front/models/OpenSilexVuePlugin";
import DefaultHeaderComponent from "opensilex-front/components/layout/DefaultHeaderComponent.vue";


const opensilex = inject<OpenSilexVuePlugin>("$opensilex");


</script>