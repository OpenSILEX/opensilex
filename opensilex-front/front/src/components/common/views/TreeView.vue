<template>
  <sl-vue-tree v-model="nodeList"
               @select="onSelectItem"
               @toggle="$emit('toggle', $event)"
               ref="slVueTree"
  >
    <template slot="toggle" slot-scope="{ node }">
      <span class="toggle-icon" v-if="!node.isLeaf">
        <opensilex-Icon v-if="node.isExpanded" icon="fa#chevron-down" />
        <opensilex-Icon v-if="!node.isExpanded" icon="fa#chevron-right" />
      </span>
    </template>

    <template slot="title" slot-scope="{ node }">
      <div v-if="node.isLeaf && node.data.parent" class="leaf-spacer"></div>

      <strong v-if="node.data.selected">
        <slot name="node" v-bind:node="node"></slot>
      </strong>
      <span v-if="!node.data.selected">
        <slot name="node" v-bind:node="node">></slot>
      </span>

      <!-- Clicking on the button group does not trigger a select -->
      <b-button-group v-if="!noButtons" class="tree-button-group" size="sm" @mouseup.prevent.stop>
        <slot name="buttons" v-bind:node="node"></slot>
      </b-button-group>
    </template>
  </sl-vue-tree>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import copy from "copy-to-clipboard";
import Vue from "vue";
import SlVueTree, { ISlTreeNodeModel } from "sl-vue-tree";

@Component
export default class TreeView<T> extends Vue {
  @PropSync("nodes")
  nodeList: Array<ISlTreeNodeModel<T>>;

  @Prop()
  noButtons: boolean;

  @Ref("slVueTree") slVueTree: SlVueTree<T>;

  copy = copy;

  onSelectItem(nodes: any[]) {
    if (nodes.length > 0) {
      this.$emit("select", nodes[0]);
    }
  }

  /**
   * Return the current selected node
   */
  getSelectedNode(): T | undefined {
    let selectedNode = this.slVueTree.getSelected();
    if (!Array.isArray(selectedNode) || selectedNode.length === 0) {
      return undefined;
    }
    return selectedNode[0].data;
  }
}
</script>

<style scoped lang="scss">
.toggle-icon {
  padding-left: 5px;
  padding-right: 5px;
}
</style>
