<template>
  <InfiniteScrollDropdown
      v-model:selected="groupURI"
      :fetchPage="searchGermplasmGroups"
      :itemLoadingMethod="loadGermplasmGroups"
      :label="props.label"
      :multiple="props.multiple"
      :placeholder="placeholder"
      :noResultsText="t('component.groupGermplasm.form.selector.filter-search-no-result')"
      @selectionChange="(option) => emit('selectionChange', option)"
      @clear="emit('clear')"
      @handlingEnterKey="emit('handlingEnterKey')"
  />
</template>

<script setup lang="ts">
import { computed, inject } from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import { GermplasmGroupGetDTO } from "opensilex-core/index";
import type { SelectOption } from "naive-ui";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { GermplasmService } from "opensilex-core/api/germplasm.service";
import InfiniteScrollDropdown from "@/components/common/forms/InfiniteScrollDropdown.vue";
import { useI18n } from "vue-i18n";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const { t } = useI18n();

const groupURI = defineModel<string | string[] | null>("selected");

const props = defineProps<{
  label: string;
  multiple: boolean;
  placeholder?: string;
}>();

const emit = defineEmits<{
  clear: [];
  selectionChange: [option: SelectOption | SelectOption[] | undefined];
  handlingEnterKey: [];
}>();

const placeholder = computed(() => {
  if (props.placeholder) {
    return props.placeholder;
  }
  return props.multiple
      ? t("component.groupGermplasm.form.selector.placeholder-multiple")
      : t("component.groupGermplasm.form.selector.placeholder");
});

function getService(): GermplasmService {
  const service = opensilex?.getService<GermplasmService>(
      "opensilex.GermplasmService"
  );

  if (!service) {
    throw new Error("OpenSilex service is not available");
  }

  return service;
}

/** Loads the already selected elements, so that their name can be displayed. */
async function loadGermplasmGroups(
    uris: string[]
): Promise<GermplasmGroupGetDTO[]> {
  try {
    const http = await getService().searchGermplasmGroupByURIs(uris);

    return http.response.result;
  } catch (error) {
    opensilex?.errorHandler(error);
    return [];
  }
}

/** Loads one page of results. `page` is zero-based. */
function searchGermplasmGroups(
    name: string,
    page: number,
    pageSize: number
): Promise<
    HttpResponse<OpenSilexResponse<GermplasmGroupGetDTO[]>>
> {
  return getService().searchGermplasmGroups(
      name,
      undefined,
      ["name=asc"],
      page,
      pageSize
  );
}
</script>

<style scoped lang="scss">
</style>