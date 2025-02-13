<template>
  <div>
    <!-- <div class=" row clearfix" :style="(small ? 'padding: 8px 20px;' : '')">
      <div class="d-inline-block w-100 float-left">
        <slot name="buttons"></slot>
        <b-nav :tabs="(tabs == true ? true : false)">
          <router-link
            v-if="returnTo && returnButton"
            :to="returnTo"
            v-slot="{ href, route, navigate}"
            @click.native="goBack"
          >
            <a
              class="btn back-button mr-2 h-100"
              :href="href"
              :title="$t(returnToTitle)"
              @click="navigate"
            >
              <opensilex-Icon icon="ik#ik-corner-up-left" class="icon-title back-button-icon" />
            </a>
          </router-link>
          <router-link
            v-if="!returnTo && returnButton"
            to="/"
            :title="$t(returnToTitle)"
            event
            @click.native.prevent="$router.go(-1)"
          >
            <a class="btn mr-2 h-100 back-button">
              <opensilex-Icon class="icon-title back-button-icon" icon="ik#ik-corner-up-left" />
            </a>
          </router-link>
          <slot></slot>
        </b-nav>
      </div>
      <div class="card-header-right">
        <slot name="rightHeader"></slot>
      </div>
    </div> -->
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
    // computed: {
    //     returnTo() {
    //         if (this.$store.state.previousPage.length > 0) {
    //           return this.$store.state.previousPage[
    //             this.$store.state.previousPage.length - 1
    //           ];
    //         } else {
    //           return false;
    //         }
    //     }
    // },
    // methods: {
    //     goBack() {
    //         this.$store.commit("goBack");
    //     }
    // },
    // props: {
    //     returnToTitle: { default: "component.pageActions.returnToTitle",
    //         type: String
    //     },
    //     returnButton: { default: false,
    //         type: Boolean
    //     },
    //     tabs: { default: false,
    //         type: Boolean
    //     },
    //     small: { default: false,
    //         type: Boolean
    //     }
    // }
})

</script>

<style scoped lang="scss">

.nav {
  margin-left: 14px;
}
button {
  margin-left: 1%;
}

li:first-child.nav-item {
  margin-left: 1%;
}

</style>

<i18n>


</i18n>