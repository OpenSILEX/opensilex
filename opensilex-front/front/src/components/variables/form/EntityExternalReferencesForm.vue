<template>
    <ValidationObserver ref="entityValidatorRef">
        <opensilex-ExternalReferencesForm
                :references.sync="form"
                :displayInsertButton="false"
                :ontologiesToSelect="selectedOntologies"
        ></opensilex-ExternalReferencesForm>
    </ValidationObserver>
</template>

<script lang="ts">

    import {Component, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {EntityCreationDTO} from "opensilex-core/model/entityCreationDTO";
    import {ExternalOntologies} from "../../../models/ExternalOntologies";

    @Component
    export default class EntityFormExternalReferences extends Vue {

        selectedOntologies: string[] = [
            ExternalOntologies.AGROVOC,
            ExternalOntologies.AGROPORTAL,
            ExternalOntologies.BIOPORTAL,
            ExternalOntologies.CROP_ONTOLOGY,
            ExternalOntologies.PLANTEOME,
            ExternalOntologies.PLANT_ONTOLOGY
        ];

        @PropSync("form")
        entityDto: EntityCreationDTO;

    }
</script>

<style scoped>

</style>