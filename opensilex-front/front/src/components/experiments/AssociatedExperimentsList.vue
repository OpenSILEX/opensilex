<template>
  <opensilex-Card :label="t('AssociatedExperimentsList.relatedExperiments')" icon="bi-layers" :no-footer="true">
    <template #body>
      <opensilex-StringFilter
        v-model:filter="filter"
        @update="updateFilters"
        :debounce="300"
        :lazy="false"
        :placeholder="t('AssociatedExperimentsList.experimentNameFilter')"
      />

      <opensilex-TableAsyncView
        ref="tableRef"
        :searchMethod="searchMethod"
        :fields="fields"
        defaultSortBy="name"
        :defaultPageSize="5"
      >
        <template #cell(name)="{ data }">
          <opensilex-UriLink
            :uri="data.item.uri"
            :value="data.item.name"
            :to="{ path: '/experiment/details/' + encodeURIComponent(data.item.uri) }"
            :allowCopy="true"
          />
        </template>

        <template #cell(start_date)="{ data }">
          <opensilex-DateView :value="data.item.start_date" />
        </template>

        <template #cell(end_date)="{ data }">
          <opensilex-DateView :value="data.item.end_date" />
        </template>

        <template #cell(species)="{ data }">
          <span class="species-list" v-if="(data.item.species ?? []).length > 0">
            <span v-for="(uri, index) in data.item.species" :key="uri ?? index">
              <span :title="uri">{{ getSpeciesName(uri) }}</span>
              <span v-if="index + 1 < data.item.species.length">, </span>
            </span>
          </span>
          <span v-else></span>
        </template>

        <template #cell(state)="{ data }">
          <opensilex-Icon
            v-if="!isEnded(data.item)"
            icon="bi#bi-activity"
            :title="t('component.experiment.common.status.in-progress')"
            class="badge-icon badge-pill badge-info-phis p-1"
          />

          <opensilex-Icon
            v-else
            icon="bi#bi-archive"
            :title="t('component.experiment.common.status.finished')"
            class="badge-icon badge-pill p-1"
          />

          <opensilex-Icon
            v-if="data.item.is_public"
            icon="bi#bi-people"
            :title="t('component.experiment.common.status.public')"
            class="badge-icon badge-info p-1"
          />
        </template>
      </opensilex-TableAsyncView>
    </template>
  </opensilex-Card>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

import type { SpeciesService } from 'opensilex-core/api/species.service'
import type HttpResponse from 'opensilex-core/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-core/HttpResponse'
import type { SpeciesDTO } from 'opensilex-core/index'

/** Props */
const props = defineProps<{
  searchMethod: (options: any) => Promise<any>
  nameFilter?: string
}>()

/** Emits (v-model:nameFilter) */
const emit = defineEmits<{
  (e: 'update:nameFilter', v: string): void
}>()

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

/** Table ref */
const tableRef = ref<any>(null)

/** v-model proxy */
const filter = computed<string>({
  get: () => props.nameFilter ?? '',
  set: (v) => emit('update:nameFilter', v)
})

/** Fields */
const fields = [
  { key: 'name', label: 'component.common.name' },
  { key: 'species', label: 'component.common.species' },
  { key: 'start_date', label: 'component.common.date-time-stuff.startDate', sortable: true },
  { key: 'end_date', label: 'component.common.date-time-stuff.endDate', sortable: true },
  { key: 'state', label: 'component.common.state' }
] as const

/** Species cache */
const speciesByUri = ref<Map<string, SpeciesDTO>>(new Map())

async function loadSpecies() {
  try {
    const service = opensilex.getService<SpeciesService>('opensilex.SpeciesService')

    const http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>> =
      await service.getAllSpecies()

    const map = new Map<string, SpeciesDTO>()
    for (const sp of http.response.result ?? []) {
      map.set(sp.uri, sp)
    }
    speciesByUri.value = map

    // refresh table once species names are ready
    tableRef.value?.refresh?.()
  } catch (e) {
    opensilex.errorHandler(e)
  }
}

function getSpeciesName(uri: string) {
  return speciesByUri.value.get(uri)?.name ?? ''
}

function isEnded(experiment: any) {
  if (experiment?.end_date) return new Date(experiment.end_date).getTime() < Date.now()
  return false
}

function updateFilters() {
  tableRef.value?.refresh?.()
}

onMounted(() => {
  loadSpecies()
})
</script>

<style scoped lang="scss">
.species-list {
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  display: inline-block;
  max-width: 150px;
}
</style>

<i18n>
en:
  AssociatedExperimentsList:
    experimentNameFilter: Search on experiment name
    relatedExperiments: Related Experiments

fr:
  AssociatedExperimentsList:
    experimentNameFilter: Chercher sur le nom de l'expérimentation
    relatedExperiments: Expérimentations connexes
</i18n>
