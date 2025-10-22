<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template #field="field">
      <!-- input natif caché (réutilisé par le bouton & la dropzone) -->
      <input
        :id="field.id"
        ref="inputRef"
        type="file"
        class="hidden-input"
        :disabled="disabled"
        :required="required"
        :accept="accept"
        @change="onChange"
      />

      <!-- DROPZONE CLIQUABLE (glisser / déposer + clic) -->
      <div
        class="dropzone"
        :class="{ dragging: isDragging, disabled }"
        role="button"
        tabindex="0"
        @click="trigger"
        @keydown.enter.prevent="trigger"
        @keydown.space.prevent="trigger"
        @dragover.prevent="onDragOver"
        @dragenter.prevent="onDragEnter"
        @dragleave.prevent="onDragLeave"
        @drop.prevent="onDrop"
      >
        <div class="dropzone-inner">
          <div class="dropzone-left">
            <div class="dropzone-title">
              <span v-if="fileName">{{ fileName }}</span>
              <span v-else>{{ t(dropPlaceholder || 'FileInputForm.file-help') }}</span>
            </div>
            <!-- <div class="dropzone-sub" v-if="!fileName">
              {{ t('FileInputForm.drop-here') }}
            </div> -->
          </div>

          <button
            type="button"
            class="btn greenThemeColor"
            :disabled="disabled"
            @click.stop="trigger"
          >
            {{ t(browseText || 'FileInputForm.browse') }}
          </button>
        </div>
      </div>
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { ref, computed, defineProps, defineEmits } from 'vue'
import { useI18n } from 'vue-i18n'

type Rules = string | ((val: any) => true | string)

const props = defineProps<{
  file?: File | undefined
  label?: string
  helpMessage?: string
  placeholder?: string
  dropPlaceholder?: string   // clé i18n pour le texte de zone
  browseText?: string        // clé i18n pour le bouton
  required?: boolean
  disabled?: boolean
  rules?: Rules
  accept?: string            // ex: ".pdf,.docx" ou "image/*"
}>()

const emit = defineEmits<{
  (e: 'update:file', f?: File): void
  (e: 'change', f?: File): void
}>()

const { t } = useI18n()

const inputRef = ref<HTMLInputElement | null>(null)
const isDragging = ref(false)

function trigger () {
  if (!props.disabled) inputRef.value?.click()
}

function onChange (e: Event) {
  const el = e.target as HTMLInputElement
  const f = el.files && el.files[0] ? el.files[0] : undefined
  emit('update:file', f)
  emit('change', f)
}

function onDragOver () {
  if (props.disabled) return
  isDragging.value = true
}

function onDragEnter () {
  if (props.disabled) return
  isDragging.value = true
}

function onDragLeave () {
  isDragging.value = false
}

function onDrop (e: DragEvent) {
  isDragging.value = false
  if (props.disabled) return
  const file = e.dataTransfer?.files?.[0]
  if (!file) return

  if (props.accept && !isFileAccepted(file, props.accept)) {
    return
  }
  emit('update:file', file)
}

const fileName = computed(() => props.file?.name ?? '')

/** Vérifie si un fichier correspond à la règle accept (".pdf,image/*") */
function isFileAccepted (file: File, accept: string) {
  // Ex: ".pdf,.docx,image/*,application/pdf"
  const tokens = accept.split(',').map(token => token.trim()).filter(Boolean)
  if (!tokens.length) return true

  const extension = '.' + (file.name.split('.').pop() || '').toLowerCase()
  const type = file.type // ex: "image/png" ou "application/pdf"
  const [typeMain] = type.split('/')

  return tokens.some(token => {
    if (token.startsWith('.')) {
      return extension === token.toLowerCase()
    }
    if (token.endsWith('/*')) {
      const main = token.slice(0, -2).toLowerCase() // "image"
      return typeMain.toLowerCase() === main
    }
    // exact
    return type.toLowerCase() === token.toLowerCase()
  })
}
</script>

<style scoped>
.hidden-input { display: none; }

.dropzone {
  border: 2px dashed rgba(0,0,0,.15);
  border-radius: .75rem;
  padding: .9rem .9rem;
  cursor: pointer;
  transition: border-color .15s ease, background-color .15s ease;
}

.dropzone.disabled {
  opacity: .6;
  cursor: not-allowed;
}

.dropzone.dragging {
  border-color: var(--primary, #bfecdf);
  background: rgba(64,158,255,.06);
}

.dropzone-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: .75rem;
  flex-wrap: wrap;
}

.dropzone-left {
  display: flex;
  flex-direction: column;
}

.dropzone-title {
  font-size: 0.95rem;
  font-weight: 500;
  color: #00A38D;
}

/* .dropzone-sub {
  font-size: .85rem;
  opacity: .7;
} */
</style>

<i18n>
en:
  FileInputForm:
    file: Document
    file-help: Drag & drop a file here or use the button (max 100MB)
    browse: Browse
    drop-here: Drag & drop here
fr:
  FileInputForm:
    file: Document
    file-help: Glissez-déposez un fichier ici ou utilisez le bouton (max 100MB)
    browse: Parcourir
    drop-here: Glissez-déposez ici
</i18n>
