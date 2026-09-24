<template>
  <FormSelector
      ref="formSelector"
      :label="props.label"
      v-model:selected="groupURI"
      :multiple="props.multiple"
      :searchMethod="searchGermplasmGroups"
      :itemLoadingMethod="loadGermplasmGroups"
      :placeholder="placeholder"
      noResultsText="component.groupGermplasm.form.selector.filter-search-no-result"
      @clear="emit('clear')"
      @select="select"
      @deselect="deselect"
      @keyup.enter="onEnter"
      @loadMoreItems="loadMoreItems"
  />
</template>

<script setup lang="ts">
import { computed, inject, nextTick, ref, useTemplateRef } from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import { GermplasmGroupGetDTO } from "opensilex-core/index";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { GermplasmService } from "opensilex-core/api/germplasm.service";
import FormSelector from "@/components/common/forms/FormSelector.vue";

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");

const pageSize = ref<number>(10);

const groupURI = defineModel<string>("selected");

const props = defineProps<{
  label: string;
  multiple: boolean;
}>();

const emit = defineEmits<{
  clear: [];
  select: [value: GermplasmGroupGetDTO];
  deselect: [value: GermplasmGroupGetDTO];
  handlingEnterKey: [];
}>();

const formSelector =
    useTemplateRef<InstanceType<typeof FormSelector>>("formSelector");

const placeholder = computed(() => {
  return props.multiple
      ? "component.groupGermplasm.form.selector.placeholder-multiple"
      : "component.groupGermplasm.form.selector.placeholder";
});

async function loadGermplasmGroups(
    group: string | string[]
): Promise<GermplasmGroupGetDTO[]> {
  try {
    const service = opensilex?.getService<GermplasmService>(
        "opensilex.GermplasmService"
    );

    if (!service) {
      throw new Error("OpenSilex service is not available");
    }

    const http = await service.searchGermplasmGroupByURIs(group);

    return http.response.result;
  } catch (error) {
    opensilex?.errorHandler(error);
    return [];
  }
}

async function searchGermplasmGroups(
    name: string
): Promise<
    HttpResponse<OpenSilexResponse<GermplasmGroupGetDTO[]>>
> {
  const service = opensilex?.getService<GermplasmService>(
      "opensilex.GermplasmService"
  );

  if (!service) {
    throw new Error("OpenSilex service is not available");
  }

  return await service.searchGermplasmGroups(
      name,
      undefined,
      ["name=asc"],
      0,
      pageSize.value
  );
}

function select(value: GermplasmGroupGetDTO) {
  emit("select", value);
}

function deselect(value: GermplasmGroupGetDTO) {
  emit("deselect", value);
}

function onEnter() {
  emit("handlingEnterKey");
}

async function loadMoreItems() {
  pageSize.value = 0;

  formSelector.value?.refresh();

  await nextTick();

  formSelector.value?.openTreeselect();
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  component:
    groupGermplasm:
      form:
        selector:
          placeholder: Select one germplasm group
          placeholder-multiple: Select one or more germplasm group
          filter-search-no-result: No germplasm group found

fr:
  component:
    groupGermplasm:
      form:
        selector:
          placeholder: Sélectionner un groupe de ressources génétiques
          placeholder-multiple: Sélectionner un ou plusieurs groupes de ressources génétiques
          filter-search-no-result: Aucun groupe de ressources génétiques trouvé
</i18n>