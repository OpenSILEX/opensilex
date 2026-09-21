<template>
  <Overlay :show="isSearching && !isGlobalLoaderVisible">
    <n-tree
        class="async-tree"
        cascade
        :data="nodeList"
        key-field="key"
        label-field="title"
        block-line
        :expanded-keys="expandedKeys"
        @update:expanded-keys="(keys) => (expandedKeys = keys)"
        :on-load="onLoad"
        :render-label="renderLabel"
        :render-switcher-icon="renderSwitcherIcon"
    />
  </Overlay>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, onUpdated, ref, VNodeChild, h } from "vue";
import { useStore } from "vuex";
import { useI18n } from "vue-i18n";
import { NCheckbox, NTree } from "naive-ui";
import Icon from "@/components/common/views/Icon.vue";
import Overlay from "@/components/layout/Overlay.vue";

const store = useStore();
const { t } = useI18n();

const props = withDefaults(
    defineProps<{
      noButtons?: boolean;
      searchMethod?: Function;
      searchMethodRoot?: Function;
      searchMethodRootChildren?: Function;
      pageSize?: number;
      enableSelection?: boolean;
      selection?: string[];
    }>(),
    {
      noButtons: false,
      pageSize: 10,
      enableSelection: false,
    }
);

const emit = defineEmits<{
  (e: "select", node: any): void;
  (e: "update:selection", value: string[]): void;
}>();

const slots = defineSlots<{
  node: (props: { node: any }) => VNodeChild;
  buttons: (props: { node: any }) => VNodeChild;
}>();

const multiSelect = computed({
  get: () => props.selection ?? [],
  set: (value) => emit("update:selection", value),
});

const nodeList = ref<any[]>([]);
const expandedKeys = ref<string[]>([]);
const isSearching = ref(false);

const isGlobalLoaderVisible = computed(() => store.state.loaderVisible);

const ROOT_LOADING_NODE_KEY = "__root_loading_more__";

function buildNode(soDTO: any, isRoot: boolean): any {
  const hasChildCount = "child_count" in soDTO;
  return {
    key: soDTO.uri,
    title: soDTO.name,
    isLeaf: hasChildCount ? soDTO.child_count === 0 : true,
    data: soDTO,
    isRoot,
  };
}

function buildRootLoadingNode(): any {
  return {
    key: ROOT_LOADING_NODE_KEY,
    title: t("TreeViewAsync.loading-more"),
    isLeaf: true,
    data: null,
  };
}

let rootPage = 0;

function appendRootNodes(nodes: any[], totalCount: number) {
  nodeList.value = nodeList.value.concat(nodes);
  if (nodeList.value.length < totalCount) {
    nodeList.value.push(buildRootLoadingNode());
  }
}

async function refresh() {
  isSearching.value = true;
  rootPage = 0;
  const method = props.searchMethodRoot ?? props.searchMethod;
  const http = await method(undefined, 0, props.pageSize);
  nodeList.value = [];
  appendRootNodes((http.response.result || []).map((dto: any) => buildNode(dto, true)), http.response.metadata.pagination.totalCount);
  isSearching.value = false;
}

async function loadMoreRoots() {
  rootPage += 1;
  const method = props.searchMethodRoot ?? props.searchMethod;
  const http = await method(undefined, rootPage, props.pageSize);
  nodeList.value.pop(); // remove the loading sentinel
  appendRootNodes((http.response.result || []).map((dto: any) => buildNode(dto, true)), http.response.metadata.pagination.totalCount);
}

async function onLoad(node: any) {
  const method = node.isRoot && props.searchMethodRootChildren ? props.searchMethodRootChildren : props.searchMethod;
  const http = await method(node.data.uri, 0, props.pageSize);
  node.children = (http.response.result || []).map((dto: any) => buildNode(dto, false));
}

const loadingMoreKeys = new Set<string>();

async function loadMoreChildren(node: any) {
  if (loadingMoreKeys.has(node.key)) {
    return;
  }
  loadingMoreKeys.add(node.key);
  try {
    const page = Math.round((node.children?.length || 0) / props.pageSize);
    const http = await props.searchMethod(node.data.uri, page, props.pageSize);
    const newChildren = (http.response.result || []).map((dto: any) => buildNode(dto, false));
    node.children = [...(node.children || []), ...newChildren];
  } finally {
    loadingMoreKeys.delete(node.key);
  }
}

