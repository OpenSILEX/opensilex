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
    
    import {EntityCreationDTO} from "opensilex-core/index";
    import EntityCreate from "./EntityCreate.vue";

    @Component
    export default class EntityFormExternalReferences extends Vue {

        selectedOntologies: string[] = EntityCreate.selectedOntologies;

        @PropSync("form")
        entityDto: EntityCreationDTO;

    }
</script>

<style scoped>

</style>