<template>
  <opensilex-DefaultHeaderComponent> 
      <template v-slot:headerLogo>   
        <img
            v-bind:src="$opensilex.getResourceURI('images/siduri_couleur_dashboard.png')"
            class="w-100" alt="logo-svg"  
          />
        </template>

        <!-- user name -->
        <template v-slot:description>
          <div class="user-name">{{ userNameHeader }}</div>
        </template>
  </opensilex-DefaultHeaderComponent>
</template>

<script lang="ts">
import { Component,  } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../../../opensilex-front/front/src/models/OpenSilexVuePlugin";

@Component
export default class SiduriHeaderComponent extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;

  /**
   * Return either the email or the first letter of first name and the last nale for the current user
   */
   get userNameHeader() {
    console.log("test");
    let userInfo = this.$store.state.user;
    console.log(userInfo);
    if (!userInfo.getLastName()){
      return userInfo.getEmail();
    } else {
      return userInfo.getFirstName().replaceAll(' ', '').substring(0, 1).toUpperCase() + '. ' + userInfo.getLastName();
    }
  }
}

</script>

<style lang="scss">

// .wrapper .header-brand .logo-img{
//  width: 150px !important;
// }


.user-name {
  min-width: 150px;
  font-weight: bold;
  padding: 0 20px;
  color: #525252 !important; // tailwind CSS neutrol-500
}

.app-logo {
  background-color: #F4F2EE !important;
}

.header-brand .text {
    color: #00A3A6 !important;
    text-transform: uppercase;
}

.title-icon {
  background-color: #00A3A6 !important; 
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

@media (min-width: 676px) and (max-width: 799px) {
  .user-name{
    display: none;
  }
}
@media (min-width: 200px) and (max-width: 675px) {
  .user-name{
    display: none;
  }
}
</style>
