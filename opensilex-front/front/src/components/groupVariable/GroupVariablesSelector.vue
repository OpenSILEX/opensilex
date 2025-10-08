<template>
  <!-- IMPORTANT: en Vue 3, @keyup/@keydown sur un composant enfant
       ne s’attache plus “nativement”. On capture donc Enter sur un wrapper. -->
  <div @keydown.enter.prevent.stop="onEnter">
    <opensilex-FormSelector
      ref="formSelector"
      :label="label"
      v-model:selected="variableGroupURI"
      :multiple="multiple"
      :searchMethod="searchVariablesGroups"
      :itemLoadingMethod="loadVariablesGroups"
      :placeholder="placeholder"
      noResultsText="component.groupVariable.form.selector.filter-search-no-result"
      @clear="$emit('clear')"
      @select="select"
      @deselect="deselect"
      @loadMoreItems="loadMoreItems"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, nextTick, ref, watch } from 'vue';
import type { VariablesGroupGetDTO, VariablesService } from 'opensilex-core/index';
import HttpResponse, { OpenSilexResponse } from 'opensilex-security/HttpResponse';
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import FormSelector from '@/components/common/forms/FormSelector.vue';
import { useI18n } from 'vue-i18n'

type VgModel = string | string[] | null | undefined;

const props = defineProps<{
  variableGroup?: VgModel;
  label?: string;
  multiple?: boolean;
  sharedResourceInstance?: string;
}>();

const emit = defineEmits<{
  (e: 'update:variableGroup', v: VgModel): void;
  (e: 'clear'): void;
  (e: 'select', v: any): void;
  (e: 'deselect', v: any): void;
  (e: 'handlingEnterKey'): void;
}>();

const { t } = useI18n()

const variableGroupURI = computed({
  get: () => props.variableGroup,
  set: (v) => emit('update:variableGroup', v)
});

const $opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;
const formSelector = ref<InstanceType<typeof FormSelector> | null>(null);

const pageSize = ref(10);

const placeholder = computed(() =>
  props.multiple
    ? t('groupVariableSelector.form.selector.placeholder-multiple')
    : t('groupVariableSelector.form.selector.placeholder')
);

watch(() => props.sharedResourceInstance, () => {
  formSelector.value?.refresh?.();
});

// charge un lot par URIs
function loadVariablesGroups(variableGroupURIs: string[]): Promise<Array<VariablesGroupGetDTO>> {
  const service = $opensilex.getService<VariablesService>('opensilex.VariablesService');
  return service
    .getVariablesGroupByURIs(variableGroupURIs, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<Array<VariablesGroupGetDTO>>>) => http.response.result)
    .catch($opensilex.errorHandler);
}

// recherche paginée
function searchVariablesGroups(
  searchQuery: string,
  page: number,
  _pageSize: number
): Promise<HttpResponse<OpenSilexResponse<Array<VariablesGroupGetDTO>>>> {
  const service = $opensilex.getService<VariablesService>('opensilex.VariablesService');
  return service.searchVariablesGroups(
      searchQuery,
      undefined,
      ['name=asc'],
      page,
      pageSize.value,
      props.sharedResourceInstance
    )
    .then((http: HttpResponse<OpenSilexResponse<Array<VariablesGroupGetDTO>>>) => http);
}

// relayer les events simples
function select(value: any)   { emit('select', value); }
function deselect(value: any) { emit('deselect', value); }

// event touche Entrée (capturée sur le wrapper div)
function onEnter() {
  emit('handlingEnterKey');
}

// charger plus (ouvre le treeselect après refresh)
async function loadMoreItems() {
  pageSize.value = 0;
  formSelector.value?.refresh?.();
  await nextTick();
  formSelector.value?.openTreeselect?.();
}
</script>

<style scoped lang="scss"></style>

<i18n>
en:
    groupVariableSelector:
      form:
        selector:
          placeholder: "Select one group of variables"
          placeholder-multiple: "Select one or more groups of variables"
          filter-search-no-result: "No groups of variables found"
fr:
    groupVariableSelector:
      form:
        selector:
          placeholder: "Sélectionner un groupe de variables"
          placeholder-multiple: "Sélectionner un ou plusieurs groupes de variables"
          filter-search-no-result: "Aucun groupe de variables trouvé"
</i18n>