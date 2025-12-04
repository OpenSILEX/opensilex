<template>
    <opensilex-FormSelector
        :key="lang"
        ref="formSelector"
        :label="label"
        :selected.sync="geneticResourceAttributeSelected"
        :multiple="false"
        :required="required"
        :optionsLoadingMethod="loadOptions"
        :conversionMethod="convertGeneticResourceAttribute"
        :placeholder="placeholder"
        helpMessage="GeneticResourceAttributesSelector.attribute-name-help"
        @clear="$emit('clear')"
        @select="select"
        @deselect="deselect"
        @keyup.enter.native="onEnter"
    ></opensilex-FormSelector>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {GeneticResourceService} from "opensilex-core/api/geneticResource.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import FormSelector from "../common/forms/FormSelector.vue";

@Component
export default class GeneticResourceAttributesSelector extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $store: any;

    service: GeneticResourceService;

    @PropSync("geneticResourceAttribute")
    geneticResourceAttributeSelected;

    @Prop({
        default: "GeneticResourceAttributesSelector.title",
    })
    label;

    @Prop({
        default: "GeneticResourceAttributesSelector.placeholder",
    })
    placeholder;

    @Prop()
    required;

    created() {
        this.service = this.$opensilex.getService("opensilex.GeneticResourceService");
    }

    get lang() {
        return this.$store.getters.language;
    }

    @Ref("formSelector") readonly formSelector!: FormSelector;

    loadOptions(query, page, pageSize) {
        return this.service
            .getGeneticResourceAttributes()
            .then(
                (http: HttpResponse<OpenSilexResponse<Array<string>>>) =>
                    http.response.result
            ).catch(this.$opensilex.errorHandler);
    }

    convertGeneticResourceAttribute(geneticResourceAttribute: string) {
        return {
            id: geneticResourceAttribute,
            label: geneticResourceAttribute,
        };
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

<style scoped lang="scss">
</style>

<i18n>
en:
    GeneticResourceAttributesSelector:
        title: Attribute
        placeholder: Select an genetic resource attribute
        attribute-name-help: Match all genetic resources which have the selected attribute

fr:
    GeneticResourceAttributesSelector:
        title: Attribut
        placeholder: Sélectionner l'attribut d'une ressource génétique
        attribute-name-help: Sélectionner les ressources génétiques qui possèdent l'attribut selectionné

</i18n>