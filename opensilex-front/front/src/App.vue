<template>
  <div id="page-wrapper">
    <header>
      <div id="header-content">
        <component class="header-logo" v-bind:is="headerComponent"></component>
        <component class="header-login" v-bind:is="loginComponent"></component>
      </div>
    </header>
    <section id="content-wrapper">
      <div class="hamburger" v-if="isLoggedIn">
        <input type="checkbox" v-model="menuVisible" />
        <span class="hamburger1"></span>
        <span class="hamburger2"></span>
        <span class="hamburger3"></span>
      </div>

      <nav v-if="isLoggedIn" v-bind:class="{'hide-responsive':!menuVisible}">
        <component v-bind:is="menuComponent"></component>
      </nav>
      <main>
        <router-view />
      </main>
    </section>
    <footer>
      <component v-bind:is="footerComponent"></component>
    </footer>
  </div>
</template>

<script lang="ts">
import {
  Component as ComponentAnnotation,
  Vue as VueBaseClass,
  Prop
} from "vue-property-decorator";
import { ModuleComponentDefinition } from "./modules/ModuleComponentDefinition";
import { VueConstructor, Component } from "vue";

declare var Vue: VueConstructor;

@ComponentAnnotation
export default class App extends VueBaseClass {
  isLoggedIn: boolean = true;
  menuVisible: boolean = false;

  @Prop() headerComponentDef!: ModuleComponentDefinition;
  @Prop() loginComponentDef!: ModuleComponentDefinition;
  @Prop() menuComponentDef!: ModuleComponentDefinition;
  @Prop() footerComponentDef!: ModuleComponentDefinition;

  headerComponent!: string | Component;
  loginComponent!: string | Component;
  menuComponent!: string | Component;
  footerComponent!: string | Component;

  beforeMount() {
    this.headerComponent = this.headerComponentDef.getName();
    this.loginComponent = this.loginComponentDef.getName();
    this.menuComponent = this.menuComponentDef.getName();
    this.footerComponent = this.footerComponentDef.getName();
  }
}
</script>

<style lang="scss">
@import "../styles/styles";

/**
 * Responsive hamburger hide/show menu
 */
.hamburger {
  display: none;
  position: absolute;
  right: 75px;
  top: 31px;
  z-index: 1;
  -webkit-user-select: none;
  user-select: none;
}

@media (max-width: 600px) {
  .hamburger {
    display: block;
  }

  nav.hide-responsive {
    display: none;
  }
}

.hamburger input {
  display: block;
  width: 40px;
  height: 32px;
  position: absolute;
  top: -16px;
  left: -9px;
  cursor: pointer;
  opacity: 0;
  z-index: 2;

  -webkit-touch-callout: none;
}

.hamburger span {
  display: block;
  width: 33px;
  height: 4px;
  margin-bottom: 5px;
  position: relative;

  background: getVar(--linkColor);
  border-radius: 3px;

  z-index: 1;

  transform-origin: 4px 0px;

  transition: transform 0.5s cubic-bezier(0.77, 0.2, 0.05, 1),
    opacity 0.55s ease;

  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
  -webkit-background-clip: content-box;
  background-clip: content-box;
}

.hamburger span:first-child {
  transform-origin: 0% 0%;
}

.hamburger span:nth-last-child(2) {
  transform-origin: 0% 100%;
}

.hamburger1,
.hamburger2 {
  top: -6px;
}

.hamburger3 {
  top: -33px;
}

.hamburger input:checked ~ span {
  opacity: 1;
  transform: rotate(45deg) translate(-2px, -1px);
  background: getVar(--linkColor);
}

.hamburger input:checked ~ span:nth-last-child(3) {
  opacity: 0;
  transform: rotate(0deg) scale(0.2, 0.2);
}

.hamburger input:checked ~ span:nth-last-child(2) {
  transform: rotate(-45deg) translate(0, -1px);
}

.hamburger input:hover ~ span {
  background: getVar(--linkHighlightColor);
}
</style>
