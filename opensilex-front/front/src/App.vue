<template>
  <div id="page-wrapper" class="wrapper customized" v-bind:class="{ embed: embed }">
    <component
      class="header-logo"
      v-bind:is="headerComponent"
      v-if="user.isLoggedIn() && !disconnected &&!embed"
    ></component>

    <header v-if="!embed" v-bind:class="{ 'logged-out': !user.isLoggedIn() || disconnected }">
      <component class="header-login" v-bind:is="loginComponent"></component>
    </header>

    <section id="content-wrapper" class="page-wrap" v-if="user.isLoggedIn() && !disconnected">
        <component v-if="!embed" v-bind:is="menuComponent"></component>

        <main class="main-content">
          <router-view />
        </main>

      <footer v-if="!embed">
        <component v-bind:is="footerComponent"></component>
      </footer>
    </section>

    <div id="loader" v-bind:class="{'visible':isLoaderVisible}">
      <div class="lds-ripple">
        <div></div>
        <div></div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component as ComponentAnnotation, Prop } from "vue-property-decorator";
import Vue from "vue";
import { ModuleComponentDefinition } from "./models/ModuleComponentDefinition";
import { VueConstructor, Component } from "vue";
import OpenSilexVuePlugin from "./models/OpenSilexVuePlugin";
import { FrontConfigDTO } from "./lib";

@ComponentAnnotation
export default class App extends Vue {
  @Prop() embed: boolean;

  @Prop() headerComponent!: string | Component;
  @Prop() loginComponent!: string | Component;
  @Prop() menuComponent!: string | Component;
  @Prop() footerComponent!: string | Component;

  $opensilex: OpenSilexVuePlugin;

  get disconnected() {
    return this.$store.state.disconnected;
  }

  get user() {
    return this.$store.state.user;
  }

  get isLoaderVisible() {
    return this.$store.state.loaderVisible;
  }
}
</script>

<style lang="scss">

@import './styles/common.scss';
@import "../node_modules/icon-kit/dist/css/iconkit.min.css";
@import '../node_modules/bootstrap/scss/bootstrap';
@import '../node_modules/bootstrap-vue/src/index.scss';
@import '../node_modules/vue-multiselect/dist/vue-multiselect.min.css';
@import '../node_modules/vue-airbnb-style-datepicker/dist/vue-airbnb-style-datepicker.min.css';

#loader {
  display: none;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100%;
  width: 100%;
  z-index: 32000;
  background-color: rgba(255, 255, 255, 0.7);
  text-align: center;
}

#loader .lds-ripple {
  display: inline-block;
  position: relative;
  width: 80px;
  height: 80px;
  top: 50%;
  transform: translateY(-50%);
  margin: auto;
}
#loader .lds-ripple div {
  position: absolute;
  border: 4px solid #000;
  opacity: 1;
  border-radius: 50%;
  animation: lds-ripple 1s cubic-bezier(0, 0.2, 0.8, 1) infinite;
}
#loader .lds-ripple div:nth-child(2) {
  animation-delay: -0.5s;
}
@keyframes lds-ripple {
  0% {
    top: 36px;
    left: 36px;
    width: 0;
    height: 0;
    opacity: 1;
  }
  100% {
    top: 0px;
    left: 0px;
    width: 72px;
    height: 72px;
    opacity: 0;
  }
}

#loader.visible {
  display: block;
}

header {
  display: flex;
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
  text-align: right;
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

.header-top.logged-out {
  box-shadow: none;
}

.wrapper.embed .page-wrap .main-content {
    margin-top: 0px;
    margin-left: 0px;
    padding: 15px;
}

</style>
