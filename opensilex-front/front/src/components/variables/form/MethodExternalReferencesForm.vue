<template>
    <ValidationObserver ref="methodValidatorRef">
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
    import MethodCreate from "./MethodCreate.vue";
    // @ts-ignore
    import { MethodCreationDTO } from "opensilex-core/index";

    @Component
    export default class MethodExternalReferencesForm extends Vue {

        selectedOntologies: string[] = MethodCreate.selectedOntologies;

        @PropSync("form")
        dto: MethodCreationDTO;

    }
</script>

<style scoped>

</style>
