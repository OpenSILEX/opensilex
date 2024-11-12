<template>
<div>
  <div class="header-top" header-theme="light">
    <router-link :to="{path: '/'}" :title="$t('component.menu.backToDashboard')">
      <div class="app-logo">
        <div class="header-brand">
          <div class="logo-img">
            <slot name="headerLogo">
              <img
                v-bind:src="$opensilex.getResourceURI('images/logo-opensilex_miniature.png')"
                class="header-brand-img"
                alt="lavalite"
              />
            </slot>
          </div>
          <span class="text">
            {{ this.applicationName }}
          </span>
        </div>
      </div>
    </router-link>

    <div class="container-fluid boxed-layout">
      <h5 v-if="iconvalue" class="header-title">
        <opensilex-Icon :icon.sync="iconvalue" class="title-icon"/>
        <slot name="title">&nbsp;{{ $t(titlevalue) }}</slot>
      </h5>
      <span v-else> <br> </span>
        <span class="title-description">
          <slot name="description" >{{ $t(descriptionevalue) }}</slot>
        </span>

      <div class="d-flex justify-content-end">
        <div class="top-menu d-flex align-items-center">
          <!--
            Label to indicate the deployment version if needed (develop or release)
            For development purposes only
           -->
          <div
              v-if="versionLabel"
              class="version-label-box"
              v-bind:class="[versionLabelClass]"
          >
            {{ versionLabel }}
          </div>

      <!-- Burger menu start -->
      <button
        class="hamburger headerburger"
        type="button"
        v-on:click="HeaderBurgerToggle = !HeaderBurgerToggle"
      >
        <span class="hamburger-box">
          <span class="hamburger-inner"></span>
        </span>
      </button>

    <Transition>
      <div v-show="HeaderBurgerToggle"
        class="burgerMenuContainer"><br>
        <div>
          <opensilex-HelpButton
            class="burgerMenuHelp"
            @click="$opensilex.getGuideFile()"
            label="component.header.user-guide"
          ></opensilex-HelpButton>
        </div>
        <!--Uri global search-->
        <opensilex-Button
          @click="$emit('uriGlobalSearch')"
          :label="$t('component.header.uriSearchHoverMessage')"
          class="burgerMenu-searchIcon ik ik-search"
          :class="{ 'selected-searchicon': searchBoxIsActive }"
          icon="ik-search"
        ></opensilex-Button>
        <div>
          <div>
            <b-dropdown
              class="langDropdown"
              :title="`language - ${this.language}`"
              variant="link"
              right
            >
              <template v-slot:button-content>
                <i class="icon ik ik-globe"></i>
              </template>

              <b-dropdown-item
                v-for="item in languages"
                :key="`language-${item}`"
                href="#"
                @click.prevent="setLanguage(item)"
                >{{ $t("component.header.language." + item) }}
              </b-dropdown-item>
            </b-dropdown>
          </div>
            <b-dropdown
              v-if="user.isLoggedIn()"
              id="userDropdown"
              :title="user.getEmail()"
              variant="link"
              right
            >
              <template v-slot:button-content class="userIcon">
                <i class="icon ik ik-user"></i>
              </template>
              <b-dropdown-item href="#" @click.prevent="logout">
                <i class="ik ik-log-out dropdown-icon"></i>
                {{ $t("component.header.account.logout") }}
              </b-dropdown-item>
            </b-dropdown>
        </div>
      </div>
    </Transition>
    <!-- Burger menu end -->

          <!--help button-->
          <opensilex-HelpButton
            class="topbarBtnHelp"
            @click="$opensilex.getGuideFile()"
            label="component.header.user-guide"
          ></opensilex-HelpButton>

          <div class="headerMenuIcons">

            <!--Uri global search-->
            <b-button
              class="searchicon"
              :class="{ 'selected-searchicon': searchBoxIsActive }"
              :title="$t('component.header.uriSearchHoverMessage')"
              @click="$emit('uriGlobalSearch')"
            >
              URI
              <i class="icon ik ik-search"></i>
            </b-button>
            <!--language button -->
            <b-dropdown
              class="langDropdown" 
              :title="`language - ${this.language}`"
              variant="link"
              right
            >
              <template v-slot:button-content>
                <i class="icon ik ik-globe languageIcon"></i>
              </template>

              <b-dropdown-item
                v-for="item in languages"
                :key="`language-${item}`"
                href="#"
                @click.prevent="setLanguage(item)"
                >{{ $t("component.header.language." + item) }}
              </b-dropdown-item>
            </b-dropdown>

            <!-- dashboard homepage button -->
            <router-link :to="{path: '/'}" :title="$t('component.menu.backToDashboard')">
              <i class="icon ik ik-home"></i>
            </router-link>

            <!-- user button-->
            <b-dropdown
              v-if="user.isLoggedIn()"
              id="userDropdown"
              :title="user.getEmail()"
              variant="link"
              right
            >
              <template v-slot:button-content>
                <i class="icon ik ik-user userIcon"></i>
              </template>
              <b-dropdown-item href="#" @click.prevent="logout">
                <i class="ik ik-log-out dropdown-icon"></i>
                {{ $t("component.header.account.logout") }}
              </b-dropdown-item>
            </b-dropdown>
          </div>
        </div>
      </div>
    </div>
  </div>

