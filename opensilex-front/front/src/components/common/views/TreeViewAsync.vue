<template>
  <Overlay :show="isSearching && !isGlobalLoaderVisible">
  <sl-vue-tree
    ref="asyncTree"
    v-model="nodeList"
    @nodeclick="selectItem"
    @toggle="toggle"
  >
    <template #toggle="{ node }">
      <span class="toggle-icon" v-if="!node.isLeaf">
        <Icon v-if="node.isExpanded" icon="fa#chevron-down" />
        <Icon v-if="!node.isExpanded" icon="fa#chevron-right" />
      </span>
    </template>
    <template #title="{ node }">
      <span v-if="node.data != null">
        <div v-if="node.isLeaf && node.data.parent" class="leaf-spacer"></div>
        <b-form-checkbox
          aria-checked="mixed"
          class="selection-box"
          v-if="enableSelection"
          :checked="getSelection(node.data.uri)"
          @change="onSelectionChange(node.data.uri)"
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
  </Overlay>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, onUpdated, ref, useTemplateRef } from "vue";
import { useStore } from "vuex";
import copyToClipboard from "copy-to-clipboard";
import Icon from "@/components/common/views/Icon.vue";
import Overlay from "@/components/layout/Overlay.vue";

const store = useStore();

const props = withDefaults(
  defineProps<{
    noButtons?: boolean;
    searchMethod?: Function;
    searchMethodRoot?: Function;
    searchMethodRootChildren?: Function;
    pageSize?: number;
    enableSelection?: boolean;
    selection?: any;
  }>(),
  {
    noButtons: false,
    pageSize: 10,
    enableSelection: false,
  }
);

const emit = defineEmits<{
  (e: "select", node: any): void;
  (e: "toggle", node: any): void;
  (e: "update:selection", value: any): void;
}>();

const multiSelect = computed({
  get: () => props.selection,
  set: (value) => emit("update:selection", value),
});

const nodeList = ref<any[]>([]);
const isSearching = ref(false);
const copy = copyToClipboard;

const asyncTree = useTemplateRef<any>("asyncTree");
const load = useTemplateRef<any>("load");

const isGlobalLoaderVisible = computed(() => store.state.loaderVisible);

let observer: IntersectionObserver;
let page = 0;
let loadedRoots: any[] = [];

function selectItem(node: any) {
  if (node.data != null) {
    emit("select", node);
  }
}

function getNodeParent(node: any) {
  console.debug("getNodeParent", node.path);
  let array: any[] = node.path;
  array.pop();
  array.push(0);
  let firstBranchNode = asyncTree.value.getNode(array);
  let root = asyncTree.value.getPrevNode(firstBranchNode);
  console.debug("nodeParent", root);

  return root;
}

onMounted(() => {
  observer = new IntersectionObserver(function ([
    { isIntersecting, target },
  ]) {
    let t: any = target;
    if (isIntersecting) {
      const ul = t.offsetParent;
      const scrollTop = t.offsetParent.scrollTop;
      page = page + 1;
      props.searchMethod(undefined, page, props.pageSize).then((http: any) => {
        nodeList.value.pop();
        updateTreeNodes(http);
        nextTick().then(() => {
          ul.scrollTop = scrollTop;
        });
      });
    }
  });

  refresh();
});

onUpdated(() => {
  nextTick(() => {
    if (observer) {
      observer.disconnect();
    }
    if (load.value) {
      observer.observe(load.value);
    }
  });
});

onBeforeUnmount(() => {
  observer.disconnect();
});

function refresh() {
  isSearching.value = true;
  loadedRoots = [];
  if (props.searchMethodRoot) {
    props.searchMethodRoot(0, props.pageSize).then((http: any) => {
      nodeList.value = [];
      updateTreeNodes(http);
      isSearching.value = false;
    });
  } else {
    props.searchMethod(undefined, 0, props.pageSize).then((http: any) => {
      nodeList.value = [];
      updateTreeNodes(http);
      isSearching.value = false;
    });
  }
}

