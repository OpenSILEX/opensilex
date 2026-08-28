<template>
  <FormSelector
    v-model:selected="fundinguri"
    :options="options"
    :multiple="multiple"
    :label="label"
    placeholder="FundingSelector.form.placeholder.funding"
    @clear="$emit('clear')"
    @select="select"
    @deselect="deselect"
    noResultsText="FundingSelector.form.placeholder.filter-search-no-result"
    :helpMessage="helpMessage"
    @handlingEnterKey="onEnter"
  ></FormSelector>
</template>

<script setup lang="ts">

import OpenSilexVuePlugin from '../../models/OpenSilexVuePlugin';
import HttpResponse, { OpenSilexResponse } from '../../lib/HttpResponse';
import { inject } from 'vue';
import FormSelector from "@/components/common/forms/FormSelector.vue";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import {ExperimentsService} from "opensilex-core/api/experiments.service";

const opensilex = inject<OpenSilexVuePlugin>('opensilex');
const service = inject<ExperimentsService>('service');

const fundinguri = defineModel<string>('fundinguri');

const props = defineProps({
  label: {
    type: String,
    default: 'FundingSelector.funding',
  },
  multiple: {
    type: Boolean,
    default: false,
  },
  helpMessage: {
    type: String,
  },
});
const options = [];

function created() {
  const service = opensilex.getService('opensilex.ExperimentsService');
  searchFunding();
}

function searchFunding() {
  service
    .searchFunding(undefined, ['name=asc'], undefined, undefined)
    .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
      if (http && http.response) {
        const options = [];
        http.response.result.forEach((fundingDto) => {
          options.push({ label: fundingDto.name, id: fundingDto.uri });
        });
      }
    })
    .catch(opensilex.errorHandler);
}

const emit = defineEmits(['update:fundinguri', 'clear', 'select', 'deselect', 'handlingEnterKey']);

function select(value) {
  emit('select', value);
}

function deselect(value) {
  emit('deselect', value);
}

function onEnter() {
  emit('handlingEnterKey');
}
</script>

<i18n>

en:
  FundingSelector: 
    funding : Funding
    form: 
        placeholder:
            funding : Select funding
            filter-search-no-result : No funding found

fr:
  FundingSelector: 
    funding : Financeur
    form: 
        placeholder:
            funding : Selectionner financeurs
            filter-search-no-result : Aucun financeurs trouvés

</i18n>
