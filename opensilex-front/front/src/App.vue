<template>
  <div>
    <opensilex-Overlay :show="isLoaderVisible" :noFade="false" zIndex="32000" :fullscreen="true">
      <div id="page-wrapper" class="wrapper customized" v-bind:class="{ embed: embed }">
         <!-- if route as credentials public -->
        <div v-if="this.$route.meta.public">
          <component v-bind:is="headerComponent"></component>
 
          <section id="content-wrapper" class="page-wrap"  v-bind:class="{ 'hidden-menu': !menuVisible }" >
            <div id="main-content">
              <main class="main-content">
                <router-view :key="$route.fullPath" />
              </main>

          <!--    <footer v-if="!embed">
                <component v-bind:is="footerComponent"></component>
              </footer> -->
            </div>
          </section>
        </div>  
        <div v-else>
          <component
            v-bind:is="headerComponent"
            v-if="user.isLoggedIn() && !disconnected && !embed"
          ></component>


          <header v-if="!embed" v-bind:class="{ 'logged-out': !user.isLoggedIn() || disconnected }">
            <component class="header-login" v-bind:is="loginComponent"></component>
          </header>

          <!-- notification message  -->
          <div
            v-if="displayNotificationMessage && notificationMessageDisplayed"
            :class="{
              'notificationWithMenu' : this.$store.state.menuVisible,
              [notificationColorClass]: true
            }"
            class="notificationMessageContainer notificationMessageDefaultColor"
          >
            <span class="notificationText" v-html="$t(this.notificationMessageDisplayed)"></span>
            <span class="notificationButtonContainer">
              <opensilex-Button
                label="component.common.close"
                icon="ik#ik-x"
                class="closeNotificationButton"
                @click="closeNotification()"
              ></opensilex-Button>
            </span>
          </div>

          <!-- URI search box -->
          <div
            v-show="uriSearchBoxVisible"
            class="uri-search-box">
            <opensilex-GlobalUriSearchBox
              @hideUriSearch="handleHideUriSearch"
            ></opensilex-GlobalUriSearchBox>

          </div>
          <section 
            id="content-wrapper" 
            class="page-wrap"  
            v-bind:class="{ 'hidden-menu': !menuVisible }" 
            v-if="user.isLoggedIn() && !disconnected"
          >
            <div>
              <component id="menu-container" v-if="!embed" v-bind:is="menuComponent"></component>
            </div>

            <div id="main-content">
              <main class="main-content">
                <router-view :key="$route.fullPath" />
              </main>

              <!--<footer v-if="!embed">
                <component v-bind:is="footerComponent"></component>
              </footer> -->
            </div>
          </section>
        </div>
      </div>
    </opensilex-Overlay>
  </div>
</template>

<script lang="ts">
import {Component as ComponentAnnotation, Prop} from "vue-property-decorator";
import Vue, { Component } from "vue";
import OpenSilexVuePlugin from "./models/OpenSilexVuePlugin";
import AsyncComputed from "vue-async-computed-decorator";
import {EventBus} from "../src/main";

@ComponentAnnotation
export default class App extends Vue {

  //#region: props
  @Prop() embed: boolean;

  @Prop() headerComponent!: string | Component;
  @Prop() loginComponent!: string | Component;
  @Prop() menuComponent!: string | Component;
  @Prop() footerComponent!: string | Component;

  //#endregion
  //#region: data

  $opensilex: OpenSilexVuePlugin;
  $i18n: any;
  $bvToast: any;
  $store: any;
  notificationMessage: {[ k : string]:string} = undefined;
  notificationMessageDisplayed: string = "";
  notificationEndDate: string = "";
  notificationColorTheme: string = "";
  displayNotificationMessage: boolean = false;
  private langUnwatcher;

