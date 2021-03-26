<template>
  <sl-vue-tree
    ref="asyncTree"
    v-model="nodeList"
    @select="selectItem"
    @toggle="toggle"
  >
    <template slot="toggle" slot-scope="{ node }">
      <span class="toggle-icon" v-if="!node.isLeaf">
        <opensilex-Icon v-if="node.isExpanded" icon="fa#chevron-down" />
        <opensilex-Icon v-if="!node.isExpanded" icon="fa#chevron-right" />
      </span>
    </template>

    <template slot="title" slot-scope="{ node }">
      <span v-if="node.data != null">
        <div v-if="node.isLeaf && node.data.parent" class="leaf-spacer"></div>
        <b-form-checkbox
          class="selection-box"
          v-if="enableSelection"
          :checked="getSelection(node.data.uri)"
          unchecked-value="false"
          @change="onSelectionChange(node.data.uri)"
          switches
        ></b-form-checkbox>
        <strong v-if="node.data.selected">
          <slot name="node" v-bind:node="node"></slot>
        </strong>
        <span v-if="!node.data.selected">
          <slot name="node" v-bind:node="node">></slot>
        </span>

        <span
          class="async-tree-action"
          v-if="
            node.children.length > 0 &&
            node.data.child_count > 0 &&
            node.data.child_count > node.children.length
          "
        >
          ({{ node.data.rdf_type_name }} - {{ node.children.length }}/{{
            node.data.child_count
          }}
          <span>
            -
            <a href="#" @click.prevent="loadMoreChildren(node)">
              {{ $t("TreeViewAsync.load-more") }}
            </a> </span
          >)
        </span>
        <span class="async-tree-action" v-else
          >&nbsp;({{ node.data.rdf_type_name }})</span
        >

        <b-button-group v-if="!noButtons" class="tree-button-group" size="sm">
          <slot name="buttons" v-bind:node="node"></slot>
        </b-button-group>
      </span>
      <span ref="load" v-else>{{ $t("TreeViewAsync.loading-more") }}</span>
    </template>
  </sl-vue-tree>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref, Watch } from "vue-property-decorator";
import copy from "copy-to-clipboard";
import Vue from "vue";

@Component
export default class TreeViewAsync extends Vue {
  nodeList: any = [];

  @Ref("asyncTree") readonly asyncTree!: any;

  @Prop()
  noButtons: boolean;

  @Prop({
    default: () => [],
  })
  searchMethod: Function;

  @Prop()
  searchMethodRoot: Function;

  @Prop()
  searchMethodRootChildren: Function;

  @Prop({
    default: 10,
  })
  pageSize: number;

  @Prop({
    default: false,
  })
  enableSelection;

  @PropSync("selection")
  multiSelect;

  copy = copy;

  selectItem(nodes: any[]) {
    if (nodes.length > 0) {
      let node = nodes[nodes.length - 1];
      if (node.data != null) {
        this.$emit("select", nodes[0]);
      }
    }
  }

  @Ref("load") readonly load!: any;
  private observer;
  private page = 0;

  mounted() {
    let self = this;
    this.observer = new IntersectionObserver(function ([
      { isIntersecting, target },
    ]) {
      let t: any = target;
      if (isIntersecting) {
        const ul = t.offsetParent;
        const scrollTop = t.offsetParent.scrollTop;
        self.page = self.page + 1;
        self.searchMethod(undefined, self.page, self.pageSize).then((http) => {
          self.nodeList.pop();
          self.updateTreeNodes(http);
          self.$nextTick().then(() => {
            ul.scrollTop = scrollTop;
          });
        });
      }
    });

    this.refresh();
  }

  updated() {
    this.$nextTick(() => {
      if (this.observer) {
        this.observer.disconnect();
      }
      if (this.load) {
        this.observer.observe(this.load);
      }
    });
  }

  beforeDestroy() {
    this.observer.disconnect();
  }

  refresh() {
    this.loadedRoots = [];
    if (this.searchMethodRoot) {
      this.searchMethodRoot(0, this.pageSize).then((http) => {
        this.nodeList = [];
        this.updateTreeNodes(http);
      });
    } else {
      this.searchMethod(undefined, 0, this.pageSize).then((http) => {
        this.nodeList = [];
        this.updateTreeNodes(http);
      });
    }
  }

