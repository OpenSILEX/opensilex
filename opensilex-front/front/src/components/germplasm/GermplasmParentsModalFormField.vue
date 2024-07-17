<template>
  <opensilex-ModalListBuilder
      ref="listBuilderRef"
      :fieldLabel = "$t('GermplasmParentsModalFormField.field-label')"
      :modalTitle="$t('GermplasmParentsModalFormField.title')"
      :placeholder="$t('GermplasmParentsModalFormField.placeholder')"
      :required="required"
      :requiredBlue="requiredBlue"
      :parseSingleLineForTreeselect="parseSingleLineForTreeselect"
      :generateEmptyLine="generateEmptyLine"
      :filterIncompleteLines="filterIncompleteLines"
      :convertLineToOutputObject="convertLineToSingleRelationDto"
      :extraProps="{existingRdfParentProperties: this.existingRdfParentProperties}"
      @validateList="setOutputList"
      lineComponent="opensilex-GermplasmParentsModalFormFieldLine"
  >
  </opensilex-ModalListBuilder>
</template>

<script lang="ts">
import { Prop, PropSync, Ref, Component } from 'vue-property-decorator';
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { SelectableItem } from '../common/forms/SelectForm.vue';
import {OntologyService} from "opensilex-core/api/ontology.service";
import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";
import Oeso from "../../ontologies/Oeso";
import {GermplasmService} from "opensilex-core/api/germplasm.service";
import ModalListBuilder from '../common/views/ModalListBuilder.vue';
import {ObjectNamedResourceDTO} from "opensilex-core/model/objectNamedResourceDTO";

export interface GermplasmParentsAttributesUsedInFront {
  germplasm_uri: string,
  parent_type_uri: string,
  germplasm_label: string,
  id: string,
}

@Component
export default class GermplasmParentsModalFormField extends Vue {

  @Ref("listBuilderRef") readonly listBuilderRef!: ModalListBuilder<GermplasmParentsAttributesUsedInFront, RDFObjectRelationDTO>;

  @Prop({ default: false })
  disabled: boolean;

  @Prop()
  required: boolean;

  @Prop()
  requiredBlue: boolean;

  @PropSync("relation_dtos")
  rdfObjectRelationDtos: Array<RDFObjectRelationDTO>;

  $opensilex: OpenSilexVuePlugin;
  loading : boolean = false;
  id: string = "";
  existingRdfParentProperties: Array<SelectableItem> = [];
  germplasmService: GermplasmService;

  setOutputList(outputList: Array<RDFObjectRelationDTO>){
    //TODO see if this doesnt need to be map
    this.rdfObjectRelationDtos = outputList.map(e=>e);
  }

  /**
   * Returns a list of single lines, excluding any that have empty or missing fields.
   * Uses lineListOnLastValidate
   */
  filterIncompleteLines(lineListOnLastValidate: Array<GermplasmParentsAttributesUsedInFront>) : Array<GermplasmParentsAttributesUsedInFront>{
    return lineListOnLastValidate.filter((singleLine) => {
      return (singleLine.germplasm_uri) &&
          (singleLine.germplasm_uri.trim().length > 0) &&
          (singleLine.parent_type_uri) &&
          (singleLine.parent_type_uri.trim().length > 0)
    });
  }

  parseSingleLineForTreeselect(singleLine : GermplasmParentsAttributesUsedInFront) : SelectableItem {
    let stringifyedCriteria = singleLine.germplasm_label
    return {
      id: singleLine.id,
      label: stringifyedCriteria
    };
  }

  convertLineToSingleRelationDto(frontSingleCriteria : GermplasmParentsAttributesUsedInFront) : RDFObjectRelationDTO{
    return {
      inverse: false,
      value: frontSingleCriteria.germplasm_uri,
      property: frontSingleCriteria.parent_type_uri
    }
  }

  generateEmptyLine (lineId: number): GermplasmParentsAttributesUsedInFront {
    return {
      germplasm_uri: "",
      parent_type_uri: "",
      germplasm_label: "",
      id: lineId.toString()
    }
  }

  async created() {
    this.id = this.$opensilex.generateID();
    this.germplasmService = this.$opensilex.getService("opensilex.GermplasmService");
    let ontologyService: OntologyService = this.$opensilex.getService("opensilex.OntologyService");
    let existingProperties: Array<ObjectNamedResourceDTO> = await ontologyService.getSubPropertiesOf(Oeso.GERMPLASM_TYPE_URI, Oeso.HAS_PARENT_GERMPLASM, false).then(http => {
      return http.response.result;
    }).catch(this.$opensilex.errorHandler);
    existingProperties.forEach(property => {
          this.existingRdfParentProperties.push({
            id: property.uri,
            label: property.name});
        }
    );
  }

  async resetLineListWithInitialLabels(){
    if(Array.isArray(this.rdfObjectRelationDtos) && this.rdfObjectRelationDtos.length > 0){
      let loadedParents  = (await this.germplasmService.getGermplasmsByURI(this.rdfObjectRelationDtos.map(rdfObject => rdfObject.value))).response.result;
      let loadedParentLabelsMap : Map<string, string> = new Map<string, string>();
      loadedParents.forEach(germplasm => loadedParentLabelsMap.set(this.$opensilex.getShortUri(germplasm.uri), germplasm.name));
      this.listBuilderRef.resetLineListWithInitialLabels([]);
      for(let relationDto of this.rdfObjectRelationDtos){
        this.listBuilderRef.addNewSingleLineToList(
            {
              germplasm_uri: relationDto.value,
              parent_type_uri: relationDto.property,
              germplasm_label: loadedParentLabelsMap.get(this.$opensilex.getShortUri(relationDto.value))
            }
        );
      }
      this.listBuilderRef.setOnLastValidateParameters();
    }else{
      this.listBuilderRef.resetLineListWithInitialLabels([this.generateEmptyLine(0)]);
    }

  }

}

</script>

<style scoped lang="scss">

</style>

<i18n>

en:
  GermplasmParentsModalFormField:
    title: Modify parents
    placeholder: Click to modify parents
    field-label: Parents

fr:
  GermplasmParentsModalFormField:
    title: Modifier parents
    placeholder: Cliquez pour modifier parents
    field-label: Parents

</i18n>