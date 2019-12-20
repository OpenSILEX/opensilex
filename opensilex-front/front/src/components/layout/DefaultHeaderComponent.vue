<template>
  <div class="container-fluid boxed-layout">
      <div class="d-flex justify-content-end">
          <div class="top-menu d-flex align-items-center">
              <div class="dropdown">
                  <a class="dropdown-toggle" href="#" id="langDropdown" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                      <i class="icon ik ik-globe"></i>
                      Français
                      <i class="ik ik-chevron-down"></i>
                  </a>
                  <div class="dropdown-menu dropdown-menu-right" aria-labelledby="userDropdown">
                      <a class="dropdown-item" href="#">Français</a>
                      <a class="dropdown-item" href="#">Anglais</a>
                  </div>
              </div>

              <div class="dropdown">
                  <a class="dropdown-toggle" href="#" id="userDropdown" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false" title="gilles.simon@inra.fr">                                    
                      <i class="icon ik ik-user"></i>
                      {{user.getFirstName()}} {{user.getLastName()}} 
                      <strong>(Admin)</strong>
                      <i class="ik ik-chevron-down"></i>
                  </a>
                  <div class="dropdown-menu dropdown-menu-right" aria-labelledby="userDropdown">
                      <a id="logout" class="dropdown-item" href="#" @click.prevent="logout"><i class="ik ik-log-out dropdown-icon"></i> Logout</a>
                  </div>
              </div>
          </div>
      </div>
  </div>
</template>

<script lang="ts">
import { Component } from 'vue-property-decorator';
import Vue from 'vue';
import { User } from "../../models/User";

@Component
export default class DefaultHeaderComponent extends Vue {

  get user() {
    return this.$store.state.user;
  }

}
</script>

<style scoped lang="scss">
</style>
