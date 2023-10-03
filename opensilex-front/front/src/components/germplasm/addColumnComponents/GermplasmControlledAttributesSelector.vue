<template>
  <opensilex-SelectForm
      ref="selectForm"
      label="GermplasmControlledAttributesSelector.label"
      :selected.sync="propertyURI"
      :multiple="false"
      :options="existingRdfAttributes"
      :clearable="true"
      placeholder="GermplasmControlledAttributesSelector.placeholder"
      @clear="selected"
      @select="cleared"
  ></opensilex-SelectForm>
</template>


<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {OntologyService} from "opensilex-core/api/ontology.service";
import Oeso from "../../..//ontologies/Oeso";
import {RDFTypeDTO} from "opensilex-core/model/rDFTypeDTO";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import {Prop, PropSync, Ref} from 'vue-property-decorator';
import { ResourceTreeDTO } from 'opensilex-core/index';
import {SelectableItem} from "../../../components/common/forms/SelectForm.vue";
@Component({})
export default class GermplasmControlledAttributesSelector extends Vue {

/*:searchMethod="searchProperties"
:itemLoadingMethod="load"
:conversionMethod="propertyToSelectNode"
*/


  @Ref("selectForm") readonly selectForm!: any;

  $opensilex: OpenSilexVuePlugin;

  service: OntologyService;

  @PropSync("pickedOne")
  havePickedOne: boolean;

  @Prop()
  existingRdfAttributes: Array<SelectableItem>;

  @PropSync("property")
  propertyURI: string;

  created() {
    this.service = this.$opensilex.getService("opensilex.OntologyService");
  }

  setDisableSelector(disabled:boolean){
    this.selectForm.disabled = disabled;
  }

  selected(){
    this.pickedOne = true;
    this.$emit('select');
  }
  cleared(){
    this.pickedOne = false;
    this.$emit('clear');
  }

  searchProperties() {
    return this.service.getSubPropertiesOf(Oeso.GERMPLASM_TYPE_URI, Oeso.HAS_PARENT_GERMPLASM, false).then(http => {
      return http;
    }).catch(this.$opensilex.errorHandler);

  }

  propertyToSelectNode(dto: ResourceTreeDTO) {
    return {
      id: dto.uri,
      label: dto.name
    };
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