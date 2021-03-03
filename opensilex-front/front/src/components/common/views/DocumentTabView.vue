<template>
  <div class="row">
    <div class="col col-xl-12">
      <opensilex-DocumentTabList
        :uri="uri"
        :modificationCredentialId="modificationCredentialId"
        :deleteCredentialId="deleteCredentialId"
      ></opensilex-DocumentTabList>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
@Component
export default class DocumentTabView extends Vue {
  $opensilex: any;
  $t: any;
  $route: any;

  @Ref("tableRef") readonly tableRef!: any;
  @Ref("documentForm") readonly documentForm!: any;

  @Prop()
  modificationCredentialId: string;

  @Prop()
  deleteCredentialId: string;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  @Prop()
  uri: string;
}
</script>

<style scoped lang="scss">
</style>