function getSelection(uri: string): boolean {
  return multiSelect.value.indexOf(uri) >= 0;
}

function onSelectionChange(uri: string) {
  const current = multiSelect.value;
  const index = current.indexOf(uri);
  if (index >= 0) {
    emit("update:selection", [...current.slice(0, index), ...current.slice(index + 1)]);
  } else {
    emit("update:selection", [...current, uri]);
  }
}

function renderSwitcherIcon(info: { expanded: boolean }): VNodeChild {
  return h(Icon, { icon: info.expanded ? "fa#chevron-down" : "fa#chevron-right" });
}

let rootLoadingSentinelEl: Element | null = null;
let observer: IntersectionObserver;

onMounted(() => {
  observer = new IntersectionObserver(([entry]) => {
    if (entry.isIntersecting) {
      loadMoreRoots();
    }
  });
  refresh();
});

onUpdated(() => {
  nextTick(() => {
    observer.disconnect();
    if (rootLoadingSentinelEl) {
      observer.observe(rootLoadingSentinelEl);
    }
  });
});

onBeforeUnmount(() => {
  observer.disconnect();
});

function renderLabel(info: { option: any }): VNodeChild {
  const node = info.option;

  if (node.data == null) {
    return h(
        "span",
        { ref: (el: Element | null) => (rootLoadingSentinelEl = el) },
        t("TreeViewAsync.loading-more")
    );
  }

  const childCount = node.data?.child_count;
  const loadedCount = Array.isArray(node.children) ? node.children.length : 0;
  const hasMoreChildren = typeof childCount === "number" && childCount > loadedCount;

  // The whole node line selects the object. Controls that have their own action
  // (checkbox, "load more", action buttons) stop the propagation.
  return h("span", { class: "async-tree-node", onClick: () => emit("select", node) }, [
    props.enableSelection
        ? h("span", { onClick: (e: Event) => e.stopPropagation() }, [
          h(NCheckbox, {
            class: "selection-box",
            checked: getSelection(node.data.uri),
            "onUpdate:checked": () => onSelectionChange(node.data.uri),
          }),
        ])
        : null,
    h(
        "span",
        { class: "async-tree-title" },
        [slots.node ? slots.node({ node }) : node.title]
    ),
    typeof childCount === "number"
        ? h(
            "span",
            { class: "async-tree-action" },
            hasMoreChildren
                ? [
                  ` (${node.data.rdf_type_name} - ${loadedCount}/${childCount} - `,
                  h(
                      "a",
                      {
                        href: "#",
                        onClick: (e: Event) => {
                          e.preventDefault();
                          e.stopPropagation();
                          loadMoreChildren(node);
                        },
                      },
                      t("TreeViewAsync.load-more")
                  ),
                  ")",
                ]
                : ` (${node.data.rdf_type_name})`
        )
        : null,
    !props.noButtons && slots.buttons
        ? h(
            "span",
            { class: "tree-button-group", onClick: (e: Event) => e.stopPropagation() },
            [slots.buttons({ node })]
        )
        : null,
  ]);
}

defineExpose({
  nodeList,
  isSearching,
  refresh,
});
</script>

<style scoped lang="scss">
:deep(.async-tree) {
  border: 1px solid #dee2e6;
  background-color: white;
  color: #545454;
  max-height: 300px;
  overflow-y: auto;
}

:deep(.n-tree-node-content) {
  padding-right: 0.3rem;
  padding-left: 0.3rem;
}

:deep(.n-tree-node-content:hover) {
  color: #545454;
  cursor: pointer;
  background-color: #f3f3f3;
}

:deep(.n-tree-node--selected .n-tree-node-content) {
  color: #545454;
  background-color: #e9e9e9;
}

.selection-box {
  display: inline-block;
  margin-right: 6px;
}

.async-tree-node {
  /* Fills the node line so the whole row, not only the label, selects the object */
  display: block;
  cursor: pointer;
}

.async-tree-action {
  font-style: italic;
}

.async-tree-action a:hover {
  text-decoration: underline;
  cursor: pointer;
}

.tree-button-group {
  float: right;
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