</div>
</template>

<script lang="ts">
import { Component, Prop, Watch} from "vue-property-decorator";
import Vue from "vue";
import { User } from "../../models/User";
import { Menu } from "../../models/Menu";
import store from "../../models/Store";


@Component
export default class DefaultHeaderComponent extends Vue {
  $i18n: any;
  $store: any;
  $opensilex: any;
  $route: any;
  $t: any;
  icon: any;
  title: any;
  description: any;

  @Prop()
  searchBoxIsActive: boolean;

  /**
   * Return the current connected user
   */
  get user() {
    return this.$store.state.user;
  }

  /**
   * Return the section path icon
   */
  get iconvalue() {
    const pathicon = this.$store.state.openSilexRouter.sectionAttributes[this.$route.path];
    if (!pathicon) {
      return ""
    }
    else {
      return pathicon.icon;
    }
  }

  /**
   * Return the section path title
   */
  get titlevalue() {
    let pathtitle = this.$store.state.openSilexRouter.sectionAttributes[this.$route.path];
    if (!pathtitle) {
      return undefined
    }
    else {
      return pathtitle.title;
    }
  }

  /**
   * Return the section path description
   */
  get descriptionevalue() {
    let pathdescription = this.$store.state.openSilexRouter.sectionAttributes[this.$route.path];
    if (!pathdescription) {
      return undefined
    }
    else {
      return pathdescription.description;
    }
  }

  /**
   * Return the current i18n language
   */
  get language() {
    return this.$i18n.locale;
  }

  /**
   * Return all available languages
   */
  get languages() {
    return Object.keys(this.$i18n.messages);
  }

  /**
   * Gets the version label string
   */
  get versionLabel(): string {
    if (!this.$opensilex.getConfig().versionLabel) {
      return undefined;
    }

    return this.$t("component.header.version-label." + this.$opensilex.getConfig().versionLabel.toLowerCase())
        .toString();
  }

  /**
   * Gets the class to use for the version label
   */
  get versionLabelClass(): string {
    if (!this.$opensilex.getConfig().versionLabel) {
      return undefined;
    }

    return this.$opensilex.getConfig().versionLabel.toLowerCase();
  }

  /**
   * Set the current i18n language
   */
  setLanguage(lang: string) {
    this.$i18n.locale = lang;
    this.$store.commit("lang", lang);
  }

  /**
   * Hide the header burger at start
   */
  data(){
    return {
      HeaderBurgerToggle : false,
    }
  }

