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
                    {{ $t("OntologyObjectProperties.inverse-property-title", { name: selected.name}) }}
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

<script lang="ts">
import Vue from "vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {VueJsOntologyExtensionService, VueRDFTypeDTO, VueRDFTypePropertyDTO} from "../../lib";
import {RDFObjectRelationDTO} from "../../../../../opensilex-core/front/src/lib";
import Component from "vue-class-component";
import {Prop, Watch} from "vue-property-decorator";
import {NamedResourceDTO} from "../../../../../opensilex-core/front/src/lib";

export interface PropertyDetail {
    detail: VueRDFTypePropertyDTO,
    values: Array<RDFObjectRelationDTO>
}

/**
 * Render relations / custom properties as fields, to be used in a "description" or "details" card
 */
@Component
export default class OntologyObjectProperties extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $vueOntologyService: VueJsOntologyExtensionService;

    propertyMap: Map<string, PropertyDetail> = new Map<string, PropertyDetail>();
    incomingPropertyMap: Map<string, PropertyDetail> = new Map<string, PropertyDetail>();

    incomingPropertiesOpen: boolean = false;

    /**
     * The object concerned by the relations
     */
    @Prop({
        required: true
    })
    selected: NamedResourceDTO;

    /**
     * The supertype of the object (e.g. "vocabulary:ScientificObject")
     */
    @Prop({
        required: true
    })
    parentType: string;

    /**
     * The array of relations. Can be different from selected.relations
     */
    @Prop({
        required: true
    })
    relations: Array<RDFObjectRelationDTO>;

    /**
     * Properties to ignore during render
     */
    @Prop({
        default: () => []
    })
    ignoredProperties: Array<string>;

    /**
     * Additional props to pass to field components
     */
    @Prop({
        default: () => {}
    })
    additionalFieldProps: { [key: string]: string };

    /**
     * Show or hide incoming properties
     */
    @Prop({
        default: true
    })
    showIncoming: boolean;

    created() {
        this.$vueOntologyService = this.$opensilex.getService<VueJsOntologyExtensionService>("opensilex.VueJsOntologyExtensionService");
        this.reload();
    }

    @Watch("relations")
    async reload() {
        const http = await this.$vueOntologyService.getRDFTypeProperties(this.selected.rdf_type, this.parentType)
            .catch(this.$opensilex.errorHandler);
        const typeModel = http.response.result;

        this.propertyMap = this.buildPropertyMap(typeModel, this.relations, false, this.ignoredProperties);
        this.incomingPropertyMap = this.buildPropertyMap(typeModel, this.relations, true, this.ignoredProperties);
    }

    private buildPropertyMap(typeModel: VueRDFTypeDTO, relations: Array<RDFObjectRelationDTO>, incoming: boolean, ignore: Array<string> = []): Map<string, PropertyDetail> {
        const propertyDefinitions = [...typeModel.object_properties, ...typeModel.data_properties];
        let map = new Map<string, PropertyDetail>();
        relations
            .filter(relation => relation.inverse === incoming)
            .filter(relation => !ignore.some(excludedUri => this.$opensilex.checkURIs(excludedUri, relation.property)))
          //For now filter out unknown properties (can only happen on incoming properties)
          //@todo make the service getRDFTypeProperties also return the incoming properties
            .filter(relation => propertyDefinitions.some(definition => this.$opensilex.checkURIs(definition.uri, relation.property)))
            .forEach(relation => {
                if (!map.has(relation.property)) {
                    map.set(relation.property, {
                        detail: propertyDefinitions.find(definition => this.$opensilex.checkURIs(definition.uri, relation.property)),
                        values: []
                    });
                }
                map.get(relation.property).values.push(relation);
            });
        return map;
    }
}
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

<i18n>
en:
    OntologyObjectProperties:
        inverse-property-title: "Properties targetting {name}"
fr:
    OntologyObjectProperties:
        inverse-property-title: "Propriétés ciblant {name}"
</i18n>