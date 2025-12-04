<template>
  <opensilex-ModalListBuilder
      ref="listBuilderRef"
      :fieldLabel = "$t('GeneticResourceParentsModalFormField.field-label')"
      :modalTitle="$t('GeneticResourceParentsModalFormField.title')"
      :placeholder="$t('GeneticResourceParentsModalFormField.placeholder')"
      :required="required"
      :requiredBlue="requiredBlue"
      :parseSingleLineForTreeselect="parseSingleLineForTreeselect"
      :generateEmptyLine="generateEmptyLine"
      :filterIncompleteLines="filterIncompleteLines"
      :convertLineToOutputObject="convertLineToSingleRelationDto"
      :extraProps="{existingRdfParentProperties: this.existingRdfParentProperties}"
      @validateList="setOutputList"
      lineComponent="opensilex-GeneticResourceParentsModalFormFieldLine"
  >
  </opensilex-ModalListBuilder>
</template>

<script lang="ts">
import { Prop, PropSync, Ref, Component } from 'vue-property-decorator';
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { SelectableItem } from '../common/forms/FormSelector.vue';
import {OntologyService} from "opensilex-core/api/ontology.service";
import {RDFObjectRelationDTO} from "opensilex-core/model/rDFObjectRelationDTO";
import Oeso from "../../ontologies/Oeso";
import {GeneticResourceService} from "opensilex-core/api/geneticResource.service";
import ModalListBuilder from '../common/views/ModalListBuilder.vue';
import {ObjectNamedResourceDTO} from "opensilex-core/model/objectNamedResourceDTO";

export interface GeneticResourceParentsAttributesUsedInFront {
  geneticResource_uri: string,
  parent_type_uri: string,
  geneticResource_label: string,
  id: string,
}

@Component
export default class GeneticResourceParentsModalFormField extends Vue {

  @Ref("listBuilderRef") readonly listBuilderRef!: ModalListBuilder<GeneticResourceParentsAttributesUsedInFront, RDFObjectRelationDTO>;

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
  geneticResourceService: GeneticResourceService;

  setOutputList(outputList: Array<RDFObjectRelationDTO>){
    //TODO see if this doesnt need to be map
    this.rdfObjectRelationDtos = outputList.map(e=>e);
  }

  /**
   * Returns a list of single lines, excluding any that have empty or missing fields.
   * Uses lineListOnLastValidate
   */
  filterIncompleteLines(lineListOnLastValidate: Array<GeneticResourceParentsAttributesUsedInFront>) : Array<GeneticResourceParentsAttributesUsedInFront>{
    return lineListOnLastValidate.filter((singleLine) => {
      return (singleLine.geneticResource_uri) &&
          (singleLine.geneticResource_uri.trim().length > 0) &&
          (singleLine.parent_type_uri) &&
          (singleLine.parent_type_uri.trim().length > 0)
    });
  }

  parseSingleLineForTreeselect(singleLine : GeneticResourceParentsAttributesUsedInFront) : SelectableItem {
    let stringifyedCriteria = singleLine.geneticResource_label
    return {
      id: singleLine.id,
      label: stringifyedCriteria
    };
  }

  convertLineToSingleRelationDto(frontSingleCriteria : GeneticResourceParentsAttributesUsedInFront) : RDFObjectRelationDTO{
    return {
      inverse: false,
      value: frontSingleCriteria.geneticResource_uri,
      property: frontSingleCriteria.parent_type_uri
    }
  }

  generateEmptyLine (lineId: number): GeneticResourceParentsAttributesUsedInFront {
    return {
      geneticResource_uri: "",
      parent_type_uri: "",
      geneticResource_label: "",
      id: lineId.toString()
    }
  }

  async created() {
    this.id = this.$opensilex.generateID();
    this.geneticResourceService = this.$opensilex.getService("opensilex.GeneticResourceService");
    let ontologyService: OntologyService = this.$opensilex.getService("opensilex.OntologyService");
    let existingProperties: Array<ObjectNamedResourceDTO> = await ontologyService.getSubPropertiesOf(Oeso.GENETIC_RESOURCE_TYPE_URI, Oeso.HAS_PARENT_GENETIC_RESOURCE, false).then(http => {
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
      let loadedParents  = (await this.geneticResourceService.getGeneticResourcesByURI(this.rdfObjectRelationDtos.map(rdfObject => rdfObject.value))).response.result;
      let loadedParentLabelsMap : Map<string, string> = new Map<string, string>();
      loadedParents.forEach(geneticResource => loadedParentLabelsMap.set(this.$opensilex.getShortUri(geneticResource.uri), geneticResource.name));
      this.listBuilderRef.resetLineListWithInitialLabels([]);
      for(let relationDto of this.rdfObjectRelationDtos){
        this.listBuilderRef.addNewSingleLineToList(
            {
              geneticResource_uri: relationDto.value,
              parent_type_uri: relationDto.property,
              geneticResource_label: loadedParentLabelsMap.get(this.$opensilex.getShortUri(relationDto.value))
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
  GeneticResourceParentsModalFormField:
    title: Modify parents
    placeholder: Click to modify parents
    field-label: Parents

fr:
  GeneticResourceParentsModalFormField:
    title: Modifier parents
    placeholder: Cliquez pour modifier parents
    field-label: Parents

</i18n>