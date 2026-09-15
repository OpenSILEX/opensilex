<template>
  <FormField
    :required="required"
    :requiredBlue="requiredBlue"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template #field="{ id }">
      <!-- NFormItem handles the asterisk, the red border and the message, through the parent NForm rules -->
      <n-form-item :path="path" :show-label="false">
        <div class="select-button-container">
          <n-select
            v-model:value="selectedValue"
            :id="id"
            class="select-main"
            filterable
            clearable
            remote
            :options="displayedOptions"
            :loading="isLoading"
            :placeholder="placeholder"
            :disabled="disabled"
            :consistent-menu-width="false"
            :resetMenuOnOptionsChange="false"
            :multiple="multiple"
            @search="onSearch"
            @scroll="onDropdownScroll"
            @update:value="onSelectionChange"
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

          <div v-if="!actionHandler && viewHandler" class="select-side-button">
            <DetailButton
              @click="viewHandler"
              :label="viewHandlerDetailsVisible ? t('component.common.hide-details') : t('component.common.show-details')"
              :detailVisible="viewHandlerDetailsVisible"
              :small="true"
              class="greenThemeColor"
            />
          </div>

          <div v-else-if="actionHandler" class="select-side-button">
            <n-button class="greenThemeColor" @click="actionHandler">+</n-button>
            <DetailButton
              v-if="viewHandler"
              @click="viewHandler"
              :label="viewHandlerDetailsVisible ? t('component.common.hide-details') : t('component.common.show-details')"
              :detailVisible="viewHandlerDetailsVisible"
              :small="true"
            />
          </div>
        </div>
      </n-form-item>
    </template>
  </FormField>
</template>

<script setup lang="ts" generic="T extends NamedResourceDTO">
import {computed, inject, onBeforeUnmount, onMounted, ref, watch} from "vue";
import type {NamedResourceDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "@/lib/HttpResponse";
import {NButton, NFormItem, NSelect, SelectOption} from "naive-ui";
import useInfiniteScrollSearch from "@/composables/useInfiniteScrollSearch";
import type OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import FormField from "@/components/common/forms/FormField.vue";
import DetailButton from "@/components/common/buttons/DetailButton.vue";

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
  /** Path of the field in the parent NForm model, used for validation */
  path?: string,
  label?: string,
  helpMessage?: string,
  /** Red asterisk */
  required?: boolean,
  /** Blue star */
  requiredBlue?: boolean,
  disabled?: boolean,
  /**
   * Method to load pre-selected elements, example for some update Form. May return synchronously,
   * some existing loaders do.
   */
  itemLoadingMethod?: (uris: string[]) => Promise<NamedResourceDTO[] | undefined> | NamedResourceDTO[] | undefined
  /** Called by the "+" button, to create a new element (usually opens a creation form) */
  actionHandler?: Function,
  /** Called by the details button, to show or hide the details of the selected element */
  viewHandler?: Function,
  /** Whether the details handled by `viewHandler` are currently visible, drives the button label */
  viewHandlerDetailsVisible?: boolean
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

//#region Event handlers
/**
 * Emits the selected option(s) rather than the bare value: callers generally need the label, as the
 * treeselect's `select` event used to provide.
 */
function onSelectionChange(value: string | string[] | null) {
  const optionFor = (uri: string | number) =>
    displayedOptions.value.find(option => uriKey(option.value) === uriKey(uri));

  if (Array.isArray(value)) {
    emit('selectionChange', value.map(optionFor).filter(option => option !== undefined));
    return;
  }

  emit('selectionChange', value !== null && value !== undefined ? optionFor(value) : undefined);
}
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

/** `refresh` re-runs the search from page 0, for callers that must reload on an external change. */
defineExpose({ refresh: reload });
</script>

<style scoped lang="scss">
.infinite-scroll-selector-footer {
  text-align: center;
}

.select-button-container {
  display: flex;
  align-items: stretch;
  width: 100%;
  gap: 0;
  flex-wrap: nowrap;
}

.select-main {
  flex: 1 1 auto;
  min-width: 0;
}

.select-side-button {
  flex: 0 0 auto;
  display: flex;
  align-items: stretch;
  margin-left: 8px;
}

.select-side-button > * {
  height: 100%;
}

.greenThemeColor {
  color: #fff;
}

/* The selection component takes all the available width */
:deep(.n-base-selection) {
  width: 100%;
}
</style>
