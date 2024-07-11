<template>
  <div id="header">
    <div class="app-logo">
      <router-link to="/">
        <img v-bind:src="$opensilex.getResourceURI('images/logo-siduri.png')" />
        <div class="app-title">Siduri</div>
      </router-link>
    </div>

    <div class="app-actions">
      <div class="language-selector">
        <b-dropdown variant="link" size="lg" right>
          <template v-slot:button-content>
            <div class="language-selector-button">
              <span class="action-label"></span>&nbsp;
              <i class="icon ik ik-globe"></i>
            </div>
          </template>

          <b-dropdown-item
            v-for="item in languages"
            :key="`language-${item}`"
            href="#"
            @click.prevent="setLanguage(item)"
          ></b-dropdown-item>
          
        </b-dropdown>
      </div>
      <b-button class="app-header-btn" variant="link" size="lg" @click.prevent="logout()">
        <span class="action-label"></span>&nbsp;
        <font-awesome-icon icon="power-off" />
      </b-button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import DefaultHeaderComponent from "../../../../../opensilex-front/front/src/components/layout/DefaultHeaderComponent.vue";

@Component
export default class SiduriHeaderComponent extends DefaultHeaderComponent {}
</script>

<style scoped lang="scss">

</style>
