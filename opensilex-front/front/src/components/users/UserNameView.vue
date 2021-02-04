<template>
  <span>
    {{ user.first_name }} {{ user.last_name }}
    <button
      v-on:click.prevent="copyEmail(user.email)"
      class="uri-copy"
      :title="user.email"
    >
      <i class="ik ik-copy"></i>
    </button>
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import copy from "copy-to-clipboard";

@Component
export default class UserNameView extends Vue {
  $opensilex: any;
  $t: any;

  @Prop()
  user: any;

  copyEmail(address) {
    copy(address);
    this.$opensilex.showSuccessToast(
      this.$t("component.common.email-copy") + ": " + address
    );
  }
}
</script>

<style scoped lang="scss">
</style>