  /**
   * Logout the current connected user -> have to redirected to the login page
   */
  logout() {
    this.$store.commit("logout");
  }

  /**
   * Gets the name of the application to display
   */
  get applicationName(): string {
    if (!this.$opensilex.getConfig().applicationName) {
      return undefined;
    }

    return this.$opensilex.getConfig().applicationName;
  }

  width;

  created() {
    window.addEventListener("resize", this.handleResize);
    this.handleResize();
  }

  beforeDestroy() {
    window.removeEventListener("resize", this.handleResize);
  }

  handleResize() {
    const minSize = 1025;
    if (
      document.body.clientWidth <= minSize &&
      (this.width == null || this.width > minSize)
    ) {
      this.width = document.body.clientWidth;
      this.$store.commit("hideMenu");
    } else if (
      document.body.clientWidth > minSize &&
      (this.width == null || this.width <= minSize)
    ) {
      this.width = document.body.clientWidth;
      this.$store.commit("showMenu");
    }
  }
}
</script>

<style scoped lang="scss">

.header-top {
  height: 65px;
}

.app-logo {
  text-align: left;
  position: absolute;
  width: 180px;
  height:60px;
  top: 0;
  left: 60px;
  background-color: rgb(0, 163, 141);
  padding-top: 6px;
  padding-bottom: 6px;
  padding-left: 35px;
}
.app-logo > a > img {
  height: 50px;
}

.app-title {
  font-family: "Eras Light ITC", Arial, Helvetica, sans-serif;
  margin-left: 10px;
  height: 50px;
  display: inline-block;
}

.header-brand .text {
    margin-left: 16px;
    color: #fff;
}

.title-icon {
  float: left;
  width: 40px;
  height: 40px;
  border-radius: 5px;
  margin-right: 20px;
  vertical-align: middle;
  font-size: 22px;
  color: #fff;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 12px -3px rgba(0, 0, 0, 0.5);
  background-color: #00a38d; 
}

.header-title{
  margin-bottom: 0px;
}
.header-title, .title-description {
  word-wrap: break-word
}
#menu-container {
  top: 60px!important;
}

.version-label-box {
  margin: 0 10px;
  padding: 5px 10px;
  border-radius: 5px;
  font-weight: bold;

  &.develop {
    background-color: #ff7800;
    color: white;
  }

  &.release {
    background-color: #cc338b;
    color: white;
  }
}
.container-fluid {
  margin-left: 240px;
  width: 85%;
}

.top-menu {
  position: absolute;
  float: right;
  top: 10px;
}

