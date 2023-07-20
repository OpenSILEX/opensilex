<template>
  <div>
    <!-- Hamburger -->
    <div class="hamburger-container">
      <button
        class="hamburger hamburger--collapse"
        v-bind:class="{ 'is-active': menuVisible }"
        type="button"
        @click="toggleMenu()"
      >
        <span class="hamburger-box">
          <span class="hamburger-inner"></span>
        </span>
      </button>
    </div>
      <!-- Sections menu -->
    <div class="app-sidebar">
      <div class="sidebar-content">
        <div class="nav-container">
          <nav id="main-menu-navigation" class="navigation-main">
            <div
              li
              v-for="item in menu"
              v-bind:key="item.id"
              class="nav-item"
              v-bind:class="{
                'has-sub': item.hasChildren(),
                open: item.showChildren,
                active: isActive(item),
              }"
            >
              <a
                v-if="item.hasChildren()"
                href="#"
                v-on:click="toogle(item, $event);"
              >
                <i class="ik" v-bind:class="getIcon(item)"></i>
                <span>{{ $t(item.label) }}</span>
              </a>
              <router-link v-else :to="item.route.path" :active="isActive">
                <i class="ik" v-bind:class="getIcon(item)"></i>
                <span>{{ $t(item.label) }}</span>
              </router-link>
              <div
                class="submenu-content"
                v-bind:class="{ open: item.showChildren }"
              > 
                <span @click="toggleMenuOnSelect()">
                <router-link
                  v-for="itemChild in item.children"
                  v-bind:key="itemChild.id"
                  v-bind:class="{
                    'is-shown': item.showChildren,
                    active: isActive(itemChild),
                  }"
                  class="menu-item"
                  :to="itemChild.route.path"
                >
                 {{ $t(itemChild.label) }}
                </router-link></span>
              </div>
            </div>
            <div li class="nav-item">
              <a
                v-if="versionInfo.api_docs.url != undefined"
                :href="versionInfo.api_docs.url"
                target="_blank"
                class="router-link-exact-active router-link-active"
              >
                <b-img
                  fluid
                  width="25"
                  src="https://cdn.svgporn.com/logos/swagger.svg"
                />
                &nbsp;&nbsp;&nbsp;
                <span class="ml-1">{{ $t("component.menu.web-api") }}</span>
              </a>
            </div>
          </nav>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import { Menu } from "../../models/Menu";

// @ts-ignore
import { versionInfoDTO } from "opensilex-core/index";
@Component
export default class DefaultMenuComponent extends Vue {
  $route: any;
  $store: any;
  $opensilex: any;
  $t : any;

  versionInfo: versionInfoDTO;

  get menu(): Array<Menu> {
    return this.$store.state.menu;
  }

  get user() {
    return this.$store.state.user;
  }

  get menuVisible(): boolean {
    return this.$store.state.menuVisible;
  }

  created() {
      this.versionInfo = this.$opensilex.versionInfo;
  }

  toggleMenu(): void {
    this.$store.commit("toggleMenu");
  }

  /*hide menu on category selected*/
  toggleMenuOnSelect(): void {
    if (document.body.clientWidth < 1040) {
      this.$store.commit("toggleMenuOnSelect");
    }
  }

  toogle(item: Menu, event: MouseEvent): void {
    if (item.hasChildren()) {
      console.info("toogle menu, old value = " + item.showChildren);
      item.showChildren = !item.showChildren;
    }
  }

  getIcon(item: Menu): string {
    var code = "icon." + item.label;
    var result = this.$t(code);
    if (code != result) {
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
.hamburger-container {
  position: fixed;
  z-index: 1030;
  -webkit-user-select: none;
  user-select: none;
  height: 65px;
  width: 60px;
  left: 0px;
  background-color: #00a38d;
}

.hamburger {
  position: relative;
  height: 55px;
  top: 1px;
}

.hamburger {
  outline: none;
}

.hamburger .hamburger-inner,
.hamburger .hamburger-inner::after,
.hamburger .hamburger-inner::before,
.hamburger.is-active .hamburger-inner,
.hamburger.is-active .hamburger-inner::after,
.hamburger.is-active .hamburger-inner::before {
  background-color: #fff;
}

.hamburger:hover .hamburger-inner,
.hamburger:hover .hamburger-inner::after,
.hamburger:hover .hamburger-inner::before,
.hamburger.is-active:hover .hamburger-inner,
.hamburger.is-active:hover .hamburger-inner::after,
.hamburger.is-active:hover .hamburger-inner::before {
  background-color: rgb(221, 221, 221);
}

.hamburger.is-active:hover,
.hamburger:hover {
  opacity: 1;
}

.hamburger-box {
  width: 35px;
}

.hamburger-inner,
.hamburger-inner:after,
.hamburger-inner:before {
  width: 41px;
}

@media (min-width: 250px) and (max-width: 1150px) {

.hamburger-inner,
.hamburger-inner:after,
.hamburger-inner:before { 
    width: 25px;

  }
}
</style>
