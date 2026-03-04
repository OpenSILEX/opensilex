<template>
    <ValidationObserver ref="validatorRef">

        <opensilex-UriForm
        :editMode="true"
        :uri.sync="variable.trait"
        label="VariableForm.trait-uri"
        helpMessage="VariableForm.trait-uri-help"
        placeholder="VariableForm.trait-uri-placeholder"
        :required="traitRequired"
        />

        <opensilex-InputForm
        :value.sync="variable.trait_name"
        label="VariableForm.trait-name"
        type="text"
        helpMessage="VariableForm.trait-name-help"
        placeholder="VariableForm.trait-name-placeholder"
        :required="traitRequired"
        />

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
    const trait = (this.variable.trait || "").trim();
    const name = (this.variable.trait_name || "").trim();
    return trait.length > 0 || name.length > 0;
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