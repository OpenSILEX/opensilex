<template>
  <n-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-placement="top"
    :show-require-mark="true"
  >
    <!-- URI -->
    <opensilex-UriForm
      v-model:uri="form.uri"
      :generated="localUriGenerated"
      @update:generated="val => (localUriGenerated = val)"
      :editMode="editMode"
      :helpMessage="$t('component.common.uri-help-message')"
      label="component.common.uri"
    />

    <!-- Motivation -->
    <n-form-item :show-label="false" path="motivation">
      <opensilex-FormSelector
        v-model:selected="form.motivation"
        :required="true"
        :multiple="false"
        :disabled="viewMode"
        :options="motivations"
        :itemLoadingMethod="loadMotivation"
        :label="'component.annotation.motivation'"
        :noResultsText="t('component.annotation.no-motivation')"
        :helpMessage="t('component.annotation.motivation-help')"
        :placeholder="t('component.annotation.select-motivation')"
      />
    </n-form-item>

    <!-- Description -->
    <n-form-item :label="$t('component.annotation.description')" path="description">
      <opensilex-TextAreaForm
        v-model:value="form.description"
        :required="true"
        @keydown.enter.stop
        :placeholder="t('component.common.set-description')"
      />
    </n-form-item>
  </n-form>
</template>

<script setup lang="ts">
import { ref, computed, inject, onMounted, onBeforeUnmount, withDefaults, defineProps } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'
import type { FormInst } from 'naive-ui'
import { NForm, NFormItem } from 'naive-ui'
import { requiredTrimmed } from  "../../../../models/FormFieldsFormatter"

import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin'
import HttpResponse, { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type { AnnotationsService } from 'opensilex-core/api/annotations.service'
import type { AnnotationCreationDTO, NamedResourceDTO } from 'opensilex-core/index'

const props = withDefaults(defineProps<{
  editMode?: boolean
  uriGenerated?: boolean
  form: AnnotationCreationDTO
}>(), {
  editMode: false,
  uriGenerated: true,
  form: () => ({
    uri: undefined,
    motivation: undefined,
    targets: [],
    description: undefined
  } as AnnotationCreationDTO)
})

const emit = defineEmits<{}>()
const { t } = useI18n()
const store = useStore()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = opensilex.getService<AnnotationsService>('opensilex.AnnotationsService')

// ---- Refs / état local
const formRef = ref<FormInst | null>(null)
const localUriGenerated = ref<boolean>(props.uriGenerated ?? true)
const editMode = computed(() => !!props.editMode)
const viewMode = ref(false)

const motivations = ref<Array<{ label: string; id: string }>>([])

// ---- Règles Naive UI
const rules = computed(() => ({
  motivation: { required: true, message: t('validations.required_if', {_field_: t('component.annotation.motivation')}), trigger: 'change' },
  description: requiredTrimmed('Description')
}))

// ---- Chargement des motivations
function searchMotivations () {
  service.searchMotivations(undefined, ['name=asc'], undefined, undefined)
    .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
      motivations.value = (http?.response?.result ?? []).map(m => ({ label: m.name, id: m.uri }))
    })
    .catch(opensilex.errorHandler)
}

/**
 * Supporte la valeur existante :
 *  - En édition, la valeur peut être un objet { uri, name } -> renvoie l’option correspondante et normalise en URI
 *  - Sinon, c’est déjà une URI -> renvoie l’option trouvée
 */
function loadMotivation (selected: Array<any>): Array<any> | undefined {
  if (!selected || selected.length === 0) return undefined

  // Édition : valeur objet { uri, name }
  if (selected[0]?.uri) {
    const opt = { label: (props.form as any).motivation.name, id: (props.form as any).motivation.uri }
    ;(props.form as any).motivation = (props.form as any).motivation.uri
    return [opt]
  }

  // Création : valeur = URI
  const found = motivations.value.find(m => m.id === selected[0])
  return found ? [found] : undefined
}

// ---- API attendue par ModalForm (exposée)
function getEmptyForm (): AnnotationCreationDTO {
  return {
    uri: undefined,
    motivation: undefined,
    targets: [],
    description: undefined
  }
}

function reset () {
  localUriGenerated.value = true
}

async function validate () {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}

defineExpose({
  getEmptyForm,
  reset,
  validate
})

// ---- Lifecycle
let unwatchLang: any
onMounted(() => {
  searchMotivations()
  unwatchLang = store.watch(
    () => store.getters.language,
    () => searchMotivations()
  )
})
onBeforeUnmount(() => {
  unwatchLang && unwatchLang()
})
</script>

<style scoped lang="scss">
</style>