  updateTreeNodes(http) {
    for (let i in http.response.result) {
      let soDTO = http.response.result[i];
      let soNode = {
        title: soDTO.name,
        data: soDTO,
        isLeaf: "child_count" in soDTO && soDTO.child_count == 0,
        children: [],
        isExpanded: false,
        isSelected: false,
        isDraggable: false,
        isSelectable: true,
      };
      this.nodeList.push(soNode);
    }

    let count = http.response.metadata.pagination.totalCount;
    let isEmpty =
      !Array.isArray(http.response.result) || http.response.result.length == 0;
    if (!isEmpty && count > this.nodeList.length) {
      this.nodeList.push({
        title: "Loading more",
        data: null,
        isLeaf: true,
        children: [],
        isExpanded: false,
        isSelected: false,
        isDraggable: false,
        isSelectable: false,
      });
    }
  }

  private loadedRoots = [];

  loadMoreChildren(node) {
    let nodeURI = node.data.uri;

    let root = this.nodeList[node.path[0]];
    for (let i = 1; i < node.path.length; i++) {
      root = root.children[node.path[i]];
    }

    if (
      this.searchMethodRootChildren &&
      node.path.length == 1 &&
      this.loadedRoots.indexOf(root.data.uri) < 0
    ) {
      let page = Math.round(root.children.length / this.pageSize);
      this.searchMethodRootChildren(nodeURI, page, this.pageSize).then(
        (http) => {
          let childrenNodes = [];

          for (let i in http.response.result) {
            let soDTO = http.response.result[i];

            let soNode = {
              title: soDTO.name,
              data: soDTO,
              isLeaf: "child_count" in soDTO && soDTO.child_count == 0,
              children: [],
              isExpanded: false,
              isSelected: false,
              isDraggable: false,
              isSelectable: true,
            };
            childrenNodes.push(soNode);
          }

          root.children = root.children.concat(childrenNodes);

          if (!("child_count" in root.data) && root.children.length == 0) {
            root.data.child_count = 0;
            root.isLeaf = true;
          }

          this.loadedRoots.push(root.data.uri);
        }
      );
    } else if (root.children.length < node.data.child_count) {
      let page = Math.round(root.children.length / this.pageSize);
      this.searchMethod(nodeURI, page, this.pageSize).then((http) => {
        let childrenNodes = [];
        for (let i in http.response.result) {
          let soDTO = http.response.result[i];

          let soNode = {
            title: soDTO.name,
            data: soDTO,
            isLeaf: "child_count" in root.data && soDTO.child_count == 0,
            children: [],
            isExpanded: false,
            isSelected: false,
            isDraggable: false,
            isSelectable: true,
          };
          childrenNodes.push(soNode);
        }

        root.children = root.children.concat(childrenNodes);
      });
    }
  }

  toggle(node) {
    if (
      !("child_count" in node.data) ||
      (node.children.length == 0 && node.data.child_count > 0)
    ) {
      this.loadMoreChildren(node);
    }
    this.$emit("toggle", node);
  }

  getSelection(uri) {
    let r = false;
    if (this.multiSelect) {
      r = this.multiSelect.indexOf(uri) >= 0;
    }
    return r;
  }

  onSelectionChange(uri) {
    if (this.multiSelect) {
      let uriIndex = this.multiSelect.indexOf(uri);
      if (uriIndex >= 0) {
        this.multiSelect.splice(uriIndex, 1);
      } else {
        this.multiSelect.push(uri);
      }
    }
  }
}
</script>

<style scoped lang="scss">
.toggle-icon {
  padding-left: 5px;
  padding-right: 5px;
}

.async-tree-action {
  font-style: italic;
}

.async-tree-action a:hover {
  text-decoration: underline;
  cursor: pointer;
}

::v-deep .sl-vue-tree-nodes-list {
  overflow-y: scroll;
  max-height: 300px;
}

::v-deep .sl-vue-tree-nodes-list .sl-vue-tree .sl-vue-tree-nodes-list {
  overflow-y: hidden;
  max-height: none;
}

.selection-box {
  display: inline;
  position: absolute;
  margin-top: 1px;
}
</style>

<i18n>
en:
  TreeViewAsync:
    load-more: Load more...
    loading-more: Loading more elements...

fr:
  TreeViewAsync:
    load-more: Charger plus...
    loading-more: Chargement en cours ...
</i18n>
