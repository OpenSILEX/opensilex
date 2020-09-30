<template>
    <ValidationObserver ref="validatorRef">

        <opensilex-InputForm
            :value.sync="variable.traitUri"
            label="VariableForm.trait-uri"
            type="text"
            helpMessage="VariableForm.trait-uri-help"
            placeholder="VariableForm.trait-uri-placeholder"
            :required.sync="traitRequired"
            @change="updateTraitRequired"
        ></opensilex-InputForm>

        <opensilex-InputForm
            :value.sync="variable.traitName"
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

        this.traitRequired = (this.variable.traitUri && this.variable.traitUri.length > 0) ||
            (this.variable.traitName && this.variable.traitName.length > 0);

        if(! this.traitRequired){
            this.variable.traitName = undefined;
            this.variable.traitUri = undefined;
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