<template>
    <ValidationObserver ref="unitValidatorRef">
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
    import {UnitCreationDTO} from "opensilex-core/model/unitCreationDTO";
    import {ExternalOntologies} from "../../../models/ExternalOntologies";

    @Component
    export default class UnitExternalReferencesForm extends Vue {

        selectedOntologies: string[] = [
            ExternalOntologies.AGROVOC,
            ExternalOntologies.AGROPORTAL,
            ExternalOntologies.BIOPORTAL,
            ExternalOntologies.QUDT,
            ExternalOntologies.UNIT_OF_MEASURE,
            ExternalOntologies.UNIT_OF_MEASUREMENT,
        ];

        @PropSync("form")
        dto: UnitCreationDTO;

    }
</script>

<style scoped>

</style>