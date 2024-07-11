<template>
  <div class="fadeInDown fullmodal" v-if="!user.isLoggedIn() || forceRefresh">
    <ValidationObserver ref="validatorRef">
      <b-form @submit.prevent="onLogin" class="fullmodal-form">
        <div class="login-header">
          <img v-bind:src="$opensilex.getResourceURI('images/logo-siduri.png')" />
          <h2>.title')}}</h2>
        </div>

        <div class="login-form-content">
          <p>.login.info')}}:</p>
          <b-form-group id="login-group" required>
            <ValidationProvider
              :name="$t('component.login.validation.email')"
              rules="required|emailOrUrl"
              v-slot="{ errors }"
            >
              <b-form-input
                id="email"
                type="email"
                v-model="form.email"
                required
                :placeholder="$t('component.login.input.email')"
              ></b-form-input>
              <i class="ik ik-user"></i>
              <div class="error-message alert alert-danger"></div>
            </ValidationProvider>
          </b-form-group>

          <b-form-group id="password-group" required>
            <ValidationProvider
              :name="$t('component.login.validation.password')"
              rules="required"
              v-slot="{ errors }"
            >
              <b-form-input
                id="password"
                type="password"
                v-model="form.password"
                required
                :placeholder="$t('component.login.input.password')"
              ></b-form-input>
              <i class="ik ik-lock"></i>
              <div class="error-message alert alert-danger"></div>
            </ValidationProvider>
          </b-form-group>
          <div class="sign-btn text-center">
            <b-button type="submit" variant="primary" v-text="$t('component.login.button.login')"></b-button>
          </div>
        </div>
      </b-form>
    </ValidationObserver>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import DefaultLoginComponent from "../../../../../opensilex-front/front/src/components/layout/DefaultLoginComponent.vue";

@Component
export default class SiduriLoginComponent extends DefaultLoginComponent {}
</script>

<style scoped lang="scss">

</style>
