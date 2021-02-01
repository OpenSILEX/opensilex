<template>
    <div class="row">
      <div class="col col-xl-12">
        <opensilex-DocumentTabList
                :modificationCredentialId="credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID"
                :deleteCredentialId="credentials.CREDENTIAL_EXPERIMENT_DELETE_ID"
        ></opensilex-DocumentTabList>
      </div>
  </div>
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  DocumentGetDTO,
  DocumentsService
} from "opensilex-core/index";
import {
  SecurityService,
  GroupDTO,
  UserGetDTO
} from "opensilex-security/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
@Component
export default class ExperimentDocument extends Vue {
  $opensilex: any;
  $t: any;
  $route: any;
  uri = null;

  @Ref("tableRef") readonly tableRef!: any;
  @Ref("documentForm") readonly documentForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
  }

}
</script>

<style scoped lang="scss">
</style>