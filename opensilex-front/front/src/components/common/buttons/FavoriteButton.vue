<template>
  <n-button
    @click.prevent="handleClick"
    :title="toggleTitle()"
    secondary
    type="warning"
    :class="{ favoriteActive: favoriteState }"
  >
    <slot name="icon">
      <opensilex-Icon icon="fa#star" />
    </slot>
  </n-button>
</template>

<script setup lang="ts">
import { inject, ref, watch, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'
import { NButton } from 'naive-ui'

import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import type { SecurityService } from 'opensilex-security/api/security.service'
import type HttpResponse from 'opensilex-core/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-core/HttpResponse'
import { FAVORITE_TYPES } from '../../home/dashboard/Favorites.vue'

const props = defineProps<{
  uri: string
}>()

const emit = defineEmits<{
  (e: 'click'): void
}>()

const store = useStore()
const { t } = useI18n()
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const favoriteState = ref(false)
const userURI = ref<string>('')

const service = $opensilex.getService<SecurityService>('opensilex.SecurityService')

async function getFavoriteState() {
  if (!props.uri) {
    favoriteState.value = false
    return
  }

  try {
    const http: HttpResponse<OpenSilexResponse<any[]>> =
      await service.getFavorites(FAVORITE_TYPES)

    favoriteState.value = http.response.result.some((fav: any) => fav.uri === props.uri)
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

async function addFavorite() {
  try {
    await service.addFavorite({ uri: props.uri })
    favoriteState.value = true
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

async function removeFavorite() {
  try {
    await service.deleteFavorite(props.uri)
    favoriteState.value = false
  } catch (error) {
    $opensilex.errorHandler(error)
  }
}

async function handleClick() {
  emit('click')

  if (!favoriteState.value) {
    await addFavorite()
  } else {
    await removeFavorite()
  }
}

function toggleTitle() {
  return favoriteState.value
    ? t('FavoriteButton.labelRemove')
    : t('FavoriteButton.labelAdd')
}

onMounted(() => {
  userURI.value = store.state.user?.tokenData?.sub ?? ''
  getFavoriteState()
})

watch(
  () => props.uri,
  () => {
    getFavoriteState()
  }
)
</script>

<style scoped lang="scss">
.favoriteActive {
  background-color: #ffc107 !important;
  border-color: #ffc107 !important;
  color: #212529 !important;
}

.favoriteActive :deep(svg),
.favoriteActive :deep(i) {
  color: #212529 !important;
}
</style>

<i18n>
en:
  FavoriteButton:
    labelAdd: Add to favorites
    labelRemove: Remove from favorites

fr:
  FavoriteButton:
    labelAdd: Ajouter aux favoris
    labelRemove: Retirer des favoris
</i18n>