<template>
  <div class="container-fluid">
    <opensilex-Dashboard></opensilex-Dashboard>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import { TokenGetDTO, AuthenticationService } from "opensilex-security/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import Vue from "vue";

@Component
export default class HomeView extends Vue {
  $opensilex: OpenSilexVuePlugin;

  get user() {
    return this.$store.state.user;
  }

  mounted() {
    // console.log("ISLOGGEDIN", this.user.isLoggedIn());
    // if (!this.user.isLoggedIn()) {
    //   this.onLoginAsGuest();
    //   console.log("TEST", this.user, this.form);
    // }
  }

  get form() {
    return {
      email: "",
      password: "",
    };
  }

  onLoginAsGuest() {
    this.form.email = "admin@opensilex.org";
    this.form.password = "admin";
    console.log("this.form", this.form);
    this.login().then(() => {
      this.form.email = "";
      this.form.password = "";
    });
  }

  login() {
    this.$opensilex.showLoader();
    return this.$opensilex
      .getService<AuthenticationService>("opensilex-security.AuthenticationService")
      .authenticate({
        identifier: this.form.email,
        password: this.form.password,
      })
      .then((http: HttpResponse<OpenSilexResponse<TokenGetDTO>>) => {
        let user = this.$opensilex.fromToken(http.response.result.token);
        this.$opensilex.setCookieValue(user);
        this.$store.commit("login", user);
        this.$store.commit("refresh");
      });
  }
}
</script>

<style scoped lang="scss"></style>
