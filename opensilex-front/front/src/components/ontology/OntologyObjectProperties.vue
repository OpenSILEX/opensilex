<template>
  <div>
    <div>
      <div v-for="[key, property] in propertyMap" :key="key" class="static-field">
            <span class="field-view-title" :class="{'multiple-values': property.detail.is_list}">
                {{ property.detail.name }}
            </span>
        <ul v-if="property.detail.is_list">
          <li
            v-for="(relation, index) in property.values"
            :key="index"
          >
            <component
              :is="property.detail.view_component"
              :value="relation.value"
              v-bind="additionalFieldProps"
            ></component>
          </li>
        </ul>
        <component
          v-else
          :is="property.detail.view_component"
          :value="property.values[0].value"
          v-bind="additionalFieldProps"
        ></component>
      </div>
    </div>
    <div v-if="showIncoming && incomingPropertyMap.size > 0">
      <div
        class="static-field field-view-title incoming-properties-title"
        @click="incomingPropertiesOpen = !incomingPropertiesOpen"
      >
                <span class="mr-3">
                    {{ t("component.ontology.property.properties-targeting-someName", { name: selected.name}) }}
                </span>
        <opensilex-Icon
          :icon="incomingPropertiesOpen ? 'fa#chevron-down' : 'fa#chevron-right'"
        ></opensilex-Icon>
      </div>
      <div
        v-show="incomingPropertiesOpen"
        class="static-field"
      >
        <ul>
          <template
            v-for="[key, property] in incomingPropertyMap"
          >
            <li
              v-for="(relation, index) in property.values"
              :key="key + index"
            >
              <component
                :is="property.detail.view_component"
                :value="relation.value"
                v-bind="additionalFieldProps"
              ></component>
              <span class="inline-property">
                                {{ property.detail.name }}
                            </span>
              <span class="own-object">
                                {{ selected.name }}
                            </span>
            </li>
          </template>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">

import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {VueJsOntologyExtensionService, VueRDFTypeDTO, VueRDFTypePropertyDTO} from "../../lib";
import {NamedResourceDTO, RDFObjectRelationDTO} from "../../../../../opensilex-core/front/src/lib";
import {inject, ref, watch} from "vue";
import {useI18n} from "vue-i18n";

export interface PropertyDetail {
  detail: VueRDFTypePropertyDTO,
  values: Array<RDFObjectRelationDTO>
}

//#region Constants & Services
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const $vueOntologyService: VueJsOntologyExtensionService = $opensilex.getService<VueJsOntologyExtensionService>("opensilex.VueJsOntologyExtensionService");
const { t } = useI18n();
//#endregion

//#region Props
interface Props{
  /** The object concerned by the relations*/
  selected: NamedResourceDTO,
  /** The supertype of the object (e.g. "vocabulary:ScientificObject")*/
  parentType: string,
  /** The array of relations. Can be different from selected.relations*/
  relations: RDFObjectRelationDTO[],
  /** Properties to ignore during render*/
  ignoredProperties?: string[],
  /** Additional props to pass to field components*/
  additionalFieldProps?: { [key: string]: string },
  /** Show or hide incoming properties*/
  showIncoming?: boolean
}

const props = withDefaults(
  defineProps<Props>(),
  {
    ignoredProperties: () => [],
    additionalFieldProps: () => {
      return {};
    },
    showIncoming: true
  }
);
//#endregion

//#region Reactive data
const propertyMap = ref<Map<string, PropertyDetail>>(new Map<string, PropertyDetail>());
const incomingPropertyMap = ref<Map<string, PropertyDetail>>(new Map<string, PropertyDetail>());
const incomingPropertiesOpen = ref<boolean>(false);
//#endregion

//#region Watch Towers
watch(
  props.relations,
  async (newRelationsValue) => {
    const http = await $vueOntologyService.getRDFTypeProperties(props.selected.rdf_type, props.parentType)
      .catch($opensilex.errorHandler);
    const typeModel = http.response.result;

    propertyMap.value = buildPropertyMap(typeModel, props.relations, false, props.ignoredProperties);
    incomingPropertyMap.value = buildPropertyMap(typeModel, props.relations, true, props.ignoredProperties);
  },
  {immediate: true}
);
//#endregion

//#region Functions
/**
 *
 * @param typeModel passed to work out what properties apply to our type
 * @param relations The relations we are using/filtering to build the returned Map
 * @param incoming If true build a Map with only reverse relations, else build Map with only standard relations
 * @param ignore The property Uris to ignore and not place in the returned Map
 *
 * @return A Map detailing properties, in function of the passed params.
 */
function buildPropertyMap(typeModel: VueRDFTypeDTO, relations: Array<RDFObjectRelationDTO>, incoming: boolean, ignore: Array<string> = []): Map<string, PropertyDetail> {
  const propertyDefinitions = [...typeModel.object_properties, ...typeModel.data_properties];
  let map = new Map<string, PropertyDetail>();
  relations
    .filter(relation => relation.inverse === incoming)
    .filter(relation => !ignore.some(excludedUri => $opensilex.compareUris(excludedUri, relation.property)))
    //For now filter out unknown properties (can only happen on incoming properties)
    //@todo make the service getRDFTypeProperties also return the incoming properties
    .filter(relation => propertyDefinitions.some(definition => $opensilex.compareUris(definition.uri, relation.property)))
    .forEach(relation => {
      if (!map.has(relation.property)) {
        map.set(relation.property, {
          detail: propertyDefinitions.find(definition => $opensilex.compareUris(definition.uri, relation.property)),
          values: []
        });
      }
      map.get(relation.property).values.push(relation);
    });
  return map;
}
//#endregion

</script>

<style scoped lang="scss">

.incoming-properties-title {
  width: 100%;
}

.incoming-properties-title:hover {
  cursor: pointer;
  background-color: #eeeeee;
}

.inline-property {
  font-weight: bold;
  margin-right: 20px;
}

.own-object {
  font-style: italic;
}
</style>
