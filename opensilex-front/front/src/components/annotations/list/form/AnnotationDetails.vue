<template>
  <teleport to="body">
    <div
      v-if="visible"
      class="ad-modal-backdrop"
      @click.self="close"
    >
      <div class="ad-modal" @keydown.enter.stop="">
        <div class="ad-modal-header">
          <strong>{{ t('component.annotation.details') }}</strong>
          <button class="ad-modal-close" @click="close" aria-label="Close">×</button>
        </div>

        <div class="ad-modal-body">
          <div class="details-container">
            <p>
              <strong>URI :</strong>
              <opensilex-UriLink
                :uri="annotationDetails?.uri"
                :value="annotationDetails?.uri"
              />
            </p>

            <p>
              <strong>{{ t('component.annotation.motivation') }} :</strong>
              {{ annotationDetails?.motivation?.name || t('component.common.not-specified') }}
            </p>

            <p>
              <strong>{{ t('component.annotation.publisher') }} :</strong>
              {{ annotationDetails?.publisher || t('component.common.not-specified') }}
            </p>

            <p>
              <strong>{{ t('component.annotation.published') }} :</strong>
              {{ formatDate(annotationDetails?.published) || t('component.common.not-specified') }}
            </p>

            <p>
              <strong>{{ t('component.annotation.description') }} :</strong>
              {{ annotationDetails?.description || t('component.common.not-specified') }}
            </p>
          </div>
        </div>

        <div class="ad-modal-footer">
          <button class="helpButton" type="button" @click="close">OK</button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin'

type AnnDetails = {
  uri?: string
  motivation?: { name?: string }
  publisher?: string
  published?: string
  description?: string
}

// Props
const props = withDefaults(defineProps<{
  value?: boolean
  annotationDetails?: AnnDetails
}>(), {
  value: false,
  annotationDetails: () => ({})
})

// Emits
const emit = defineEmits<{
  (e: 'update:value', v: boolean): void
  (e: 'close'): void
}>()

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

const visible = computed({
  get: () => !!props.value,
  set: (v: boolean) => emit('update:value', v)
})

function close () {
  visible.value = false
  emit('close')
}

function formatDate (dateStr?: string): string {
  if (!dateStr) return ''
  // Utilise le même formateur que la liste (YYYY-MM-DD HH:mm:ss)
  return opensilex.$dateTimeFormatter.formatLocalFixedDateTime(dateStr)
}
</script>

<style scoped>
.ad-modal-backdrop {
  position: fixed; 
  inset: 0;
  background: rgba(0,0,0,.45);
  display: flex; 
  align-items: center; 
  justify-content: center;
  z-index: 1050;
}
.ad-modal {
  background: white; 
  border-radius: 8px;
  width: min(620px, 92vw);
  box-shadow: 0 10px 30px rgba(0,0,0,.2);
  display: flex; 
  flex-direction: column;
}
.ad-modal-header {
  display: flex; 
  align-items: center; 
  justify-content: space-between;
  padding: 12px 16px; 
  border-bottom: 1px solid #eee;
}
.ad-modal-close {
  background: transparent; 
  border: none; 
  font-size: 20px; 
  line-height: 1; 
  cursor: pointer;
}
.ad-modal-body { padding: 16px; }

.ad-modal-footer { 
  padding: 12px 16px; 
  text-align: right; 
}

.details-container { text-align: left; padding: 4px 4px; }

.helpButton {
  color: #f1f1f1;
  background-color: #00A28C;
  border: none;
  border-radius: 4px;
  padding: 6px 12px;
}
.helpButton:hover {
  background-color: #00A2B0;
  color: #f1f1f1;
  border: none;
}
.title-popup { font-weight: bold; padding-right: 1%; }
.champ-popup { display: flex; }
</style>