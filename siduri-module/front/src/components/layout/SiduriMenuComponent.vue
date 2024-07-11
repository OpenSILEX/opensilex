<template>
  <div class="app-sidebar">
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

    <div class="sidebar-content" v-bind:class="{ 'invisible': !menuVisible }">
      <nav id="main-menu-navigation" class="navigation-main">
        <div li v-for="item in menu" v-bind:key="item.id" class="nav-item">
          <span v-if="!item.route">
            <i v-if="item.icon" class="ik" v-bind:class="item.icon"></i>
            <span></span>
          </span>
          <router-link v-else :to="item.route.path">
            <i v-if="item.icon" class="ik" v-bind:class="item.icon"></i>
            <span></span>
          </router-link>
          <div v-for="itemChild in item.children" v-bind:key="itemChild.id" class="submenu-content">
            <span v-if="!itemChild.route">
              <span></span>
            </span>
            <router-link v-else :to="itemChild.route.path">
              <span></span>
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
import DefaultMenuComponent from "../../../../../opensilex-front/front/src/components/layout/DefaultMenuComponent.vue";

@Component
export default class SiduriMenuComponent extends DefaultMenuComponent {

  created() {
    window.addEventListener("resize", this.handleResize);
    this.handleResize();
  }

  beforeDestroy() {
    window.removeEventListener("resize", this.handleResize);
  }

  handleResize() {
    const minSize = 480;
    if (document.body.clientWidth <= minSize && (this.width == null || this.width > minSize)) {
      this.width = document.body.clientWidth;
      this.$store.commit("hideMenu");
    } else if (document.body.clientWidth > minSize && (this.width == null || this.width <= minSize)) {
      this.width = document.body.clientWidth;
      this.$store.commit("showMenu");
    }
  }
}
</script>

<style scoped lang="scss">

</style>
