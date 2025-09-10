<template>
  <div v-if="selected && selected.uri" class="card">
    <div class="card-header">
      <h3 class="mb-0 d-flex align-items-center">
        <opensilex-Icon icon="bi#bi-clipboard" class="me-2" />
        {{ $t("component.common.details-label") }}
      </h3>
    </div>
    <div class="card-body">
      <!-- URI -->
      <opensilex-UriView :uri="selected.uri" />

      <!-- Name -->
      <opensilex-TextView label="component.common.name" :value="selected.name" />

      <!-- Description -->
      <opensilex-TextView v-if="selected.description" :value="selected.description" label="component.common.description" />

      <!-- Metadata -->
      <opensilex-MetadataView
        v-if="selected.publisher && selected.publisher.uri"
        :publisher="selected.publisher"
        :publicationDate="selected.publication_date"
        :lastUpdatedDate="selected.last_updated_date"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineProps } from 'vue';

const props = defineProps<{
  selected: {
    uri?: string;
    name?: string;
    description?: string;
    publisher?: { uri?: string };
    publication_date?: string;
    last_updated_date?: string;
  };
}>();
</script>

<style scoped lang="scss">
</style>
