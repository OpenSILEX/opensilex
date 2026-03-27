<template>
  <div>
    <div v-if="!safeFacilities.length" id="no-facilities">
      {{ t('FacilitiesModalList.no_facilities') }}
    </div>

    <n-button
      v-else-if="safeFacilities.length === 1"
      class="greenThemeColorText"
      text
      @click="showPopup"
    >
      {{ safeFacilities[0].name }}
    </n-button>

    <n-button
      v-else
      class="greenThemeColorText"
      text
      @click="showPopup"
    >
      {{ safeFacilities[0].name + ' ...' }}
      <span class="badge badge-pill greenThemeColor">
        {{ '+' + (safeFacilities.length - 1) }}
      </span>
    </n-button>

    <n-modal v-model:show="show" class="facilities-modal">
      <n-card
        :title="t('FacilitiesModalList.title', [hostNameForTitle])"
        closable
        style="width: 65%; height: 90%;"
        @close="show = false"
      >
        <div id="facility-view">
          <opensilex-FacilitiesView
            :withActions="true"
            @onUpdate="emitOnCRUD"
            @onCreate="emitOnCRUD"
            @onDelete="emitOnCRUD"
            :facilities="safeFacilities"
            :site="currentSite"
            :isSelectable="false"
            isScrollable
          />
        </div>
      </n-card>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { NButton, NModal, NCard } from 'naive-ui'
import type { NamedResourceDTO } from 'opensilex-core/model/namedResourceDTO'

const { t } = useI18n()

const emit = defineEmits<{
  (e: 'onCRUD'): void
}>()

const props = withDefaults(defineProps<{
  facilities: NamedResourceDTO[]
  currentSite?: NamedResourceDTO | null
  hostNameForTitle?: string
}>(), {
  currentSite: null,
  hostNameForTitle: ''
})

const show = ref(false)

const safeFacilities = computed(() => Array.isArray(props.facilities) ? props.facilities : [])

function emitOnCRUD() {
  emit('onCRUD')
}

function showPopup() {
  show.value = true
}
</script>

<style lang="scss">
#name, #no-facilities {
  padding: 0;
}

#facility-view {
  padding-top: 5%;
}

// .facilities-modal .n-card {
//   width: fit-content;
// }
</style>

<i18n>
en:
  FacilitiesModalList:
    title: "Facilities of {0}"
    no_facilities: "No facilities"
fr:
  FacilitiesModalList:
    title: "Installations de {0}"
    no_facilities: "Aucune installation"
</i18n>
