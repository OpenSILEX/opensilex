<template>
  <opensilex-FormSelector
    :selected.sync="fundinguri"
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
  ></opensilex-FormSelector>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { ExperimentsService, NamedResourceDTO } from "../../../../../opensilex-core/front/src/lib";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class FundingSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $i18n: any;
  $service: ExperimentsService

  @PropSync("funding")
  fundinguri;

  @Prop({
    default: "FundingSelector.funding",
  })
  label;

  @Prop()
  multiple;

  @Prop()
  helpMessage: string;

  options = [];
  form: any;

  created(){
    this.$service = this.$opensilex.getService("opensilex.ExperimentsService");
    this.searchFunding();
  }

  searchFunding() {
    this.$service.searchFunding(undefined, ["name=asc"], undefined, undefined)
        .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
          if (http && http.response) {
            this.options = [];
            http.response.result.forEach(fundingDto => {
              this.options.push({label: fundingDto.name, id: fundingDto.uri})
            })
          }
        }).catch(this.$opensilex.errorHandler);
  }


  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }

  onEnter() {
    this.$emit("handlingEnterKey")
  }

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