<template>

    <div class="header-top" header-theme="light">
        <div class="container-fluid boxed-layout">
          <div class="d-flex justify-content-end">
              <div class="top-menu d-flex align-items-center">

                <b-dropdown id="langDropdown" :title="user.getEmail()" variant="link" right>
                  <template v-slot:button-content>
                    <i class="icon ik ik-globe"></i>
                    {{ $t('component.header.language.' + language) }}
                    <i class="ik ik-chevron-down"></i>
                  </template>

                  <b-dropdown-item v-for="item in languages" :key="`language-${item}`" href="#" @click.prevent="setLanguage(item)">{{ $t('component.header.language.' + item) }}</b-dropdown-item>
                </b-dropdown>

                <b-dropdown id="userDropdown" :title="user.getEmail()" variant="link" right>
                    <template v-slot:button-content>
                    <i class="icon ik ik-user"></i>
                    {{user.getFirstName()}} {{user.getLastName()}}
                    <strong v-if="user.isAdmin()">({{ $t('component.header.user.admin') }})</strong>
                    <i class="ik ik-chevron-down"></i>
                    </template>
                    <b-dropdown-item href="#" @click.prevent="logout">
                    <i class="ik ik-log-out dropdown-icon"></i> {{ $t('component.header.user.logout') }}
                    </b-dropdown-item>
                </b-dropdown>
              </div>
          </div>
        </div>
    </div>

</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import { User } from "../../models/User";

@Component
export default class DefaultHeaderComponent extends Vue {
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
   * Set the current i18n language
   */
  setLanguage(lang: string) {
    this.$i18n.locale = lang;
  }

  /**
   * Logout the current connected user -> have to redirected to the login page
   */
  logout() {
    this.$store.commit("logout");
  }
}
</script>

<style scoped lang="scss">
</style>
