<template>
  <div class="header-top" header-theme="light">
    <div class="app-logo">
      <div class="header-brand" to="/">
        <div class="logo-img">
          <img
            v-bind:src="$opensilex.getResourceURI('images/logo-phis.svg')"
            class="header-brand-img"
            alt="lavalite"
          />
        </div>
        <span class="text">
          PHIS
        </span>
      </div>
    </div>

    <div class="container-fluid boxed-layout">
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

          <opensilex-HelpButton
            @click="$opensilex.getGuideFile()"
            label="component.header.user-guide"
          ></opensilex-HelpButton>

          <b-dropdown
            id="langDropdown"
            :title="user.getEmail()"
            variant="link"
            right
          >
            <template v-slot:button-content>
              <i class="icon ik ik-globe"></i>
              <span class="hidden-phone">{{ $t("component.header.language." + language) }}</span>
              <span class="show-phone">{{ $t("component.header.language." + language).substring(0,2) }}</span>
              <i class="ik ik-chevron-down"></i>
            </template>

            <b-dropdown-item
              v-for="item in languages"
              :key="`language-${item}`"
              href="#"
              @click.prevent="setLanguage(item)"
              >{{ $t("component.header.language." + item) }}</b-dropdown-item
            >
          </b-dropdown>

          <b-dropdown
            v-if="user.isLoggedIn()"
            id="userDropdown"
            :title="user.getEmail()"
            variant="link"
            right
          >
            <template v-slot:button-content>
              <i class="icon ik ik-user"></i>
              <span class="hidden-phone">
              {{ user.getFirstName() }} {{ user.getLastName() }}
              <strong v-if="user.isAdmin()"
                >({{ $t("component.header.user.admin") }})</strong
              >
              </span>
              <i class="ik ik-chevron-down"></i>
            </template>
            <b-dropdown-item href="#" @click.prevent="logout">
              <i class="ik ik-log-out dropdown-icon"></i>
              {{ $t("component.header.user.logout") }}
            </b-dropdown-item>
          </b-dropdown>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class DefaultHeaderComponent extends Vue {
  $i18n: any;
  $store: any;
  $opensilex: OpenSilexVuePlugin;

  /**
   * Return the current connected user
   */
  get user() {
    return this.$store.state.user;
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
   * Logout the current connected user -> have to redirected to the login page
   */
  logout() {
    this.$store.commit("logout");
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
    const minSize = 768;
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
.app-logo {
  text-align: left;
  position: absolute;
  width: 180px;
  top: 0;
  background-color: rgb(0, 163, 141);
  padding-top: 6px;
  padding-bottom: 6px;
  padding-left: 15px;
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
</style>
