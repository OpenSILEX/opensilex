<template>
  <div>
    <b-button
      @click="showCreateForm"
      variant="success"
      v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
    >{{$t('component.infrastructure.add')}}</b-button>
    <opensilex-core-InfrastructureTree></opensilex-core-InfrastructureTree>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { InfrastructuresService } from "../../lib/api/api";
import { ResourceTreeDTO, InfrastructureGetDTO } from "../../lib";

@Component
export default class InfrastructureView extends Vue {
  $opensilex: any;
  $store: any;
  service: InfrastructuresService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
}
</script>

<style scoped lang="scss">
</style>

