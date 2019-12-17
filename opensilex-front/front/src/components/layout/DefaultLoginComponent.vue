<template>
  <div>
    <a id="logout" href="#" @click.prevent="logout">
      <span>Logout: {{user.getFirstName()}}</span>&nbsp;
      <font-awesome-icon icon="power-off" size="lg" />
    </a>
    <div class="fullmodal" v-if="!user.isLoggedIn() || forceRefresh">
      <b-form @submit.prevent="onLogin" class="fullmodal-form">
        <h2>Welcome to opensilex</h2>
        <b-form-group
          id="login-group"
          label="Login:"
          label-for="email"
          required
          description="Please enter your email."
        >
          <b-form-input
            id="email"
            type="email"
            v-model="form.email"
            required
            placeholder="Enter your email"
          ></b-form-input>
        </b-form-group>
        <b-form-group
          id="password-group"
          label="Password:"
          label-for="password"
          required
          description="Please enter your password."
        >
          <b-form-input
            id="password"
            type="password"
            v-model="form.password"
            required
            placeholder="Enter your password"
          ></b-form-input>
        </b-form-group>
        <b-button type="reset" variant="danger">Clear</b-button>&nbsp;
        <b-button type="submit" variant="primary">Login</b-button>
      </b-form>
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import { SecurityService, TokenGetDTO } from "opensilex/index";
import { OpenSilexVuePlugin } from "../../models/OpenSilexVuePlugin";
import HttpResponse, { OpenSilexResponse } from "opensilex/HttpResponse";
import { User } from "../../models/User";

@Component
export default class DefaultLoginComponent extends Vue {
  get form() {
    return {
      email: "",
      password: ""
    };
  }

  $store: any;

  get user() {
    return this.$store.state.user;
  }

  $opensilex: OpenSilexVuePlugin;

  static async asyncInit($opensilex: OpenSilexVuePlugin) {
    await $opensilex.loadService("opensilex.SecurityService");
  }

  logout() {
    this.$store.commit("logout");
    this.$router.push("/");
  }

  forceRefresh = false;
  onLogin() {
    this.$opensilex.showLoader();
    this.$opensilex
      .getService<SecurityService>("opensilex.SecurityService")
      .authenticate({
        identifier: this.form.email,
        password: this.form.password
      })
      .then((http: HttpResponse<OpenSilexResponse<TokenGetDTO>>) => {
        let user = new User();
        user.setToken(http.response.result.token);
        this.forceRefresh = true;
        this.$store.commit("login", user);
        this.$store.commit("refresh");
      })
      .catch(error => {
        if (error.status == 403) {
          console.error("TODO invalid crevalid credentials error", error);
          // TODO display invalid crevalid credentials error
        } else {
          this.$opensilex.errorHandler(error);
        }
        this.$opensilex.hideLoader();
      });
  }
}
</script>

<style scoped lang="scss">
@import "../../../styles/styles";

.fullmodal {
  display: block;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100%;
  width: 100%;
  z-index: 9999;
  background: getVar(--defaultColorLight);
}

.fullmodal-form {
  position: relative;
  top: 50%;
  transform: translateY(-50%);
  width: 50%;
  margin: auto;
}

#logout {
  color: getVar(--linkColor);
  display: inline-block;
}

#logout:hover {
  color: getVar(--linkHighlightColor);
}

#logout > font-awesome-icon {
  vertical-align: text-bottom;
  font-size: 24px;
}

@media (max-width: 600px) {
  #logout > span {
    display: none;
  }
}
</style>
