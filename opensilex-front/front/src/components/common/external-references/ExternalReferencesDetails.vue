<template>
  <div>
    <n-data-table
      v-if="relations.length"
      :columns="columns"
      :data="relations"
      :pagination="false"
      size="small"
      class="rounded-md shadow"
      bordered
    />
    <p v-else>
      <strong>{{ t('component.skos.no-external-links-provided') }}</strong>
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed, h} from 'vue';
import { useI18n } from 'vue-i18n';
import { NDataTable } from 'naive-ui';
import SUPPORTED_SKOS_RELATIONS from '../../../models/SkosRelations';

const props = defineProps<{
  skosReferences: Record<string, string[]>;
}>();

const { t } = useI18n();




const relations = computed(() => {
  const allRelations: { relation: string; relationURI: string }[] = [];

  const skosArray = Array.from(SUPPORTED_SKOS_RELATIONS);

for (const skosRelation of skosArray) {
  const references = props.skosReferences?.[skosRelation.dtoKey];

  if (references) {
    for (const uri of references) {
      allRelations.push({
        relation: t(skosRelation.dtoKey),
        relationURI: uri,
      });
    }
  }
}

  return allRelations;
});

const columns = [
  {
    title: t('component.skos.relation'),
    key: 'relation',
    sorter: 'default',
  },
  {
    title: t('component.skos.uri'),
    key: 'relationURI',
    render(row: any) {
      return h('a', { href: row.relationURI, target: '_blank' }, row.relationURI);
    },
  },
];
</script>