  //#endregion
  //#region: hooks

  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      () => {
        this.notificationMessageDisplayed = this.notificationMessage[this.$i18n.locale]
      }
    );
    EventBus.$on('uriGlobalSearch', () =>this.handleUriGlobalSearchPressed());
  }

  beforeDestroy() {
      this.langUnwatcher();
  }


  created() {
    this.$opensilex.$bvToast = this.$bvToast;
    const currentDate = new Date();
    const formattedCurrentDate = currentDate.toISOString().slice(0, 10);
    this.notificationMessage = this.$opensilex.getConfig().notificationMessage;
    this.notificationEndDate = this.$opensilex.getConfig().notificationEndDate;
    this.notificationColorTheme = this.$opensilex.getConfig().notificationColorTheme;

    try {
      if(this.notificationEndDate){
        new Date(this.notificationEndDate)
      }
      if (!this.notificationEndDate || this.notificationEndDate > formattedCurrentDate) {
        this.displayNotificationMessage = true;
        this.notificationMessageDisplayed = this.notificationMessage[this.$i18n.locale]
      }
    } catch {
      this.$opensilex.showErrorToast(this.$i18n.t("component.header.bad-notification-end-date"));
    }
  }

  //#endregion
  //#region: AsyncComputed

  @AsyncComputed()
  notificationColorClass() {
    const theme = this.notificationColorTheme.toLowerCase();

    if (theme === 'information') {
      return 'notificationMessageInfoColor';
    } else if (theme === 'warning') {
      return 'notificationMessageWarningColor';
    } else {
      return 'notificationMessageDefaultColor';
    }
  }

  //#endregion
  //#region: EventHandlers

  private handleUriGlobalSearchPressed(){
    console.debug("handling");
      this.toggleUriSearchBox();
  }

  private handleHideUriSearch(){
    this.toggleUriSearchBox(false);
  }

  closeNotification(){
    this.displayNotificationMessage = false;
  }

  //#endregion
  //#region: private functions

  /**
   * Toggles or sets the value of this.uriSearchBoxVisible
   *
   * @param visible if not null then sets this.uriSearchBoxVisible to this value
   */
  private toggleUriSearchBox(visible?: boolean) {
    this.$store.state.uriSearchBoxVisible = visible !== undefined ? visible : !this.uriSearchBoxVisible;
  }

  //#endregion
  //#region: computed

  get lang() {
    return this.$store.state.lang;
  }

  get disconnected() {
    return this.$store.state.disconnected;
  }

  get user() {
    return this.$store.state.user;
  }

  get isLoaderVisible() {
    return this.$store.state.loaderVisible;
  }

  get menuVisible(): boolean {
    return this.$store.state.menuVisible;
  }

  //The following concerns the URI global search functionality
  get uriSearchBoxVisible(){
    return this.$store.state.uriSearchBoxVisible;
  }
  //#endregion
}
</script>

<style lang="scss">
@import "./styles/common.scss";

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

.uri-search-box {
  position: fixed;
  max-width: 500px;
  top: 70px;
  right: 8%;
  padding: 20px;
  background-color: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  border-radius: 10px;
  z-index: 1030;
}

.uriLinkGlobalUriSearchRes a {
    color: #007bff;
}

.notificationMessageContainer{
  display: flex;
  position: relative;
  z-index: 1029; // header has an index of 1030, must be lower to allow the display of dropdown buttons above
  width: 100%;
  min-height: 35px;
  top: 65px;
  padding: 0 10px 0 10px;
  justify-content: center;
  align-items: center;
  font-family: sans-serif;
  font-size: small;
  font-style: oblique;
  font-weight: bold;
  color: darkslategray;
}

.notificationMessageDefaultColor {
  background: linear-gradient(45deg, #CFD8D5, #B1D8CB);
}
.notificationMessageInfoColor {
  background: linear-gradient(45deg, #c8e7eb, #91c8db);
}
.notificationMessageWarningColor {
    background: linear-gradient(45deg, #ebc8c8, #db9191);
}

.notificationWithMenu {
  left: 241px;
  max-width: calc(100% - 241px);
}

.notificationButtonContainer {
  position: inherit;
  right: 0;
}

.closeNotificationButton{
  border: none;
  padding: 1px 8px;
  margin: 4px;
  font-size: 1.5em !important;
  color:rgba(101, 101, 101, 0.5);
  font-weight: bolder;
  cursor: pointer;
  background: none;
}

.closeNotificationButton:hover{
  color : #00A28C;
  background: none;
}

.notificationText {
  padding-right: 25px;
  max-width: calc(100% - 50px);
}
</style>
