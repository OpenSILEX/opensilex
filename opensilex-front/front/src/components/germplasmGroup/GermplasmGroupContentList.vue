<template>
  <Card
    :label="t('GermplasmGroupView.type')"
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

interface TableSearchOptions {
  orderBy?: string[];
  currentPage: number;
  pageSize: number;
}

const props = defineProps<{
  selectedUri: string;
}>();

watch(
  () => props.selectedUri,
  () => refresh()
)

const { t } = useI18n();
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const service = opensilex.getService<GermplasmService>('opensilex.GermplasmService');
const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef');

const fields = computed(() => [
  {
    key: 'name',
    label: t('component.common.name'),
    sortable: true,
  },
]);

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
</script>

<i18n>
en:
  GermplasmGroupView:
    type: Germplasm
fr:
  GermplasmGroupView:
    type: Ressources génétiques
</i18n>
