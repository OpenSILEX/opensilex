<template>
    <ValidationObserver ref="characteristicValidatorRef">
        <opensilex-ExternalReferencesForm
                :references.sync="form"
                :displayInsertButton="false"
                :ontologiesToSelect="selectedOntologies"
        ></opensilex-ExternalReferencesForm>
    </ValidationObserver>
</template>

<script lang="ts">

    import {Component, PropSync} from "vue-property-decorator";
    import Vue from "vue";
    import CharacteristicModalForm from "./CharacteristicModalForm.vue";
    // @ts-ignore
    import { CharacteristicCreationDTO } from "opensilex-core/index";

    @Component
    export default class CharacteristicExternalReferencesForm extends Vue {

        selectedOntologies: string[] = CharacteristicModalForm.selectedOntologies;

        @PropSync("form")
        dto: CharacteristicCreationDTO;

    }
</script>

<style scoped>

</style>