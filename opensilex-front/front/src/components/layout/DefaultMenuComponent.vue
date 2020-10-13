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
      <div class="nav-container">
        <nav id="main-menu-navigation" class="navigation-main">
          <div li v-for="item in menu" v-bind:key="item.id" class="nav-item" v-bind:class="{ 'has-sub': item.hasChildren(),  'open': item.showChildren, 'active': isActive(item)}">
            <a v-if="item.hasChildren()" href="#" v-on:click="toogle(item, $event)">
              <i class="ik" v-bind:class="getIcon(item)"></i>
              <span>{{ $t(item.label) }}</span>
            </a>
            <router-link v-else :to="item.route.path" :active="isActive">
              <i class="ik" v-bind:class="getIcon(item)"></i>
              <span>{{ $t(item.label) }}</span>
            </router-link>
            <div class="submenu-content" v-bind:class="{ 'open': item.showChildren}">
              <router-link v-for="itemChild in item.children" v-bind:key="itemChild.id" v-bind:class="{ 'is-shown': item.showChildren, 'active': isActive(itemChild) }" class="menu-item" :to="itemChild.route.path">
                {{ $t(itemChild.label) }}
              </router-link>
            </div>
          </div>
        </nav>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import { Menu } from "../../models/Menu";
import { UserGetDTO } from 'opensilex-security/index';
import VueI18n from 'vue-i18n'
import { MenuItemDTO } from '../../lib';


@Component
export default class DefaultMenuComponent extends Vue {
  $route: any;
  $store: any;

  get menu(): Array<Menu> {
    return this.$store.state.menu;
  }

  get user() {
    return this.$store.state.user;
  }

  get menuVisible(): boolean {
    return this.$store.state.menuVisible;
  }

  toggleMenu(): void {
    this.$store.commit("toggleMenu");
  }

  toogle(item: Menu, event: MouseEvent): void {
    if(item.hasChildren()) {
      console.info("toogle menu, old value = " + item.showChildren)
      item.showChildren = !item.showChildren;
    }
  }

  getIcon(item: Menu): string {
    var code = "icon." + item.label;
    var result = this.$t(code);
    if(code != result) {
      return result.toString();
    }
    return "ik-folder";
  }

  isActive(item: Menu): boolean {
    return item.route && this.$route.path.indexOf(item.route.path) === 0;
  }
}
</script>

<style scoped lang="scss">
</style>
