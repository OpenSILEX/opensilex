<template>
  <div class="row">
    <div class="card">
      <div class="card-header row clearfix">
        <div class="col col-sm-6">
          <div class="card-options d-inline-block">
            <ul class="nav nav-tabs">
              <router-link v-if="returnTo" :to="returnTo" v-slot="{ href, route, navigate,}">
                <li>
                  <a
                    class="btn btn-outline-primary mb-2 mr-2"
                    :href="href"
                    :title="returnToTitle"
                    @click="navigate"
                  >
                    <opensilex-Icon icon="ik#ik-corner-up-left" class="icon-title" />
                  </a>
                </li>
              </router-link>
              <slot name="linksLeft"></slot>
            </ul>
          </div>
        </div>
        <div class="col col-sm-3"></div>
        <div class="col col-sm-3">
          <div class="card-options text-right">
            <slot name="linksRight"></slot>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class NavBar extends Vue {
  $opensilex: any;
  $store: any;
  $router: any;
  $t: any;
  $i18n: any;

  @Prop()
  returnTo: string;
  @Prop({ default: "returnToTitle" })
  returnToTitle: string;
}
</script>


<style scoped lang="scss">
</style>
