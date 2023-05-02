<template>
    <opensilex-SelectForm
        :key="lang"
        ref="selectForm"
        :label="label"
        :selected.sync="germplasmAttributeSelected"
        :multiple="false"
        :required="required"
        :optionsLoadingMethod="loadOptions"
        :conversionMethod="convertGermplasmAttribute"
        :placeholder="placeholder"
        helpMessage="GermplasmAttributesSelector.attribute-name-help"
        @clear="$emit('clear')"
        @select="select"
        @deselect="deselect"
        @keyup.enter.native="onEnter"
    ></opensilex-SelectForm>
</template>

<script lang="ts">
import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {GermplasmService} from "opensilex-core/api/germplasm.service";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import SelectForm from "../common/forms/SelectForm.vue";

@Component
export default class GermplasmAttributesSelector extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $store: any;

    service: GermplasmService;

    @PropSync("germplasmAttribute")
    germplasmAttributeSelected;

    @Prop({
        default: "GermplasmAttributesSelector.title",
    })
    label;

    @Prop({
        default: "GermplasmAttributesSelector.placeholder",
    })
    placeholder;

    @Prop()
    required;

    created() {
        this.service = this.$opensilex.getService("opensilex.GermplasmService");
    }

    get lang() {
        return this.$store.getters.language;
    }

    @Ref("selectForm") readonly selectForm!: SelectForm;

    loadOptions(query, page, pageSize) {
        return this.service
            .getGermplasmAttributes()
            .then(
                (http: HttpResponse<OpenSilexResponse<Array<string>>>) =>
                    http.response.result
            ).catch(this.$opensilex.errorHandler);
    }

    convertGermplasmAttribute(germplasmAttribute: string) {
        return {
            id: germplasmAttribute,
            label: germplasmAttribute,
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
    GermplasmAttributesSelector:
        title: Attribute


        placeholder: Select an germplasm attribute
        attribute-name-help: Match all germplasms which have the selected attribute

fr:
    GermplasmAttributesSelector:
        title: Attribut
        placeholder: Sélectionner l'attribut d'une resource génétique
        attribute-name-help: Sélectionner les germplasms qui possèdent l'attribut selectionné

</i18n>