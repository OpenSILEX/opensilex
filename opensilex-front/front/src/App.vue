<template>
  <div id="page-wrapper">
    <header>
      <div id="header-content">
        <component class="header-logo" v-bind:is="headerComponent"></component>
        <component class="header-login" v-bind:is="loginComponent"></component>
      </div>
    </header>
    <section id="content-wrapper">
      <component v-bind:is="menuComponent"></component>
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
import { ModuleComponentDefinition } from "./plugin/ModuleComponentDefinition";
import { VueConstructor, Component } from "vue";

@ComponentAnnotation
export default class App extends VueBaseClass {
  @Prop() headerComponentDef!: ModuleComponentDefinition;
  @Prop() loginComponentDef!: ModuleComponentDefinition;
  @Prop() menuComponentDef!: ModuleComponentDefinition;
  @Prop() footerComponentDef!: ModuleComponentDefinition;

  headerComponent!: string | Component;
  loginComponent!: string | Component;
  menuComponent!: string | Component;
  footerComponent!: string | Component;

  get user() {
    return this.$store.state.user;
  }

  get menuVisible() {
    return this.$store.state.menuVisible;
  }

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

#page-wrapper {
  min-height: 100vh;
  flex-direction: column;
  display: -webkit-flex;
  display: flex;
  margin: auto;
}

header {
  padding: 5px 30px;
  min-height: 50px;
  color: white;
  display: flex;
  background-color: getVar(--highlightBackgroundColorLight);
  color: getVar(--defaultColorDark);
}

main {
    background-color: getVar(--defaultColorLight);
    color: getVar(--defaultColorDark);
}

#header-content {
  max-width: 1600px;
  margin: auto;
  display: flex;
  width: 100%;
}

#header-content .header-logo {
  width: 70%;
}

#header-content .header-login {
  width: 30%;
}

section#content-wrapper {
  display: -webkit-flex;
  display: flex;
  margin: 0 auto;
  height: 100%;
  flex-grow: 1;
  width: 100%;
}

main {
  padding: 15px;
  width: 100%;
}

@media (max-width: 600px) {
  section#content-wrapper {
    -webkit-flex-direction: column;
    flex-direction: column;
  }
}
</style>
