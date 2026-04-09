<template>
  <opensilex-DefaultHeaderComponent> 
      <template v-slot:headerLogo>   
        <img
            v-bind:src="$opensilex.getResourceURI('images/siduri_couleur_dashboard.png')"
            class="header-brand-img"
            alt="lavalite" 
          />
        </template>
  </opensilex-DefaultHeaderComponent>
</template>

<script lang="ts">
import { Component,  } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class SiduriHeaderComponent extends Vue {

}

</script>

<style lang="scss">

// .wrapper .header-brand .logo-img{
//  width: 150px !important;
// }


.app-logo {
  background-color: #F4F2EE !important;
}

.header-brand .text {
    color: #00A3A6 !important;
}

.hamburger-inner, .hamburger-inner:before, .hamburger-inner:after{
  background-color: #0070B9 !important;
}

.helpButton {
    color: #0070B9 !important;
    border-color: #0070B9 !important;
}
.helpButton:hover{
    border-color: #0070B9 !important;
    color: #0070B9 !important;
}

</style>
