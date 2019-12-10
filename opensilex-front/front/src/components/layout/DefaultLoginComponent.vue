<template>
  <div>
    <a id="logout" href="#" @click="logout" ><span>Logout: {{user.getFirstName()}}</span>&nbsp;<font-awesome-icon icon="power-off" size="lg"/></a>
    <div class="fullmodal" v-if="!user.isLoggedIn()">
      <b-form @submit="onSubmit" class="fullmodal-form">
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
import { Component, Prop, Vue } from "vue-property-decorator";
import { SecurityService } from "opensilex/index";
import { OpenSilexVuePlugin } from "../../plugin/OpenSilexVuePlugin";
import HttpResponse from "opensilex/HttpResponse";
import { User } from "../../users/User";

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

  async created() {
    this.$opensilex.loadModule("opensilex");
  }

  logout(event) {
    event.preventDefault();
    this.$store.commit("logout");
  }

  onSubmit(event) {
    event.preventDefault();
    this.$store.commit("showLoader");
    var self = this;
    this.$opensilex
      .getService<SecurityService>("opensilex#SecurityService")
      .authenticate({
        identifier: this.form.email,
        password: this.form.password
      })
      .then(function(sucess: HttpResponse<any>) {
        let user = new User();
        user.setToken(sucess.response.result);
        self.$store.commit("login", user);
      })
      .catch(function() {
        // TODO 
        console.error("TODO: Invalid credentials", arguments);
      })
      .then(() => self.$store.commit("hideLoader"));
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

#logout > font-awesome-icon  {
  vertical-align: text-bottom;
  font-size: 24px;
}

@media (max-width: 600px) {
  #logout > span{
    display: none;
  }
}
</style>
