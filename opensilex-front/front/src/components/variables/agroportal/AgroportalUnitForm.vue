<template>
  <opensilex-AgroportalCreateForm
    ref="createForm"
    :requireCreate="true"
    @onCreate="(e) => emit('onCreate', e)"
    @onUpdate="(e) => emit('onUpdate', e)"
    ontologiesConfig="unitOntologies"
    :searchPlaceholder="t('AgroportalUnitForm.name-placeholder')"
    :createTitle="t('AgroportalUnitForm.add')"
    :editTitle="t('AgroportalUnitForm.edit')"
    :createMethod="createMethod"
    :updateMethod="updateMethod"
    :emptyForm="emptyForm"
  >
    <template #createAdditionalFields="{ form, errors }">
      <!-- Symbol avec affichage de l’erreur -->
      <div class="mb-3" :class="{ 'has-error': !!errors?.symbol }">
        <opensilex-InputForm
          v-model:value="form.symbol"
          :label="t('AgroportalUnitForm.symbol')"
          type="text"
          :required="true"
          :placeholder="t('AgroportalUnitForm.symbol-placeholder')"
        />
        <div v-if="errors?.symbol" class="field-error">
          {{ errors.symbol }}
        </div>
      </div>

      <!-- Alternative symbol -->
      <opensilex-InputForm
        v-model:value="form.alternative_symbol"
        :label="t('AgroportalUnitForm.alternative-symbol')"
        type="text"
        :placeholder="t('AgroportalUnitForm.alternative-symbol-placeholder')"
      />
    </template>
    
  </opensilex-AgroportalCreateForm>
</template>


<script setup lang="ts">
import { ref, inject } from 'vue'
import type OpenSilexVuePlugin from '../../../models/OpenSilexVuePlugin'
import { VariablesService } from 'opensilex-core/api/variables.service'
import type { UnitCreationDTO } from 'opensilex-core/model/unitCreationDTO'
import { useI18n } from 'vue-i18n'

// Événements à propager vers le parent
const emit = defineEmits<{
  (e: 'onCreate', payload: any): void
  (e: 'onUpdate', payload: any): void
}>()

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = opensilex.getService<VariablesService>('opensilex-core.VariablesService')
const { t } = useI18n({ useScope: 'local' })

// Référence vers le composant enfant AgroportalCreateForm
const createForm = ref<any>(null)

// État initial du formulaire
const emptyForm: UnitCreationDTO = {
  uri: null,
  name: null,
  description: null,
  symbol: null,
  alternative_symbol: null,
  exact_match: [],
  close_match: [],
  broad_match: [],
  narrow_match: []
}

// Méthodes passées à AgroportalCreateForm
const createMethod = (payload: any) => service.createUnit(payload)
const updateMethod = (payload: any) => service.updateUnit(payload)

function showCreateForm() {
  createForm.value?.showCreateForm?.()
}

function showEditForm(dto: any) {
  createForm.value?.showEditForm?.(dto)
}

defineExpose({ showCreateForm, showEditForm })
</script>

<style scoped lang="scss">
/* Bordure rouge autour du champ requis quand erreur */
.has-error :deep(.n-input)
 {
  border: 1px solid !important;
  border-color: red !important;
}
</style>

<i18n>
en:
  AgroportalUnitForm:
    add: Add unit
    edit: Edit unit
    symbol: Symbol
    alternative-symbol: Alternative symbol
    name-placeholder: Meter per second
    symbol-placeholder: m/s
    alternative-symbol-placeholder: m.s⁻¹
fr:
  AgroportalUnitForm:
    add: Ajouter une unité
    edit: Modifier une unité
    symbol: Symbole
    alternative-symbol: Symbole alternatif
    name-placeholder: Mètre par seconde
    symbol-placeholder: m/s
    alternative-symbol-placeholder: m.s⁻¹
</i18n>
