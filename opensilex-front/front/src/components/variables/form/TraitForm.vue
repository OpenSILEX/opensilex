<template>
    <ValidationObserver ref="validatorRef">

        <opensilex-UriForm
            :editMode="true"
            :uri.sync="variable.trait"
            label="VariableForm.trait-uri"
            helpMessage="VariableForm.trait-uri-help"
            placeholder="VariableForm.trait-uri-placeholder"
        ></opensilex-UriForm>

        <opensilex-InputForm
            :value.sync="variable.trait_name"
            label="VariableForm.trait-name"
            type="text"
            helpMessage="VariableForm.trait-name-help"
            placeholder="VariableForm.trait-name-placeholder"
            :required="traitRequired"
        ></opensilex-InputForm>

    </ValidationObserver>

</template>

<script lang="ts">

import {Component, PropSync, Ref} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class TraitForm extends Vue {

    @PropSync("form")
    variable;

    @Ref("validatorRef") readonly validatorRef!: any;

    get traitRequired(): boolean {
        const trait_uri_is_filled = this.variable.trait && this.variable.trait.length >= 0
        const trait_name_is_filled = this.variable.trait_name && this.variable.trait_name.length >= 0
        return trait_uri_is_filled || trait_name_is_filled ;
    }

    reset() {
        return this.validatorRef.reset();
    }

    validate() {
        return this.validatorRef.validate();
    }

}
</script>

<style scoped>

</style>