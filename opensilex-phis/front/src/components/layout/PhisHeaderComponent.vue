<template>
  <opensilex-DefaultHeaderComponent>
    <template v-slot:headerLogo>
        <img
            v-bind:src="opensilex.getResourceURI('images/logo-phis.svg')"
            class="header-brand-img"
            alt="lavalite"
        />
    </template>
  </opensilex-DefaultHeaderComponent>
  
</template>

<script setup lang="ts">
import { inject } from "vue";
import OpenSilexVuePlugin from "../../../../../opensilex-front/front/src/models/OpenSilexVuePlugin";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");


</script>