function updateTreeNodes(http: any) {
  for (let i in http.response.result) {
    let soDTO = http.response.result[i];
    let soNode;
    if ("child_count" in soDTO && soDTO.child_count == 0) {
      soNode = {
        title: soDTO.name,
        data: soDTO,
        isLeaf: true,
        children: [],
        isExpanded: false,
        isSelected: false,
        isDraggable: false,
        isSelectable: true,
      };
    } else if (!("child_count" in soDTO)) {
      soNode = {
        title: soDTO.name,
        data: soDTO,
        isLeaf: true,
        isExpanded: false,
        isSelected: false,
        isDraggable: false,
        isSelectable: true,
      };
    } else {
      soNode = {
        title: soDTO.name,
        data: soDTO,
        isLeaf: false,
        children: [],
        isExpanded: false,
        isSelected: false,
        isDraggable: false,
        isSelectable: true,
      };
    }

    nodeList.value.push(soNode);
  }

  let count = http.response.metadata.pagination.totalCount;
  let isEmpty =
    !Array.isArray(http.response.result) || http.response.result.length == 0;
  if (!isEmpty && count > nodeList.value.length) {
    nodeList.value.push({
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

function loadMoreChildren(node: any) {
  let nodeURI = node.data.uri;

  let root = nodeList.value[node.path[0]];
  for (let i = 1; i < node.path.length; i++) {
    root = root.children[node.path[i]];
  }
  if (
    props.searchMethodRootChildren &&
    node.path.length == 1 &&
    loadedRoots.indexOf(root.data.uri) < 0
  ) {
    let page = Math.round(root.children.length / props.pageSize);
    props.searchMethodRootChildren(nodeURI, page, props.pageSize).then(
      (http: any) => {
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

        loadedRoots.push(root.data.uri);
      }
    );
  } else if (root.children.length < node.data.child_count) {
    let page = Math.round(root.children.length / props.pageSize);
    props.searchMethod(nodeURI, page, props.pageSize).then((http: any) => {
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

function selectMoreChildren(node: any, areSelected: boolean) {
  let nodeURI = node.data.uri;

  let root = nodeList.value[node.path[0]];
  for (let i = 1; i < node.path.length; i++) {
    root = root.children[node.path[i]];
  }
  if (
    props.searchMethodRootChildren &&
    node.path.length == 1 &&
    loadedRoots.indexOf(root.data.uri) < 0
  ) {
    let page = Math.round(root.children.length / props.pageSize);
    props.searchMethodRootChildren(nodeURI, page, props.pageSize).then(
      (http: any) => {
        console.debug("searchMethodRootChildren", http);

        for (let i in http.response.result) {
          let soDTO = http.response.result[i];

          if (areSelected === true) {
            selectElement(soDTO.uri);
          }
          if (areSelected === false) {
            deselectElement(soDTO.uri);
          }
        }
      }
    );
  } else if (root.children.length < node.data.child_count) {
    let page = Math.round(root.children.length / props.pageSize);
    props.searchMethod(nodeURI, page, props.pageSize).then((http: any) => {
      for (let i in http.response.result) {
        let soDTO = http.response.result[i];

        if (areSelected === true) {
          selectElement(soDTO.uri);
        }
        if (areSelected === false) {
          deselectElement(soDTO.uri);
        }
      }
    });
  }
}

function toggle(node: any) {
  if (
    !("child_count" in node.data) ||
    (node.children.length == 0 && node.data.child_count > 0)
  ) {
    loadMoreChildren(node);
  }
  emit("toggle", node);
}

function getSelection(uri: string) {
  let r = false;
  if (multiSelect.value) {
    r = multiSelect.value.indexOf(uri) >= 0;
  }
  return r;
}

function selectAllChildren(node: any) {
  selectMoreChildren(node, true);
  if (multiSelect.value) {
    console.debug("selectAllChildren", node.children);
    if (node.children.length > 0) {
      for (let nbChild in node.children) {
        console.debug("selectAllChild", node.children[nbChild]);
        if (node.children[nbChild].isLeaf) {
          selectElement(node.children[nbChild].data.uri);
        } else {
          selectAllChildren(node.children[nbChild]);
        }
      }
    }
  }
}

function deselectAllChildren(node: any) {
  selectMoreChildren(node, false);
  if (multiSelect.value) {
    console.debug("deselectAllChildren", node.children);
    if (node.children.length > 0) {
      for (let nbChild in node.children) {
        console.debug("deselectAllChild", node.children[nbChild]);
        if (node.children[nbChild].isLeaf) {
          deselectElement(node.children[nbChild].data.uri);
        } else {
          deselectElement(node.children[nbChild].data.uri);
          selectMoreChildren(node.children[nbChild], false);
          deselectAllChildren(node.children[nbChild]);
        }
      }
    }
  }
}

function selectElement(uri: string) {
  console.debug("uri: " + uri);
  if (multiSelect.value) {
    let uriIndex = multiSelect.value.indexOf(uri);
    console.debug("uriIndex to select: " + uriIndex);
    if (uriIndex == -1) {
      multiSelect.value.push(uri);
    }
    console.debug("multiSelect after select", multiSelect.value);
  }
}

function deselectElement(uri: string) {
  console.debug("uri: " + uri);
  if (multiSelect.value) {
    let uriIndex = multiSelect.value.indexOf(uri);
    console.debug("uriIndex to deselect: " + uriIndex);
    if (uriIndex !== -1) {
      multiSelect.value.splice(uriIndex, 1);
    }
    console.debug("multiSelect after deselect", multiSelect.value);
  }
}

function onSelectionChange(uri: string) {
  console.debug("uri: " + uri);
  if (multiSelect.value) {
    let uriIndex = multiSelect.value.indexOf(uri);
    console.debug("uriIndex", uriIndex);
    if (uriIndex >= 0) {
      multiSelect.value.splice(uriIndex, 1);
    } else {
      multiSelect.value.push(uri);
    }
    console.debug("multiSelect", multiSelect.value);
  }
}

defineExpose({
  nodeList,
  isSearching,
  copy,
  refresh,
  selectItem,
  getNodeParent,
  loadMoreChildren,
  selectMoreChildren,
  toggle,
  getSelection,
  selectAllChildren,
  deselectAllChildren,
  selectElement,
  deselectElement,
  onSelectionChange,
});
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
