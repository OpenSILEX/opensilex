<template>
  <div class="card">
    <div class="card-header row clearfix">
      <div class="d-inline-block w-100 float-left">
        <b-nav pills>
          <router-link
            v-if="returnTo && returnButton"
            :to="returnTo"
            v-slot="{ href, route, navigate,}"
          >
            <li>
              <a
                class="btn btn-outline-primary back-button mr-2 h-100"
                :href="href"
                :title="$t(returnToTitle)"
                @click="navigate"
              >
                <opensilex-Icon icon="ik#ik-corner-up-left" class="icon-title" />
              </a>
            </li>
          </router-link>
          <router-link
            v-if="!returnTo && returnButton"
            to="/"
            class="ml-3"
            :title="$t(returnToTitle)"
            event
            @click.native.prevent="$router.go(-1)"
          >
            <li>
              <a class="btn btn-outline-primary mr-2 h-100 back-button">
                <opensilex-Icon class="icon-title" icon="ik#ik-corner-up-left" />
              </a>
            </li>
          </router-link>
          <slot></slot>
        </b-nav>
      </div>
       <div class="card-header-right">
        <slot name="rightHeader"></slot>
      </div>
    </div>
     
  </div>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class PageActions extends Vue {
  @Prop()
  returnTo: string;

  @Prop({ default: "component.pageActions.returnToTitle" })
  returnToTitle: string;

  @Prop({ default: false })
  returnButton: boolean;
}
</script>

<style scoped lang="scss">
.back-button {
  display: flex;
  justify-content: center;
  align-items: center;
}

button {
  margin-left: 1%;
}

li:first-child.nav-item {
  margin-left: 1%;
}
</style>

<i18n>
en:
  component: 
    pageActions: 
        returnToTitle: Return to the previous page
         
    
            
fr:
  component: 
    pageActions: 
        returnToTitle: Retourner à la page précédente

</i18n>