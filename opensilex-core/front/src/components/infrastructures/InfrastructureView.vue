<template>
  <div>
    <sl-vue-tree v-model="nodes">
      <template slot="title" slot-scope="{ node }">
        <span class="item-icon">
          <i class="fa fa-file" v-if="node.isLeaf"></i>
          <i class="fa fa-folder" v-if="!node.isLeaf"></i>
        </span>
        {{ node.title }}
      </template>
    </sl-vue-tree>
  </div>
</template>

<script lang="ts">
import { Component } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { InfrastructuresService } from "../../lib/api/api";
import { ResourceTreeDTO } from "../../lib";

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

  created() {
    this.service = this.$opensilex.getService(
      "opensilex.InfrastructuresService"
    );

    this.service
      .searchInfrastructuresTree(this.user.getAuthorizationHeader())
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        console.error(http.response.result);
        // TODO map to nodes
      });
  }

  public nodes = [
    { title: "Item1", isLeaf: true },
    { title: "Item2", isLeaf: true, data: { visible: false } },
    { title: "Folder1" },
    {
      title: "Folder2",
      isExpanded: true,
      children: [
        { title: "Item3", isLeaf: true },
        { title: "Item4", isLeaf: true }
      ]
    }
  ];

  // TODO chercher l'arbre
}
</script>

<style scoped lang="scss">
</style>

