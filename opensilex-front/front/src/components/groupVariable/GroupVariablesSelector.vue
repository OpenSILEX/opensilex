<template>
  <!-- IMPORTANT: in Vue 3, @keyup/@keydown on a child component is no longer attached natively.
       Enter is therefore captured on a wrapper. -->
  <div @keydown.enter.prevent.stop="onEnter">
    <InfiniteScrollDropdown
      ref="dropdown"
      v-model:selected="variableGroupURI"
      :fetchPage="searchVariablesGroups"
      :itemLoadingMethod="loadVariablesGroups"
      :conversionMethod="variablesGroupToSelectOption"
      :label="label"
      :multiple="multiple"
      :placeholder="placeholder"
    :noResultsText="t('groupVariableSelector.form.selector.filter-search-no-result')"
      @selectionChange="(option) => emit('selectionChange', option)"
      @clear="emit('clear')"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue';
import type { SelectOption } from 'naive-ui';
import type { VariablesGroupGetDTO, VariablesService } from 'opensilex-core/index';
import HttpResponse, { OpenSilexResponse } from 'opensilex-security/HttpResponse';
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin';
import InfiniteScrollDropdown from '@/components/common/forms/InfiniteScrollDropdown.vue';
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
  (e: 'selectionChange', option: SelectOption | SelectOption[] | undefined): void;
  (e: 'clear'): void;
  (e: 'handlingEnterKey'): void;
}>();

const { t } = useI18n()

// v-model proxy
const variableGroupURI = computed({
  get: () => props.variableGroup,
  set: (v) => emit('update:variableGroup', v)
});

const $opensilex = inject<OpenSilexVuePlugin>("$opensilex")!;

// InfiniteScrollDropdown is a generic component, so it has no constructor type for InstanceType to
// read. Only `refresh` is needed here, so the exposed shape is declared directly.
const dropdown = ref<{ refresh: () => Promise<void> } | null>(null);

const placeholder = computed(() =>
  props.multiple
    ? t('groupVariableSelector.form.selector.placeholder-multiple')
    : t('groupVariableSelector.form.selector.placeholder')
);

// Results depend on the shared resource instance, so the search is re-run whenever it changes.
watch(() => props.sharedResourceInstance, () => {
  dropdown.value?.refresh();
});

/** Loads the already selected elements (update form), so that their name can be displayed. */
function loadVariablesGroups(variableGroupURIs: string[]): Promise<Array<VariablesGroupGetDTO>> {
  const service = $opensilex.getService<VariablesService>('opensilex.VariablesService');
  return service
    .getVariablesGroupByURIs(variableGroupURIs, props.sharedResourceInstance)
    .then((http: HttpResponse<OpenSilexResponse<Array<VariablesGroupGetDTO>>>) => http.response.result)
    .catch($opensilex.errorHandler);
}

/** Loads one page of results. `page` is zero-based. */
function searchVariablesGroups(
  searchQuery: string,
  page: number,
  pageSize: number
): Promise<HttpResponse<OpenSilexResponse<Array<VariablesGroupGetDTO>>>> {
  const service = $opensilex.getService<VariablesService>('opensilex.VariablesService');
  return service.searchVariablesGroups(
      searchQuery,
      undefined,
      ['name=asc'],
      page,
      pageSize,
      props.sharedResourceInstance
    )
    .then((http: HttpResponse<OpenSilexResponse<Array<VariablesGroupGetDTO>>>) => http);
}

const variablesGroupToSelectOption = (dto: VariablesGroupGetDTO): SelectOption => ({
  label: dto.name,
  value: dto.uri
});

// Enter key event (captured on the wrapper div)
function onEnter() {
  emit('handlingEnterKey');
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