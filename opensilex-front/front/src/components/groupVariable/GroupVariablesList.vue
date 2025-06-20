<template>
    <opensilex-StringFilter
    v-model:filter="nameFilter"
    @update="updateFilters"
    :placeholder="t('GroupVariableList.label-filter-placeholder')"
    />
    <opensilex-PageContent>

<opensilex-TableAsyncView
  ref="tableRef"
  :searchMethod="searchVariablesGroups"
  :fields="fields"
  defaultSortBy="name"
  :isSelectable="isSelectable"
  :maximumSelectedRows="maximumSelectedRows"
  :labelNumberOfSelectedRow="t('GroupVariableList.selected')"
  :iconNumberOfSelectedRow="iconNumberOfSelectedRow"
  :showCount="true"
>
  <template #cell(name)="slotProps">
    {{ slotProps?.data?.item?.name ?? '' }}
  </template>

  <template #cell(description)="slotProps">
    {{ slotProps?.data?.item?.description ?? '' }}
  </template>
</opensilex-TableAsyncView>


    </opensilex-PageContent>
  
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, inject } from 'vue';
import { useRoute } from 'vue-router';
import { useStore } from 'vuex';
import { VariablesService } from 'opensilex-core/index';
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin"
import { useI18n } from 'vue-i18n';

const { t, n } = useI18n();

const props = defineProps<{
  isSelectable: { type: Boolean, default: true }
  noActions?: boolean;
  maximumSelectedRows?: number;
  iconNumberOfSelectedRow?: string;
}>();

const isSelectable = computed(() => props.isSelectable ?? true);
const noActions = computed(() => props.noActions ?? true);

const nameFilter = ref('');
const variables = ref<any>(null);

const tableRef = ref();
const route = useRoute();
const store = useStore();
const opensilex = inject<OpenSilexVuePlugin>('opensilex');

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

const service = ref<VariablesService>();

onMounted(() => {
  const query: any = route.query;
  if (query.name) {
    nameFilter.value = decodeURIComponent(query.name);
  }

 if (opensilex) {
    opensilex.disableLoader();
    service.value = opensilex.getService('opensilex.VariablesService');
  }
  
});

function searchVariablesGroups(options: any) {
  if (!service.value) {
    return Promise.resolve({ results: [], total: 0 });
  }

  const name = nameFilter.value || undefined;
  const variableUri = variables.value || undefined;
  const orderBy = options?.orderBy || undefined;
  const page = options?.currentPage ?? 0;
  const pageSize = options?.pageSize ?? 10;

  return service.value.searchVariablesGroups(
    name,
    variableUri,
    orderBy,
    page,
    pageSize,
    undefined // sharedResourceInstance
  );
}


function updateFilters() {
  if (opensilex) {
    opensilex.updateURLParameter('name', nameFilter.value, '');
  }
  refresh();
}

function resetSearch() {
  nameFilter.value = '';
  if (opensilex) {
    opensilex.updateURLParameter('name', undefined, undefined);
  }
  refresh();
}

function refresh() {
  if (tableRef.value) {
    console.log("tableref.value ", tableRef.value)
    tableRef.value.refresh();
  }
}

function getSelected() {
  return tableRef.value?.getSelected() ?? [];
}

function onItemSelected(row: any) {
  tableRef.value?.onItemSelected(row);
}

function onItemUnselected(row: any) {
  tableRef.value?.onItemUnselected(row);
}

const fields = computed(() => [
  {
    key: 'name',
    label: t('component.common.name'),
    sortable: true,
  },
  {
    key: 'description',
    label: t('component.common.description'),
    sortable: false,
  },
]);

defineExpose({
  refresh,
  getSelected,
  onItemSelected,
  onItemUnselected,
});

</script>

<i18n>
en:
    GroupVariableList:
        label-filter: Search variables group
        label-filter-placeholder: "Search variables groups."
        selected: Selected variables groups
fr:
    GroupVariableList:
        label-filter: Chercher un groupe de variables
        label-filter-placeholder: "Rechercher des groupes de variables."
        selected: Groupes de variables sélectionnés
</i18n>

