<template>
  <Card
    :label="t('component.germplasms.title')"
    icon="fa#seedling"
    :noFooter="true"
  >
    <template #body>
      <TableAsyncView
        ref="tableRef"
        :searchMethod="searchGermplasmGroupContent"
        :fields="fields"
        :isSelectable="false"
        :defaultPageSize="5"
        defaultSortBy="name"
      >
        <template #cell(name)="{ data }">
          <UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :to="{
              path: `/germplasm/details/${encodeURIComponent(data.item.uri)}`,
            }"
          />
        </template>
      </TableAsyncView>
    </template>
  </Card>
</template>

<script setup lang="ts">
import { computed, inject, useTemplateRef, watch  } from 'vue';
import { useI18n } from 'vue-i18n';
import type { GermplasmService } from 'opensilex-core/api/germplasm.service';
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import Card from '@/components/common/views/Card.vue';
import TableAsyncView from '@/components/common/views/TableAsyncView.vue';
import UriLink from '@/components/common/views/UriLink.vue';

//#region Public
const props = defineProps<{
  selectedUri: string;
}>();
//#endregion

//#region Private
interface TableSearchOptions {
  orderBy?: string[];
  currentPage: number;
  pageSize: number;
}
//#region Hooks
watch(
  () => props.selectedUri,
  () => refresh()
)
//#endregion

//#region Plugins and services
const { t } = useI18n();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const service = opensilex.getService<GermplasmService>('opensilex.GermplasmService');
//#endregion

//#region Template refs
const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef');
//#endregion

//#region Data and computed
const fields = computed(() => [
  {
    key: 'name',
    label: t('component.common.name'),
    sortable: true,
  },
]);
//#endregion

//#region Methods
function searchGermplasmGroupContent(options: TableSearchOptions) {
  return service.getGermplasmGroupContent(
    props.selectedUri,
    options.orderBy,
    options.currentPage,
    options.pageSize,
  );
}

function refresh() {
  tableRef.value?.refresh();
}

defineExpose({
  refresh,
});
//#endregion
//#endregion
</script>
