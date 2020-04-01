<template>
  <div>
    <div class="row">
      <b-input-group class="mt-3 mb-3 col-md" size="sm">
        <b-input-group>
          <b-form-input
            v-model="filterPattern"
            debounce="300"
            :placeholder="$t('component.infrastructure.filter-placeholder')"
          ></b-form-input>
          <template v-slot:append>
            <b-btn :disabled="!filterPattern" variant="primary" @click="filterPattern = ''">
              <font-awesome-icon icon="times" size="sm" />
            </b-btn>
          </template>
        </b-input-group>
      </b-input-group>
    </div>
    <div class="row">
      <div class="col-md">
        <sl-vue-tree v-model="nodes" @select="displayNodesDetail">
          <template slot="title" slot-scope="{ node }">
            <span class="item-icon">
              <i class="fa fa-file" v-if="node.isLeaf"></i>
              <i class="fa fa-folder" v-if="!node.isLeaf"></i>
            </span>
            <strong v-if="node.data.selected">{{ node.title }}</strong>
            <span v-if="!node.data.selected">{{ node.title }}</span>
            <b-button-group class="tree-button-group" size="sm">
              <b-button
                size="sm"
                v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
                @click.prevent="$emit('onEdit', node.data.uri)"
                variant="outline-primary"
              >
                <font-awesome-icon icon="edit" size="sm" />
              </b-button>
              <b-button
                size="sm"
                v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_MODIFICATION_ID)"
                @click.prevent="$emit('onAddChild', node.data.uri)"
                variant="outline-success"
              >
                <font-awesome-icon icon="plus" size="sm" />
              </b-button>
              <b-button
                size="sm"
                v-if="user.hasCredential(credentials.CREDENTIAL_INFRASTRUCTURE_DELETE_ID)"
                @click.prevent="$emit('onDelete', node.data.uri)"
                variant="danger"
              >
                <font-awesome-icon icon="trash-alt" size="sm" />
              </b-button>
            </b-button-group>
          </template>
        </sl-vue-tree>
      </div>
      <div class="col-md">
        <table class="table table-striped table-hover">
          <thead class="thead-dark">
            <tr>
              <th colspan="2">{{$t("component.infrastructure.detail")}}</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="capitalize-first-letter">{{$t("component.common.uri")}}</td>
              <td v-if="selected != null">{{selected.uri}}</td>
            </tr>
            <tr>
              <td class="capitalize-first-letter">{{$t("component.common.name")}}</td>
              <td v-if="selected != null">{{selected.name}}</td>
            </tr>
            <tr>
              <td class="capitalize-first-letter">{{$t("component.common.type")}}</td>
              <td v-if="selected != null">
                <span class="capitalize-first-letter">{{selected.typeLabel}}</span>
              </td>
            </tr>
            <tr>
              <td class="capitalize-first-letter">{{$t("component.user.users")}}</td>
              <td v-if="selected != null">
                <ul class="user-list">
                  <li v-for="userProfile in selected.userProfiles" v-bind:key="userProfile.uri">
                    <small>{{userProfile.userName}} - {{userProfile.profileName}}</small>
                  </li>
                </ul>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import {
  InfrastructuresService,
  ResourceTreeDTO,
  InfrastructureGetDTO
} from "opensilex-core/index";

@Component
export default class InfrastructureTree extends Vue {
  $opensilex: any;
  $store: any;
  service: InfrastructuresService;
  $i18n: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  private filterPatternValue: any = "";
  set filterPattern(value: string) {
    this.filterPatternValue = value;
    this.refresh();
  }

  get filterPattern() {
    return this.filterPatternValue;
  }

  created() {
    this.service = this.$opensilex.getService(
      "opensilex-core.InfrastructuresService"
    );

    this.refresh();

    this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        if (this.selected != null) {
          this.displayNodeDetail(this.selected.uri);
        }
      }
    );
  }

  refresh(uri?) {
    this.service
      .searchInfrastructuresTree(
        this.user.getAuthorizationHeader(),
        this.filterPattern
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
        let treeNode = [];

        let first = true;
        for (let i in http.response.result) {
          let resourceTree: ResourceTreeDTO = http.response.result[i];
          let node = this.dtoToNode(resourceTree, first, uri);
          treeNode.push(node);
          if (first && uri == null) {
            this.displayNodeDetail(node.data.uri);
            first = false;
          } else if (uri != null && uri == node.data.uri) {
            this.displayNodeDetail(node.data.uri);
          }
        }

        if (http.response.result.length == 0) {
          this.selected = null;
        }

        this.nodes = treeNode;

        this.$router
          .push({
            path: this.$route.fullPath,
            query: {
              filterPattern: encodeURI(this.filterPattern)
            }
          })
          .catch(function() {});
      })
      .catch(this.$opensilex.errorHandler);
  }

  private dtoToNode(dto: ResourceTreeDTO, first: boolean, uri: string) {
    let isLeaf = dto.children.length == 0;

    let childrenDTOs = [];
    if (!isLeaf) {
      for (let i in dto.children) {
        childrenDTOs.push(this.dtoToNode(dto.children[i], false, uri));
      }
    }

    let isSeleted = first && uri == null;
    if (uri != null) {
      isSeleted = uri == dto.uri;
    }
    return {
      title: dto.name,
      data: dto,
      isLeaf: isLeaf,
      children: childrenDTOs,
      isExpanded: true,
      isSelected: isSeleted,
      isDraggable: false,
      isSelectable: true
    };
  }

  public nodes = [];

  private selected: InfrastructureGetDTO = null;

  public displayNodesDetail(nodes: any[]) {
    if (nodes.length > 0) {
      let node = nodes[nodes.length - 1];
      this.displayNodeDetail(node.data.uri);
    }
  }

  public displayNodeDetail(uri: string) {
    return this.service
      .getInfrastructure(
        this.user.getAuthorizationHeader(),
        uri,
        this.$i18n.locale
      )
      .then((http: HttpResponse<OpenSilexResponse<InfrastructureGetDTO>>) => {
        let detailDTO: InfrastructureGetDTO = http.response.result;
        this.selected = detailDTO;
        this.$emit("onSelect", detailDTO);
      });
  }
}
</script>

<style scoped lang="scss">
.table {
  border: 1px solid #dee2e6;
  border-radius: 3px;
  border-collapse: separate;
}
.table th {
  text-align: center;
}

.table td {
  width: 100%;
}

.sl-vue-tree-root {
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
}

.user-list {
  padding-left: 10px;
}

@media (max-width: 768px) {
  .sl-vue-tree-root {
    min-height: auto;
  }

  .table {
    margin-top: 10px;
  }
}
</style>

