<template>
  <opensilex-DefaultLoginComponent>
    <template v-slot:loginMedia>
      <!-- display from the last to the first one, first one must have class "bottom" -->
      <img class="bottom"
        v-bind:src="$opensilex.getResourceURI('images/login_page/MAIAGE_data_center.jpeg')"
      />

      <img class="top"
        v-bind:src="$opensilex.getResourceURI('images/login_page/fromage.jpeg')"
      />

      <img class="top"
        v-bind:src="$opensilex.getResourceURI('images/login_page/MGP_chambre.jpg')"
      />

      <img class="top"
        v-bind:src="$opensilex.getResourceURI('images/login_page/SayFood_HPLC.jpg')"
      />

      <img class="top"
        v-bind:src="$opensilex.getResourceURI('images/login_page/pills.jpeg')"
      />

      <img class="top"
        v-bind:src="$opensilex.getResourceURI('images/login_page/STLO_Omnilog.jpg')"
      />

      <img class="top"
        v-bind:src="$opensilex.getResourceURI('images/login_page/fermented_food.jpg')"
      />

      <img class="top"
        v-bind:src="$opensilex.getResourceURI('images/login_page/fdf_pfd_ci2f.png')"
      />
    </template>

    <template v-slot:loginLogo>
      <b-img :src="$opensilex.getResourceURI('images/siduri_couleur_name.png')" fluid alt="logo siduri" width="300px"></b-img>
    </template>

    <template v-slot:loginFooter>
      <div class="trademark">
        <p>
          {{ $t("siduri.component.login.copyright.1") }}
          <br />
          {{ $t("siduri.component.login.copyright.2") }}
          <br />
        </p>
      </div>
    </template>

  </opensilex-DefaultLoginComponent>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../../../opensilex-front/front/src/models/OpenSilexVuePlugin";

@Component
export default class SiduriLoginComponent extends Vue {
  $opensilex: OpenSilexVuePlugin;
}
</script>


