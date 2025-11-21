<template>
  <div class="v-step-skos-relation-input">
    <div class="row">
      <n-input
        class="uri-input"
        v-model:value="uri"
        type="text"
        :placeholder="t('component.skos.uri-placeholder')"
        @keyup.enter="onEnter"
      />
      <opensilex-SkosSelector
        class="selector"
        :selectedRelation="relationDtoKey"
        @update:selectedRelation="val => { relationDtoKey = val; onSelected() }"
      />
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, defineEmits, defineExpose } from 'vue'
import { useI18n } from 'vue-i18n'
import { NInput } from 'naive-ui'
import SkosSelector from './SkosSelector.vue'

type UriSkosRelation = { uri: string; relationDtoKey: string }

const emit = defineEmits<{
  (e: 'input', payload: UriSkosRelation): void
}>()

const { t } = useI18n()

// state
const uri = ref<string>('')
const relationDtoKey = ref<string | null>(null)
const touched = ref(false)

// regex schéma requis + chemin non vide
const uriRegex =
  /^(([^:/?#]+):)(\/\/([^/?#]*))?([^?#]+)(\?([^#]*))?(#(.*))?$/

const isValidUri = computed(() => uri.value.trim().length > 0 && uriRegex.test(uri.value))

const error = computed(() => {
  if (!touched.value) return ''
  if (uri.value.trim() === '') return t('component.skos.uri') + ' - ' + t('component.common.required') as string
  if (!isValidUri.value) return t('component.skos.uri') + ' - ' + t('component.common.errors.not-a-valid-uri') as string
  if (!relationDtoKey.value) return t('component.skos.relation') + ' - ' + t('component.common.required') as string
  return ''
})

function tryEmit () {
  touched.value = true
  if (isValidUri.value && relationDtoKey.value) {
    emit('input', { uri: uri.value.trim(), relationDtoKey: relationDtoKey.value } )
  }
}

function onSelected () {
  tryEmit()
}

function onEnter () {
  tryEmit()
}

// API pour le parent (remplace reset du ValidationProvider)
function reset () {
  uri.value = ''
  relationDtoKey.value = null
  touched.value = false
}
defineExpose({ reset })
</script>

<style scoped>
.v-step-skos-relation-input {
  display: block;
}
.row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.uri-input {
  flex: 1 1 auto;
  min-width: 220px;
}
.selector {
  flex: 0 0 auto;
}
.error-message {
  margin-top: 6px;
  padding: 6px 8px;
  border-radius: 4px;
  background: #f8d7da;
  color: #842029;
  font-size: 0.9rem;
}
</style>
