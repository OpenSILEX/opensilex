<template>
    <opensilex-FormSelector
        v-if="metadataKey"
        :key=metadataKey
        :label="label"
        ref="formSelector"
        :selected.sync="metadataValue"
        :multiple="false"
        :searchMethod="searchAttributeValues"
        :conversionMethod="convert"
        noResultsText="component.experiment.form.selector.filter-search-no-result"
        @clear="$emit('clear')"
        @select="select"
        @deselect="deselect"
    ></opensilex-FormSelector>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, { OpenSilexResponse } from "opensilex-security/HttpResponse";
import {GeneticResourceService} from "opensilex-core/api/geneticResource.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import FormSelector from "../common/forms/FormSelector.vue";

@Component
export default class GeneticResourceAttributesValueSelector extends Vue {

    $opensilex: OpenSilexVuePlugin;

    @PropSync("attributeKey")
    metadataKey: string;

    @PropSync("attributeValue")
    metadataValue: string;

    @Prop({
        default: "GeneticResourceList.filter.metadataValue",
    })
    label;

    service: GeneticResourceService;

    @Ref("formSelector") readonly formSelector!: FormSelector;

    created() {
        this.service = this.$opensilex.getService("opensilex.GeneticResourceService");
    }

    searchAttributeValues(query, page, pageSize) {

        return this.service
            .getGeneticResourceAttributeValues(this.metadataKey, query, page, pageSize)
            .then(
                (http: HttpResponse<OpenSilexResponse<Array<string>>>) => http
            ).catch(this.$opensilex.errorHandler);
    }

    convert(attributeValue: string){
        return {
            id: attributeValue,
            label: attributeValue
        };
    }

    select(value) {
        this.$emit("select", value);
    }

    deselect(value) {
        this.$emit("deselect", value);
    }
}
</script>

<style scoped lang="scss">
</style>