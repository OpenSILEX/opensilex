<template>
  <FormField
    :required="required"
    :requiredBlue="requiredBlue"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template #field="{ id }">
      <!-- NFormItem gère l'astérisque, la bordure rouge et le message via les rules du NForm parent -->
      <n-form-item :path="path" :show-label="false">
        <n-select
          v-model:value="selectedValue"
          :id="id"
          filterable
          clearable
          remote
          :options="displayedOptions"
          :loading="isLoading"
          :placeholder="placeholder"
          :consistent-menu-width="false"
          :resetMenuOnOptionsChange="false"
          :multiple="multiple"
          @search="onSearch"
          @scroll="onDropdownScroll"
          @update:value="emit('selectionChange')"
          @keydown.enter.prevent="emit('handlingEnterKey')"
        >
          <template #action>
            <div class="infinite-scroll-selector-footer" :class="{ greenThemeColor: hasMoreResults }">
              <span v-if="isLoading">
                {{ t('component.common.loading') }}
                <template v-if="totalCount > 0">
                  —
                  {{ displayedCount }} / {{ totalCount }}
                </template>
              </span>

              <span v-else-if="hasMoreResults && totalCount > 0">
                {{ displayedCount }} / {{ totalCount }}
                —
                {{ t('component.common.scroll-to-load-more') }}
              </span>

              <span v-else-if="totalCount > 0">
                {{ displayedCount }} / {{ totalCount }}
                —
                {{ t('component.common.no-more-results') }}
              </span>

              <span v-else>
                {{ t('component.common.no-results') }}
              </span>
            </div>
          </template>
        </n-select>
      </n-form-item>
    </template>
  </FormField>
</template>

