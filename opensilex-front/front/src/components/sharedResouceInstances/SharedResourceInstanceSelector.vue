<template>
  <FormSelector
      :label="label"
      v-model:selected="selectedSriId"
      :optionsLoadingMethod="loadSharedResourceInstances"
      :conversionMethod="sharedResourceInstanceToSelectOption"
      :placeholder="t('component.sharedResourceInstances.local-instance')"
      @clear="emit('clear')"
      @select="select"
      @deselect="deselect"
  ></FormSelector>
</template>

<script lang="ts">
export const LOCAL_INSTANCE_ID = "local_instance";
</script>

<script setup lang="ts">
import {computed, inject} from "vue";
import {useI18n} from "vue-i18n";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {SharedResourceInstanceDTO} from "opensilex-core/model/sharedResourceInstanceDTO";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {OntologyService} from "opensilex-core/api/ontology.service";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import {SelectorItem} from "@/components/common/forms/CustomTreeselect.vue";

//#region public

defineProps<{
  label?: string;
}>();

const emit = defineEmits<{
  select: [value: SharedResourceInstanceDTO | undefined];
  deselect: [value: SelectorItem];
  clear: [];
}>();
//#endregion

//#region private
const $opensilex = inject<OpenSilexVuePlugin>("opensilex")!;
const {t} = useI18n();

/**
 * Represents the URI of the selected SRI.
 */
const selectedSriUri = defineModel<string | undefined>("sharedResourceInstance", {
  default: undefined
});

let sharedResourceInstanceDTOs: Array<SharedResourceInstanceDTO> = [];

const selectedSriId = computed<string>({
  get: () => {
    if (selectedSriUri.value === undefined) {
      return LOCAL_INSTANCE_ID;
    }
    return selectedSriUri.value;
  },
  set: (newId: string) => {
    if (newId === LOCAL_INSTANCE_ID) {
      selectedSriUri.value = undefined;
    } else {
      selectedSriUri.value = newId;
    }
  }
});

function loadSharedResourceInstances(): Promise<Array<Pick<SharedResourceInstanceDTO, "uri" | "label">>> {
  return $opensilex
      .getService<OntologyService>("opensilex.OntologyService")
      .getSharedResourceInstances()
      .then(
          (http: HttpResponse<OpenSilexResponse<Array<SharedResourceInstanceDTO>>>) => {
            sharedResourceInstanceDTOs = http.response.result;

            return [
              ...sharedResourceInstanceDTOs,
              {
                uri: LOCAL_INSTANCE_ID,
                label: t("component.sharedResourceInstances.local-instance")
              }
            ];
          }
      );
}

function sharedResourceInstanceToSelectOption(dto: Pick<SharedResourceInstanceDTO, "uri" | "label">): SelectorItem {
  return {
    id: dto.uri,
    label: dto.label
  };
}

function select(value: SelectorItem) {
  for (let resourceDTO of sharedResourceInstanceDTOs) {
    if (resourceDTO.uri === value.id) {
      emit("select", resourceDTO);
      return;
    }
  }
  emit("select", undefined);
}

function deselect(value: SharedResourceInstanceOption) {
  emit("deselect", value);
}

//#endregion

</script>

<style scoped lang="scss">
</style>
