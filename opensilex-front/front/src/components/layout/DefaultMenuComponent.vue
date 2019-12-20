<template>
  <div class="app-sidebar">
    <div class="sidebar-header">
        <a class="header-brand" href="index.html">
            <div class="logo-img">
                <img src="logo-phis.svg" class="header-brand-img" alt="lavalite"> 
            </div>
            <span class="text">PHIS <span class="instance-name">Diaphen</span></span>
        </a>
        <button id="sidebarClose" class="nav-close"><i class="ik ik-x"></i></button>
    </div>
      <div class="sidebar-content">
        <div class="nav-container"></div>
        <nav id="main-menu-navigation" class="navigation-main">
          <div li v-for="item in menu" v-bind:key="item.id" class="nav-item">
            <a href="#"><i class="ik" v-bind:class="item.icon"></i><span>{{ $t(item.label) }}</span></a>
            <div v-for="itemChild in item.children" v-bind:key="itemChild.id" class="submenu-content">
              <a href="#" class="menu-item">{{ $t(itemChild.label) }}</a>
            </div>
          </div>
        </nav>
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from 'vue-property-decorator';
import Vue from 'vue';
import { MenuItemDTO } from "../../lib";

@Component
export default class DefaultMenuComponent extends Vue {
  get menu(): Array<MenuItemDTO> {
    return this.$store.state.menu;
  }

  get user() {
    return this.$store.state.user;
  }
}
</script>

<style scoped lang="scss">
</style>
