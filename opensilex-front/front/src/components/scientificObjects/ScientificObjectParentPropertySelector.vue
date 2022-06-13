<template>
    <div>
        <opensilex-SelectForm
            :label="property.name"
            :selected.sync="internalValue"
            :multiple="property.is_list"
            :required="property.is_required"
            :searchMethod="searchParents"
            :itemLoadingMethod="getParentsByURI"
            placeholder="ScientificObjectParentPropertySelector.parent-placeholder"
        ></opensilex-SelectForm>
    </div>
</template>

<script lang="ts">
import {
    Component,
    Prop,
    PropSync,
} from "vue-property-decorator";
import Vue from "vue";
import Oeso from "../../ontologies/Oeso";
import {ScientificObjectsService} from "opensilex-core/api/scientificObjects.service";

@Component
export default class ScientificObjectParentPropertySelector extends Vue {
    $opensilex: any;

    @Prop()
    property;

    @PropSync("value")
    internalValue;

    @Prop()
    context: { experimentURI: string };

    @Prop({default: () => new Set<string>()})
    excluded: Set<string>

    service: ScientificObjectsService;

    created(){
        this.service = this.$opensilex.getService("opensilex.ScientificObjectsService");
    }
    getContextURI() {
        if (this.context && this.context.experimentURI) {
            return this.context.experimentURI;
        } else {
            return undefined;
        }
    }

    searchParents(query, page, pageSize) {

        let types: Array<string>;

        // use property type if defined
        if (this.property) {
            // don't call OS search API with vocabulary:ScientificObject type, since the API return object with exact type match.
            // But in this case, if the property has vocabulary:ScientificObject, we want all OS, not only the OS with the generic type
            if (! Oeso.checkURIs(this.property.target_property, Oeso.SCIENTIFIC_OBJECT_TYPE_URI)) {
                types = [this.property.target_property];
            }
        }

        return this.service.searchScientificObjects(
                this.getContextURI(),
                types,
                query,
                undefined,
                undefined,
                undefined,
                undefined,
                undefined,
                undefined,
                [],
                page,
                pageSize
            )
            .then((http) => {
                let nodeList = [];
                for (let so of http.response.result) {

                    if(! this.excluded.has(so.uri)){
                        nodeList.push({
                            id: so.uri,
                            label: so.name + " (" + so.rdf_type_name + ")",
                        });
                    }

                }
                http.response.result = nodeList;
                return http;
            });
    }

    getParentsByURI(soURIs) {
        let contextURI = undefined;
        if (this.context && this.context.experimentURI) {
            contextURI = this.context.experimentURI;
        }
        return this.$opensilex
            .getService("opensilex.ScientificObjectsService")
            .getScientificObjectsListByUris(contextURI, soURIs)
            .then((http) => {

                let nodeList = [];
                for (let so of http.response.result) {
                    nodeList.push({
                        id: so.uri,
                        label: so.name + " (" + so.rdf_type_name + ")",
                    });
                }
                return nodeList;
            });
    }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
    ScientificObjectParentPropertySelector:
        label: Facilities
        placeholder: Select a facility
        parent-placeholder: Select a scientific object

fr:
    ScientificObjectParentPropertySelector:
        label: Installation environnementale
        placeholder: Sélectionner une installation environnementale
        parent-placeholder: Sélectionner un objet scientifique


</i18n>