.topbarBtnHelp {
  height: 36px;
  line-height: 5px;
  border: none;
  width: 29px;
  padding-bottom: 2px;
  margin-bottom: 4px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Burger button*/
.headerburger {
  position: fixed;
  top: 0;
  right:5px;
  z-index: 2000;
}
/* Burger Icon Container*/
.hamburger-box {
  position: absolute;
  top: 3px;
  left: 5px;
}
/* Burger Icon*/
.hamburger-inner, .hamburger-inner:before, .hamburger-inner:after{
  width: 20px;
  height: 3px;
  background-color: #00a38d;
  justify-content: center;
  align-items: center;
  margin-top: 3px;
  margin-bottom: 3px;
}

.burgerMenuContainer {
  position: fixed;
  top: 30px;
  right: 5px;
}
.burgerMenuHelp {
  height: 15px;
  line-height: 5px;
  border: none;
  width: 5px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 8px;
}

/* Burger Transition */
.v-enter-active,
.v-leave-active {
  transition: opacity 0.5s ease;
}
.v-enter,
.v-leave-to {
  opacity: 0;
}

.helpButton {
    color: #00A38D;
    border-color: #00A38D;
    background-color: #FFFFFF;
    padding-bottom: 5px
}
.helpButton:hover{
    background-color: #F0F1F5;
    border-color: #00A38D;
    color: #00A38D;
}

.ik-home{
  font-size: 1.3em;
  vertical-align: middle;
  margin: -2px 0 0 12px
}

@media only screen and (min-width: 1380px) {
  .top-menu {
    margin-right:35px;
    transition: 1s;
  }
  .headerburger {
    display: none;
  }
  .burgerMenuContainer {
    display: none;
  }
  .languageIcon, .userIcon{
    margin: -2px 0 0 4px
  }
}
@media (min-width: 1151px) and (max-width: 1379px) {
  .container-fluid {
    width: 80%;
    transition: 1s;
  }
  .top-menu {
    margin-right:20px;
    transition: 1s;
  }
  .headerburger {
    display: none;
  }
  .burgerMenuContainer {
    display: none;
  }
  .languageIcon, .userIcon{
    margin: -2px 0 0 4px
  }
}
@media (min-width: 950px) and (max-width: 1150px) {
  .top-menu{
    margin-right: 110px;
  }
  .headerburger {
    display: none;
  }
  .burgerMenuContainer {
    display: none;
  }
}
@media (min-width: 800px) and (max-width: 949px) {
  .top-menu{
    margin-right: 120px;
    transition: 1s;
  }
  .headerburger {
    display: none;
  }
  .burgerMenuContainer {
    display: none;
  }
    .title-description{
    width:50%;
    display: block;
    line-height: 1.2;
    overflow: hidden;
    margin-left: -30px;
  }
}
@media (min-width: 676px) and (max-width: 799px) {
  .top-menu{
    margin-right: 150px;
    transition: 1s;
  }
  .headerburger {
    display: none;
  }
  .burgerMenuContainer {
    display: none;
  }
    .title-description{
    width:50%;
    display: block;
    line-height: 1.2;
    overflow: hidden;
    margin-left: -30px;
  }
}
@media (min-width: 250px) and (max-width: 1150px) {
  .topbarBtnHelp { 
    height: 25px;
    width: 25px;
    font-size: 85%;
    line-height: 9px;
  }
  .top-menu {
    margin-top: 10px;
  }
  .header-brand .text {
    font-size: 90%;
    margin-left: 45px;
  }

}
@media (min-width: 950px) and (max-width: 1150px) {
  .top-menu{
    margin-right: 110px;
  }
}
@media (min-width: 800px) and (max-width: 949px) {
  .top-menu{
    margin-right: 120px;
    transition: 1s;
  }
}
@media (min-width: 676px) and (max-width: 799px) {
  .top-menu{
    margin-right: 150px;
    transition: 1s;
  }
}
@media (min-width: 200px) and (max-width: 675px) {
  .app-logo {
    width: 150px;
  }
  .top-menu{
    margin-right: 170px;
    transition: 1s;
  }
  .title-icon{
    display: none;
  }
  .container-fluid {
    margin-left: 240px;
    transition: 1s;
    width: 50%;
  }
  .header-brand .text {
    margin-left: 5px;
  }
  .header-brand-img {
    margin-left: 0px;
  }
  .header-title, .title-description {
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .header-title {
    font-size: 1.1em;
    display: block;
    line-height: 1.2;
    font-weight: 600;
    overflow: hidden;
  }

  .title-description {
    width: 50%;
  }

  .headerMenuIcons, .topbarBtnHelp {
    display: none;
  }

}

.searchicon {
  font-size: 1.3em;
  color: #212121;
  vertical-align: middle;
  padding: 5px;
  background-color: rgba(0,0,0,0);
  border-color: rgba(0,0,0,0);
}

.burgerMenu-searchIcon{
  color: #212121;
  background-color: rgba(0,0,0,0);
  border-color: rgba(0,0,0,0);
}

.searchicon:hover, burgerMenu-searchIcon:hover {
  color: #007bff;
  background-color: #F0F1F5;
  border-color: #F0F1F5;
}

.selected-searchicon {
  color: #007bff;
}

</style>
