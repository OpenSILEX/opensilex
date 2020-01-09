<template>
  <div class="app-sidebar" v-if="menuVisible" >
    <div class="sidebar-header">
      <router-link class="header-brand" to="/">
        <div class="logo-img">
          <img v-bind:src="$opensilex.getResourceURI('images/logo-phis.svg')" class="header-brand-img" alt="lavalite" />
        </div>
        <span class="text">
          PHIS
          <span class="instance-name">Diaphen</span>
        </span>
      </router-link>
      <button id="sidebarClose" class="nav-close">
        <i class="ik ik-x"></i>
      </button>
    </div>
    <div class="sidebar-content">
      <div class="nav-container"></div>
      <nav id="main-menu-navigation" class="navigation-main">
        <div li v-for="item in menu" v-bind:key="item.id" class="nav-item">
          <span v-if="!item.route">
            <i class="ik" v-bind:class="item.icon"></i>
            <span>{{ $t(item.label) }}</span>
          </span>
          <router-link v-else :to="item.route.path">
            <i class="ik" v-bind:class="item.icon"></i>
            <span>{{ $t(item.label) }}</span>
          </router-link>
          <div v-for="itemChild in item.children" v-bind:key="itemChild.id" class="submenu-content">
            <span v-if="!itemChild.route">
              <span>{{ $t(itemChild.label) }}</span>
            </span>
            <router-link v-else :to="itemChild.route.path">
              <span>{{ $t(itemChild.label) }}</span>
            </router-link>
          </div>
        </div>
      </nav>
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import { MenuItemDTO } from "../../lib";

@Component
export default class DefaultMenuComponent extends Vue {
  get menu(): Array<MenuItemDTO> {
    return this.$store.state.menu;
  }

  get user() {
    return this.$store.state.user;
  }

  get menuVisible() {
    return this.$store.state.menuVisible;
  }
}
</script>

<style scoped lang="scss">
</style>
