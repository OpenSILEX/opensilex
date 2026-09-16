<template>
  <Overlay :show="isSearching && !isGlobalLoaderVisible">
    <n-tree
        :data="nodeList"
        key-field="key"
        label-field="title"
        block-line
        :checkable="enableSelection"
        v-model:checked-keys="multiSelect"
        v-model:expanded-keys="expandedKeys"
        :on-load="onLoad"
        :render-label="renderLabel"
        :render-switcher-icon="renderSwitcherIcon"
    />
    <div v-if="hasMoreRoots" class="async-tree-action root-load-more">
      <a href="#" @click.prevent="loadMoreRoots()">
        {{ isLoadingMoreRoots ? t("TreeViewAsync.loading-more") : t("TreeViewAsync.load-more") }}
      </a>
    </div>
  </Overlay>
</template>

<script setup lang="ts">
import { computed, ref, VNodeChild, h } from "vue";
import { useStore } from "vuex";
import { useI18n } from "vue-i18n";
import { NTree } from "naive-ui";
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

const rootPage = ref(0);
const rootTotalCount = ref(0);
const isLoadingMoreRoots = ref(false);
const hasMoreRoots = computed(() => nodeList.value.length < rootTotalCount.value);

const isGlobalLoaderVisible = computed(() => store.state.loaderVisible);

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

async function refresh() {
  isSearching.value = true;
  rootPage.value = 0;
  const method = props.searchMethodRoot ?? props.searchMethod;
  const http = await method(undefined, 0, props.pageSize);
  nodeList.value = (http.response.result || []).map((dto: any) => buildNode(dto, true));
  rootTotalCount.value = http.response.metadata.pagination.totalCount;
  isSearching.value = false;
}

async function loadMoreRoots() {
  if (isLoadingMoreRoots.value) {
    return;
  }
  isLoadingMoreRoots.value = true;
  try {
    rootPage.value += 1;
    const method = props.searchMethodRoot ?? props.searchMethod;
    const http = await method(undefined, rootPage.value, props.pageSize);
    const newNodes = (http.response.result || []).map((dto: any) => buildNode(dto, true));
    nodeList.value = [...nodeList.value, ...newNodes];
  } finally {
    isLoadingMoreRoots.value = false;
  }
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

function renderSwitcherIcon(info: { expanded: boolean }): VNodeChild {
  return h(Icon, { icon: info.expanded ? "fa#chevron-down" : "fa#chevron-right" });
}

function renderLabel(info: { option: any }): VNodeChild {
  const node = info.option;
  const childCount = node.data?.child_count;
  const loadedCount = Array.isArray(node.children) ? node.children.length : 0;
  const hasMoreChildren = typeof childCount === "number" && childCount > loadedCount;

  return h("div", { class: "d-flex align-items-center async-tree-node" }, [
    h(
        "span",
        { class: "async-tree-title", onClick: () => emit("select", node) },
        [slots.node ? slots.node({ node }) : node.title]
    ),
    typeof childCount === "number"
        ? h(
            "span",
            { class: "async-tree-action" },
            hasMoreChildren
                ? [
                  ` (${node.data.rdf_type_name} - ${loadedCount}/${childCount} - `,
                  h(
                      "a",
                      {
                        href: "#",
                        onClick: (e: Event) => {
                          e.preventDefault();
                          loadMoreChildren(node);
                        },
                      },
                      t("TreeViewAsync.load-more")
                  ),
                  ")",
                ]
                : ` (${node.data.rdf_type_name})`
        )
        : null,
    !props.noButtons && slots.buttons
        ? h("span", { class: "tree-button-group" }, [slots.buttons({ node })])
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
.async-tree-title {
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
  margin-left: 8px;
}

.root-load-more {
  padding: 5px 10px;
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
