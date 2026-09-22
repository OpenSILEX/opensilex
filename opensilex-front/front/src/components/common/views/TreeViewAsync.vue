```vue
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
        :selected-keys="selectedKeys"
        @update:expanded-keys="(keys) => (expandedKeys = keys)"
        @update:selected-keys="handleSelectedKeys"
        :on-load="onLoad"
        :render-label="renderLabel"
        :render-switcher-icon="renderSwitcherIcon"
    />
  </Overlay>
</template>

<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  onUpdated,
  ref,
  VNodeChild,
  h,
} from "vue";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {NCheckbox, NTree} from "naive-ui";

import Icon from "@/components/common/views/Icon.vue";
import Overlay from "@/components/layout/Overlay.vue";

const store = useStore();
const {t} = useI18n();

const props = withDefaults(
    defineProps<{
      noButtons?: boolean;
      searchMethod?: Function;
      searchMethodRoot?: Function;
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

/**
 * Sélection des checkboxes.
 */
const multiSelect = computed({
  get: () => props.selection ?? [],
  set: (value) => emit("update:selection", value),
});

/**
 * Sélection interne du NTree.
 *
 * Cette sélection sert uniquement à permettre à NTree
 * de gérer le clic sur toute la ligne.
 */
const selectedKeys = ref<string[]>([]);

const nodeList = ref<any[]>([]);
const expandedKeys = ref<string[]>([]);
const isSearching = ref(false);

const isGlobalLoaderVisible = computed(
    () => store.state.loaderVisible
);

const ROOT_LOADING_NODE_KEY = "__root_loading_more__";

const loadingMoreKeys = new Set<string>();

function buildNode(soDTO: any, isRoot: boolean): any {
  const hasChildCount = "child_count" in soDTO;

  return {
    key: soDTO.uri,
    title: soDTO.name,

    // Sans child_count, on considère que le nœud peut avoir des enfants.
    isLeaf: hasChildCount
        ? soDTO.child_count === 0
        : false,

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

function appendRootNodes(
    nodes: any[],
    totalCount: number
) {
  nodeList.value = nodeList.value.concat(nodes);

  if (nodeList.value.length < totalCount) {
    nodeList.value.push(
        buildRootLoadingNode()
    );
  }
}

/**
 * Rafraîchit l'arbre.
 */
async function refresh() {
  isSearching.value = true;
  rootPage = 0;

  // Les anciens nœuds sélectionnés ne doivent pas rester actifs.
  selectedKeys.value = [];

  // Les anciennes expansions deviennent invalides
  // lorsque les résultats changent.
  expandedKeys.value = [];

  loadingMoreKeys.clear();

  try {
    const method =
        props.searchMethodRoot ??
        props.searchMethod;

    const http = await method(
        undefined,
        0,
        props.pageSize
    );

    nodeList.value = [];

    appendRootNodes(
        (http.response.result || []).map(
            (dto: any) => buildNode(dto, true)
        ),
        http.response.metadata.pagination.totalCount
    );
  } finally {
    isSearching.value = false;
  }
}

let isLoadingMoreRoots = false;

/**
 * Charge la page suivante des nœuds racine.
 */
async function loadMoreRoots() {
  if (isLoadingMoreRoots) {
    return;
  }

  isLoadingMoreRoots = true;

  try {
    rootPage += 1;

    const method =
        props.searchMethodRoot ??
        props.searchMethod;

    const http = await method(
        undefined,
        rootPage,
        props.pageSize
    );

    if (
        nodeList.value[
        nodeList.value.length - 1
            ]?.key === ROOT_LOADING_NODE_KEY
    ) {
      nodeList.value.pop();
    }

    appendRootNodes(
        (http.response.result || []).map(
            (dto: any) => buildNode(dto, true)
        ),
        http.response.metadata.pagination.totalCount
    );
  } finally {
    isLoadingMoreRoots = false;
  }
}

/**
 * Charge les enfants directs d'un nœud.
 */
async function onLoad(node: any) {
  const http = await props.searchMethod(
      node.data.uri,
      0,
      props.pageSize
  );

  const children =
      (http.response.result || []).map(
          (dto: any) => buildNode(dto, false)
      );

  node.data.child_count =
      http.response.metadata.pagination.totalCount;

  node.children = children;

  node.isLeaf =
      children.length === 0;
}

/**
 * Recherche récursive d'un nœud.
 */
function findNode(
    key: string,
    nodes: any[] = nodeList.value
): any | undefined {
  for (const node of nodes) {
    if (node.key === key) {
      return node;
    }

    if (Array.isArray(node.children)) {
      const found = findNode(
          key,
          node.children
      );

      if (found) {
        return found;
      }
    }
  }

  return undefined;
}

/**
 * Recharge les enfants d'un nœud.
 */
async function reloadNodeChildren(
    key: string
) {
  const node = findNode(key);

  if (!node) {
    await refresh();
    return;
  }

  const http = await props.searchMethod(
      node.data.uri,
      0,
      props.pageSize
  );

  const children =
      (http.response.result || []).map(
          (dto: any) => buildNode(dto, false)
      );

  node.data.child_count =
      http.response.metadata.pagination.totalCount;

  node.children = children;

  node.isLeaf =
      children.length === 0;

  if (
      !node.isLeaf &&
      !expandedKeys.value.includes(key)
  ) {
    expandedKeys.value = [
      ...expandedKeys.value,
      key,
    ];
  }
}

/**
 * Charge davantage d'enfants.
 */
async function loadMoreChildren(
    node: any
) {
  if (loadingMoreKeys.has(node.key)) {
    return;
  }

  loadingMoreKeys.add(node.key);

  try {
    const page = Math.floor(
        (node.children?.length || 0) /
        props.pageSize
    );

    const http = await props.searchMethod(
        node.data.uri,
        page,
        props.pageSize
    );

    const newChildren =
        (http.response.result || []).map(
            (dto: any) => buildNode(dto, false)
        );

    node.children = [
      ...(node.children || []),
      ...newChildren,
    ];
  } finally {
    loadingMoreKeys.delete(node.key);
  }
}

/**
 * Vérifie si une URI est sélectionnée
 * via les checkbox.
 */
function getSelection(
    uri: string
): boolean {
  return (
      multiSelect.value.indexOf(uri) >= 0
  );
}

/**
 * Modifie la sélection des checkbox.
 */
function onSelectionChange(
    uri: string
) {
  const current = multiSelect.value;

  const index =
      current.indexOf(uri);

  if (index >= 0) {
    emit(
        "update:selection",
        [
          ...current.slice(0, index),
          ...current.slice(index + 1),
        ]
    );
  } else {
    emit(
        "update:selection",
        [...current, uri]
    );
  }
}

/**
 * Clic/sélection d'une ligne entière.
 *
 * C'est maintenant NTree qui déclenche cet événement.
 * On ne dépend plus du clic sur le texte du label.
 */
function handleSelectedKeys(
    keys: string[]
) {
  selectedKeys.value = keys;

  const key = keys[0];

  if (
      !key ||
      key === ROOT_LOADING_NODE_KEY
  ) {
    return;
  }

  const node = findNode(key);

  if (node) {
    emit("select", node);
  }
}

function renderSwitcherIcon(
    info: { expanded: boolean }
): VNodeChild {
  return h(Icon, {
    icon: info.expanded
        ? "fa#chevron-down"
        : "fa#chevron-right",
  });
}

let rootLoadingSentinelEl: Element | null =
    null;

let observer: IntersectionObserver;

onMounted(() => {
  observer =
      new IntersectionObserver(
          ([entry]) => {
            if (entry.isIntersecting) {
              loadMoreRoots();
            }
          }
      );

  refresh();
});

onUpdated(() => {
  nextTick(() => {
    observer.disconnect();

    if (rootLoadingSentinelEl) {
      observer.observe(
          rootLoadingSentinelEl
      );
    }
  });
});

onBeforeUnmount(() => {
  observer?.disconnect();
});

/**
 * Génère uniquement le contenu visuel de la ligne.
 *
 * IMPORTANT :
 * Il n'y a plus de onClick ici.
 * Le clic sur la ligne est maintenant géré par NTree.
 */
function renderLabel(
    info: { option: any }
): VNodeChild {
  const node = info.option;

  /**
   * Sentinel "load more" des racines.
   */
  if (node.data == null) {
    return h(
        "span",
        {
          ref: (
              el: Element | null
          ) => {
            rootLoadingSentinelEl = el;
          },
        },
        t(
            "TreeViewAsync.loading-more"
        )
    );
  }

  const childCount =
      node.data?.child_count;

  const childrenLoaded =
      Array.isArray(node.children);

  const loadedCount =
      childrenLoaded
          ? node.children.length
          : 0;

  const hasMoreChildren =
      childrenLoaded &&
      typeof childCount === "number" &&
      childCount > loadedCount;

  return h(
      "span",
      {
        class: "async-tree-node",
      },
      [
        /**
         * Checkbox.
         *
         * Elle arrête la propagation afin de ne pas
         * déclencher le clic de sélection du nœud.
         */
        props.enableSelection
            ? h(
                "span",
                {
                  class:
                      "async-tree-checkbox",
                  onClick: (
                      e: Event
                  ) =>
                      e.stopPropagation(),
                },
                [
                  h(NCheckbox, {
                    class:
                        "selection-box",

                    checked:
                        getSelection(
                            node.data.uri
                        ),

                    "onUpdate:checked":
                        () =>
                            onSelectionChange(
                                node.data.uri
                            ),
                  }),
                ]
            )
            : null,

        /**
         * Titre.
         */
        h(
            "span",
            {
              class:
                  "async-tree-title",
            },
            [
              slots.node
                  ? slots.node({node})
                  : node.title,
            ]
        ),

        /**
         * Informations complémentaires.
         */
        h(
            "span",
            {
              class:
                  "async-tree-action",
            },
            hasMoreChildren
                ? [
                  ` (${node.data.rdf_type_name} - ${loadedCount}/${childCount} - `,

                  h(
                      "a",
                      {
                        href: "#",

                        onClick: (
                            e: Event
                        ) => {
                          e.preventDefault();
                          e.stopPropagation();

                          loadMoreChildren(
                              node
                          );
                        },
                      },

                      t(
                          "TreeViewAsync.load-more"
                      )
                  ),

                  ")",
                ]
                : ` (${node.data.rdf_type_name})`
        ),

        /**
         * Boutons d'action.
         */
        !props.noButtons &&
        slots.buttons
            ? h(
                "span",
                {
                  class:
                      "tree-button-group",

                  onClick: (
                      e: Event
                  ) =>
                      e.stopPropagation(),
                },
                [
                  slots.buttons({
                    node,
                  }),
                ]
            )
            : null,
      ]
  );
}

defineExpose({
  nodeList,
  isSearching,
  refresh,
  reloadNodeChildren,
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

/*
 * NTree doit occuper toute la largeur.
 */
:deep(.n-tree-node-content) {
  width: 100%;
  padding-right: 0.3rem;
  padding-left: 0.3rem;
}

/*
 * Le contenu du label prend tout l'espace
 * disponible dans la ligne.
 */
:deep(.n-tree-node-content__text) {
  flex: 1 1 auto;
  min-width: 0;
  width: 100%;
}

/*
 * Notre contenu de label prend toute la largeur
 * qui lui est donnée par NTree.
 */
.async-tree-node {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  cursor: pointer;
}

/*
 * Titre flexible.
 */
.async-tree-title {
  min-width: 0;
}

/*
 * Partie informative.
 */
.async-tree-action {
  font-style: italic;
}

/*
 * Checkbox.
 */
.async-tree-checkbox {
  flex: 0 0 auto;
}

.selection-box {
  display: inline-block;
  margin-right: 6px;
}

/*
 * Boutons à droite.
 */
.tree-button-group {
  flex: 0 0 auto;
  margin-left: auto;
}

/*
 * Hover de toute la ligne.
 */
:deep(.n-tree-node-content:hover) {
  color: #545454;
  cursor: pointer;
  background-color: #f3f3f3;
}

/*
 * Ligne sélectionnée.
 */
:deep(.n-tree-node--selected .n-tree-node-content) {
  color: #545454;
  background-color: #e9e9e9;
}

/*
 * Lien "Load more".
 */
.async-tree-action a:hover {
  text-decoration: underline;
  cursor: pointer;
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
```
