<template>
    <ValidationObserver ref="validatorRef">

        <opensilex-InputForm
            :value.sync="variable.trait"
            label="VariableForm.trait-uri"
            type="text"
            helpMessage="VariableForm.trait-uri-help"
            placeholder="VariableForm.trait-uri-placeholder"
            :required.sync="traitRequired"
            @change="updateTraitRequired"
        ></opensilex-InputForm>

        <opensilex-InputForm
            :value.sync="variable.trait_name"
            label="VariableForm.trait-name"
            type="text"
            helpMessage="VariableForm.trait-name-help"
            placeholder="VariableForm.trait-name-placeholder"
            :required.sync="traitRequired"
            @change="updateTraitRequired"
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

    traitRequired: boolean = false;

    created() {
        this.updateTraitRequired();
    }

    updateTraitRequired() {

        this.traitRequired = (this.variable.trait && this.variable.trait.length > 0) ||
            (this.variable.trait_name && this.variable.trait_name.length > 0);

        if(! this.traitRequired){
            this.variable.trait_name = undefined;
            this.variable.trait = undefined;
        }
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