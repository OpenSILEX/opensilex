<template>
  <opensilex-SelectForm
      ref="selectForm"
      label="GermplasmControlledAttributesSelector.label"
      :selected.sync="propertyURI"
      :multiple="false"
      :options="existingRdfAttributes"
      :itemLoadingMethod="load"
      :clearable="true"
      :required="required"
      placeholder="GermplasmControlledAttributesSelector.placeholder"
      @clear="$emit('clear')"
      @select="$emit('select')"
  ></opensilex-SelectForm>
</template>


<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {OntologyService} from "opensilex-core/api/ontology.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import {Prop, PropSync, Ref} from 'vue-property-decorator';
import {SelectableItem} from "../../../components/common/forms/SelectForm.vue";
import SelectForm from "@/components/common/forms/SelectForm.vue";
@Component({})
export default class GermplasmControlledAttributesSelector extends Vue {


  @Ref("selectForm") readonly selectForm!: SelectForm;

  $opensilex: OpenSilexVuePlugin;

  service: OntologyService;

  @Prop()
  existingRdfAttributes: Array<SelectableItem>;

  @Prop()
  required: boolean;

  @PropSync("property")
  propertyURI: string;

  created() {
    this.service = this.$opensilex.getService("opensilex.OntologyService");
  }

  load(properties: Array<string>) {
    return this.service.getURILabelsList(properties).then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
      return (http && http.response) ? http.response.result : undefined
    }).catch(this.$opensilex.errorHandler);

  }
}
</script>

<style scoped lang="scss">

</style>

<i18n>

en:
  GermplasmControlledAttributesSelector:
    placeholder: Select a property
    label: Existing property

fr:
  GermplasmControlledAttributesSelector:
    placeholder: Sélectionner une propriété
    label: Propriété existante

</i18n>