<template>
  <div class="col-md-3 col-sm-4 cardContainer">
    <n-card style="max-width: 20rem" content-style="padding: 0;">
      <img
        :src="src"
        class="card-img-top"
        @click="showImage"
      />

      <div class="card-body">
        <p class="card-text">
          <span class="imageAttribut">Date:</span> {{ date }}
        </p>

        <p class="card-text">
          <span class="imageAttribut">Type:</span> {{ type }}
        </p>

        <p class="card-text">
          <span class="imageAttribut">{{ t('ImageSingle.target') }}</span> :
          {{ image.target }}
        </p>

        <p class="card-text">
          <span class="imageAttribut">{{ t('ImageSingle.filename') }}</span> :
          {{ image.filename }}
        </p>

        <p class="card-text">
          <span class="imageAttribut">{{ t('ImageSingle.publisher') }}</span> :
          {{ publisher }}
        </p>

        <p class="card-text">
          <span class="imageAttribut">{{ t('ImageSingle.datePublication') }}</span> :
          {{ datePublication }}
        </p>

        <p v-if="image.metadata" class="card-text">
          {{ t('component.common.metadata') }}: {{ image.metadata }}
        </p>

        <div class="d-flex justify-content-between">
          <n-tooltip trigger="hover" placement="top">
            <template #trigger>
              <div
                @click="annotate"
                role="button"
                class="m-2 cardAnnotationIcon"
              >
                <opensilex-Icon icon="fa#pencil-alt" />
              </div>
            </template>
            {{ t('ImageSingle.annotate') }}
          </n-tooltip>

          <n-tooltip trigger="hover" placement="top">
            <template #trigger>
              <div
                @click="toggleAdvancedSearch"
                role="button"
                class="m-2 cardAdvancedSearchIcon"
              >
                <opensilex-Icon v-if="!detailOpen" icon="fa#eye" />
                <i v-else class="bi bi-dash-lg"></i>
              </div>
            </template>
            {{ t('ImageSingle.provenance') }}
          </n-tooltip>
        </div>

        <!-- Image details on click -->
        <n-collapse-transition :show="detailOpen">
          <div class="mt-2 provenanceInformations">
            <p class="card-text">
              <span class="imageAttribut">
                {{ t('ImageSingle.filter.provenance') }}:
              </span>
            </p>

            <p v-if="image.provenance?.uri" class="card-text">
              <span class="imageAttribut">{{ t('component.common.uri') }}:</span>
              {{ image.provenance.uri }}
            </p>

            <p v-if="image.provenance?.prov_used" class="card-text">
              <span class="imageAttribut">{{ t('ImageSingle.used') }}:</span>
              {{ image.provenance.prov_used }}
            </p>

            <p v-if="image.provenance?.prov_was_associated_with" class="card-text">
              <span class="imageAttribut">{{ t('ImageSingle.associated') }}:</span>
              {{ image.provenance.prov_was_associated_with }}
            </p>

            <p v-if="image.provenance?.settings" class="card-text">
              <span class="imageAttribut">{{ t('ImageSingle.setting') }}:</span>
              {{ image.provenance.settings }}
            </p>

            <p v-if="image.provenance?.experiments" class="card-text">
              <span class="imageAttribut">{{ t('ImageSingle.filter.experiments') }}:</span>
              {{ image.provenance.experiments }}
            </p>
          </div>
        </n-collapse-transition>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NCard, NCollapseTransition, NTooltip } from 'naive-ui'

import type { DataFileImageDTO } from '../data/DataFileImageDTO'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

const props = defineProps<{
  image: DataFileImageDTO
  index: number
}>()

const emit = defineEmits<{
  (e: 'click', index: number): void
  (e: 'annotate', uri: string): void
}>()

const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const detailOpen = ref(false)

const src = computed(() => props.image.url)

const date = computed(() => {
  return props.image.date
    ? $opensilex.$dateTimeFormatter.formatISODateTime(props.image.date)
    : undefined
})

const type = computed(() => {
  return props.image.rdf_type?.includes(':')
    ? props.image.rdf_type.split(':')[1]
    : props.image.rdf_type
})

const publisher = computed(() => {
  const publisher = props.image.publisher

  if (!publisher || !publisher.uri) {
    return undefined
  }

  if (publisher.first_name && publisher.last_name) {
    return `${publisher.first_name} ${publisher.last_name}`
  }

  return publisher.uri
})

const datePublication = computed(() => {
  return props.image.issued
    ? $opensilex.$dateTimeFormatter.formatISODateTime(props.image.issued)
    : undefined
})

function toggleAdvancedSearch() {
  detailOpen.value = !detailOpen.value
}

function showImage() {
  emit('click', props.index)
}

function annotate() {
  emit('annotate', props.image.uri)
}
</script>

<style scoped lang="scss">
.cardContainer {
  padding: 0 10px 10px 0;
}

:deep(.n-card__content) {
  padding: 0;
  background-color: #ffffff;
}

.card-body {
  padding: 0;
  background-color: #ffffff;
}

.provenanceInformations {
  margin-bottom: 10px;
}

.card-img-top {
  margin-bottom: 10px;
}

p {
  font-size: 9px;
}

.imageAttribut {
  font-weight: bold;
}

.cardAdvancedSearchIcon,
.cardAnnotationIcon {
  padding: 5px 5px 5px 5px;
  border-radius: 50%;
  font-size: 1.1em;
}

.card-text {
  margin-bottom: 0;
  font-size: 1em;
  margin: 0 5px 0 5px;
}

img {
  width: 100% !important;
  max-width: 400px;
}

img:hover {
  cursor: pointer;
}

@media (min-width: 992px) {
  .cardContainer {
    -webkit-box-flex: 0;
    flex: 0 0 16.5%;
    max-width: 16.5%;
  }
}
</style>

<i18n>
en:
  ImageSingle:
    filename: Name
    target: Target
    used: 'Used by'
    associated: 'Associated to'
    setting: 'Settings'
    annotate: Annotate
    provenance: Provenance
    publisher: Publisher
    datePublication: Publication date
    filter:
      experiments: Experiment(s)
      provenance: Provenance

fr:
  ImageSingle:
    filename: Nom
    target: Cible
    used: 'Utilisée par'
    associated: 'Asssociée à'
    setting: 'Paramètres'
    annotate: Annoter
    provenance: Provenance
    publisher: Publieur
    datePublication: Date de publication
    filter:
      experiments: Expérimentation(s)
      provenance: Provenance
</i18n>