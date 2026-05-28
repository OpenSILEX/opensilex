<template>
  <div>
    <div v-if="images.length === 0 && !showScrollSpinner">
      {{ t('DataFilesImagesList.no-file') }}
    </div>

    <n-tooltip
    v-if="showGoBackToTopButton"
    trigger="hover"
    placement="top"
    >
    <template #trigger>
        <n-button
        @click="goBackToTop"
        id="goBackToTop"
        class="greenThemeColor"
        quaternary
        circle
        >
        <i class="bi bi-chevron-double-up"></i>
        </n-button>
    </template>

    {{ t('DataFilesImagesList.goToTopButton') }}
    </n-tooltip>

    <!-- Images list -->
    <opensilex-ImageGrid
      :images="images"
      @click="onImageClicked"
      @annotate="onImageAnnotate"
    />

    <!-- Enlarged image view -->
    <opensilex-ImageLightBox
      :key="key"
      ref="imageLightBox"
      :images="images"
    />

    <strong v-if="images.length >= 35 && !showScrollSpinner">
      {{ t('DataFilesImagesList.scroll-to-display') }}
    </strong>

    <opensilex-AnnotationModalForm
      ref="annotationModalForm"
    />

    <div v-if="showScrollSpinner" class="d-flex align-items-center">
      <strong>{{ t('component.common.loading') }}</strong>
      <div
        class="spinner-border ms-auto"
        role="status"
        aria-hidden="true"
      ></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { inject, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { NButton, NTooltip } from 'naive-ui'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type HttpResponse from '@/lib/HttpResponse'
import type {
  DataFileGetDTO,
  DataService,
  OpenSilexResponse
} from 'opensilex-core/index'
import type { DataFileImageDTO } from './DataFileImageDTO'

const props = withDefaults(defineProps<{
  filter?: {
    name?: string
    start_date?: string
    end_date?: string
    rdf_type?: string
    provenance?: string
    experiments?: string[]
    scientificObjects?: string[]
    devices?: string[]
  }
  scaledWidth?: number
  scaledHeight?: number
}>(), {
  filter: () => ({
    name: undefined,
    start_date: undefined,
    end_date: undefined,
    rdf_type: undefined,
    provenance: undefined,
    experiments: [],
    scientificObjects: [],
    devices: []
  }),
  scaledWidth: 1250,
  scaledHeight: 640
})

const { t } = useI18n()
const route = useRoute()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = $opensilex.getService<DataService>('opensilex.DataService')

const currentPage = ref(0)
const pageSize = ref(30)

const images = ref<DataFileImageDTO[]>([])
const showScrollSpinner = ref(false)
const showGoBackToTopButton = ref(false)
const canReload = ref(true)
const paused = ref(false)
const key = ref(0)

const imageLightBox = ref<any>(null)
const annotationModalForm = ref<any>(null)

function normalizeDates() {
  if (props.filter.start_date === '') {
    props.filter.start_date = undefined
  }

  if (props.filter.end_date === '') {
    props.filter.end_date = undefined
  }
}

function isBottomOfWindow() {
  return window.innerHeight + window.scrollY >= document.body.offsetHeight - 1
}

function initScroll() {
  window.addEventListener('scroll', pageScroll)
}

function pageScroll() {
  toggleButtonDisplay()

  if (isBottomOfWindow() && !paused.value) {
    if (canReload.value) {
      reload()
    }

    paused.value = true
  }
}

function toggleButtonDisplay() {
  showGoBackToTopButton.value = isBottomOfWindow()
}

function goBackToTop() {
  document.body.scrollTop = 0
  document.documentElement.scrollTop = 0
}

function onImageClicked(index: number) {
  imageLightBox.value?.openOnIndex?.(index)
}

function onImageAnnotate(target: any) {
  annotationModalForm.value?.showCreateForm?.([target])
}

function refresh() {
  normalizeDates()

  images.value = []
  showScrollSpinner.value = true
  currentPage.value = 0
  canReload.value = true
  paused.value = false

  loadImages()
}

function reload() {
  normalizeDates()

  showScrollSpinner.value = true
  currentPage.value += 1

  loadImages()
}

function loadImage(
  dto: DataFileGetDTO,
  maxLength: number,
  index: number,
  reject: (reason?: any) => void
) {
  if (dto.archive) {
    return
  }

  const path =
    '/core/datafiles/' +
    encodeURIComponent(dto.uri) +
    `/thumbnail?scaled_width=${props.scaledWidth}&scaled_height=${props.scaledHeight}`

  $opensilex.viewImageFromGetService(path)
    .then((url: string) => {
      const image: DataFileImageDTO = {
        url,
        uri: dto.uri,
        rdf_type: dto.rdf_type,
        target: dto.target,
        date: dto.date,
        provenance: dto.provenance,
        filename: dto.filename,
        publisher: dto.publisher,
        issued: dto.issued
      }

      images.value.push(image)

      if (index === maxLength) {
        paused.value = false
        showScrollSpinner.value = false
      }

      key.value += 1
    })
    .catch((error: any) => {
      if (index === maxLength) {
        paused.value = false
        showScrollSpinner.value = false
        key.value += 1
      }

      reject(error)
    })
}

function loadImages() {
  return new Promise((resolve, reject) => {
    service
      .getDataFileDescriptionsByTargets(
        props.filter.name,
        props.filter.rdf_type
          ? props.filter.rdf_type
          : $opensilex.Oeso.IMAGE_TYPE_URI,
        props.filter.start_date,
        props.filter.end_date,
        undefined,
        props.filter.experiments,
        props.filter.devices,
        props.filter.provenance ? [props.filter.provenance] : undefined,
        undefined,
        ['date=desc'],
        currentPage.value,
        pageSize.value,
        props.filter.scientificObjects
      )
      .then((http: HttpResponse<OpenSilexResponse<Array<DataFileGetDTO>>>) => {
        const result = http.response.result as Array<DataFileGetDTO>

        if (showScrollSpinner.value) {
          showScrollSpinner.value = false
        }

        if (!result || result.length === 0) {
          canReload.value = false
          showScrollSpinner.value = false
          paused.value = false
          resolve(result)
          return
        }

        result.forEach((element, index) => {
          loadImage(element, result.length - 1, index, reject)
        })

        resolve(result)
      })
      .catch((error: any) => {
        $opensilex.errorHandler(error)
        canReload.value = false
        showScrollSpinner.value = false
        paused.value = false
        reject(error)
      })
  })
}

onMounted(() => {
  initScroll()

  $opensilex.updateFiltersFromURL(route.query, props.filter)

  normalizeDates()
  loadImages()
})

onUnmounted(() => {
  window.removeEventListener('scroll', pageScroll)
})

defineExpose({
  refresh,
  reload
})
</script>

<style scoped lang="scss">
#goBackToTop {
  position: fixed;
  bottom: 20px;
  right: 30px;
  z-index: 99;
  border: none;
  outline: none;
  cursor: pointer;
  padding: 10px;
  border-radius: 10px;
  font-size: 18px;
}
</style>

<i18n>
en:
  DataFilesImagesList:
    no-file: No file to display
    scroll-to-display: Scroll to see more images
    goToTopButton: "Go back to top"

fr:
  DataFilesImagesList:
    no-file: "Aucun fichier à afficher"
    scroll-to-display: "Faire défiler pour afficher plus d'images"
    goToTopButton: "Retour au début"
</i18n>