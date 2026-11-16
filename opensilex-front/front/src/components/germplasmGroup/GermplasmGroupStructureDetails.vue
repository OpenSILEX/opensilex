<template>
  <div v-if="props.selected?.uri" class="card">
    <div class="card-header">
      <h3 class="mb-0 d-flex align-items-center">
        <Icon icon="bi#bi-clipboard" class="me-2" />
        {{ t('component.common.details-label') }}
      </h3>
    </div>

    <div class="card-body">
      <UriView :uri="props.selected.uri" />

      <TextView
        label="component.common.name"
        :value="props.selected.name"
      />

      <TextView
        label="component.common.description"
        :value="props.selected.description"
      />

      <MetadataView
        v-if="props.selected.publisher?.uri"
        :publisher="props.selected.publisher"
        :publication-date="props.selected.publication_date"
        :last-updated-date="props.selected.last_updated_date"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import type { GermplasmGroupGetDTO } from 'opensilex-core/index';
import Icon from '@/components/common/views/Icon.vue';
import MetadataView from '@/components/common/views/MetadataView.vue';
import TextView from '@/components/common/views/TextView.vue';
import UriView from '@/components/common/views/UriView.vue';

const { t } = useI18n();

const props = defineProps<{
  selected?: GermplasmGroupGetDTO | null;
}>();
</script>