<script setup lang="ts" generic="T extends NamedResourceDTO">
import {computed, inject, onBeforeUnmount, onMounted, ref, watch} from "vue";
import type {NamedResourceDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import {NFormItem, NSelect, SelectOption} from "naive-ui";
import useInfiniteScrollSearch from "@/composables/useInfiniteScrollSearch";
import type OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import FormField from "@/components/common/forms/FormField.vue";

//#region PUBLIC
const selectedValue = defineModel<string | string[] | null>('selected');

interface Props{
  /** Method to fetch the next page of elements */
  fetchPage: (query: string, page: number, pageSize: number) => Promise<HttpResponse<OpenSilexResponse<T[]>>>
  conversionMethod?: (dto: T) => SelectOption
  /** The pageSize to use in the fetchPage call */
  resultLimit?: number,
  placeholder?: string,
  multiple?: boolean,
  /** Chemin du champ dans le modèle du NForm parent, utilisé pour la validation */
  path?: string,
  label?: string,
  helpMessage?: string,
  /** Astérisque rouge */
  required?: boolean,
  /** Étoile bleue */
  requiredBlue?: boolean,
  /** Method to load pre-selected elements, example for some update Form */
  itemLoadingMethod?: (uris: string[]) => Promise<NamedResourceDTO[] | undefined>
}
const props = withDefaults(defineProps<Props>(), {multiple: false});

const emit = defineEmits(['handlingEnterKey', 'selectionChange']);
//#endregion

//#region Constants
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const PAGE_SIZE = 20;
const { t } = useI18n();
//#endregion

//#region Reactive Data
/**
 * Options resolved through `itemLoadingMethod` for values that are already selected but absent
 * from the search results (typically an update form arriving with URIs but no labels).
 */
const preselectedOptions = ref<SelectOption[]>([]);

// Guards against out-of-order resolutions if the selection changes while a load is in flight.
let resolveRequestId = 0;
//#endregion

//#region Functions
/**
 * Single mapping used by both the search results and the pre-selected items: both paths must
 * produce the exact same option `value` for a given element, or the selection won't match.
 */
function toOption(dto: T): SelectOption {
  return props.conversionMethod
    ? props.conversionMethod(dto)
    : { label: dto.name, value: dto.uri };
}

/**
 * Normalised comparison key
 */
function uriKey(value: unknown): string {
  return opensilex.getShortUri(String(value));
}
//#endregion

const {
  options,
  isLoading,
  hasMoreResults,
  totalCount,
  displayedCount,
  search: onSearch,
  reload,
  onScroll: onDropdownScroll,
  dispose
} = useInfiniteScrollSearch<T, SelectOption, string | number>({
  pageSize: () => props.resultLimit ?? PAGE_SIZE,
  fetchPage: (query, pageIndex, pageSize) =>
    props.fetchPage(query, pageIndex, pageSize).then(resp => ({
      result: resp.response.result ?? [],
      total: resp.response.metadata?.pagination?.totalCount ?? 0,
      hasNext: resp.response.metadata?.pagination?.hasNextPage
    })),
  mapItem: toOption,
  getOptionKey: (option) => option.value as string | number,
  onError: (error) => opensilex.errorHandler(error)
});

//#region Computed
/**
 * What the select actually renders: the search results, plus any pre-selected option they don't
 * already cover. Deduplicated on the normalised URI so an element fetched by both paths appears once.
 */
const displayedOptions = computed<SelectOption[]>(() => {
  const preselectedByKey = new Map(preselectedOptions.value.map(option => [uriKey(option.value), option]));
  const merged: SelectOption[] = [];
  const usedKeys = new Set<string>();

  // Search results keep their server order. When an element was also resolved as pre-selected, the
  // pre-selected one wins: its `value` is spelled the way the parent model spells it, which is what
  // the select matches against.
  for (const option of options.value) {
    const key = uriKey(option.value);
    if (usedKeys.has(key)) {
      continue;
    }
    usedKeys.add(key);
    merged.push(preselectedByKey.get(key) ?? option);
  }

  for (const [key, option] of preselectedByKey) {
    if (!usedKeys.has(key)) {
      usedKeys.add(key);
      merged.push(option);
    }
  }

  return merged;
});
//#endregion

//#region Watchers
/**
 * Resolves labels for values that are selected but missing from the loaded options. Watched rather
 * than done once on mount, because an update form often receives its value *after* this component
 * has mounted.
 */
watch(
  selectedValue,
  async (newSelection) => {
    if (!props.itemLoadingMethod) {
      return;
    }

    const selectedUris = (Array.isArray(newSelection) ? newSelection : [newSelection])
      .filter((uri): uri is string => typeof uri === 'string' && uri.length > 0);

    if (selectedUris.length === 0) {
      preselectedOptions.value = [];
      return;
    }

    const selectedKeys = new Set(selectedUris.map(uriKey));

    // Drop what is no longer selected, so this list can't grow indefinitely.
    const stillSelected = preselectedOptions.value.filter(option => selectedKeys.has(uriKey(option.value)));
    if (stillSelected.length !== preselectedOptions.value.length) {
      preselectedOptions.value = stillSelected;
    }

    const knownKeys = new Set([
      ...options.value.map(option => uriKey(option.value)),
      ...stillSelected.map(option => uriKey(option.value))
    ]);
    const urisToLoad = selectedUris.filter(uri => !knownKeys.has(uriKey(uri)));

    // Nothing missing: no request at all. Without this the selector would call the API on every
    // selection change.
    if (urisToLoad.length === 0) {
      return;
    }

    const currentRequestId = ++resolveRequestId;

    try {
      const loadedDtos = await props.itemLoadingMethod(urisToLoad) ?? [];

      if (currentRequestId !== resolveRequestId) {
        return;
      }

      const resolved = loadedDtos.map(dto => {
        const option = toOption(dto as unknown as T);
        // Key the option on the URI as the parent model spells it, otherwise the select can't
        // match the value it was given.
        const requestedUri = urisToLoad.find(uri => uriKey(uri) === uriKey(dto.uri));
        return requestedUri !== undefined ? { ...option, value: requestedUri } : option;
      });

      preselectedOptions.value = [...preselectedOptions.value, ...resolved];
    } catch (error) {
      opensilex.errorHandler(error);
    }
  },
  { immediate: true }
);
//#endregion

//#region Hooks
onMounted(() => {
  reload()
})

onBeforeUnmount(() => {
  dispose()
})
//#endregion
</script>

<style scoped lang="scss">
.infinite-scroll-selector-footer {
  text-align: center;
}

/* Le composant sélection prend toute la largeur dispo */
:deep(.n-base-selection) {
  width: 100%;
}
</style>
