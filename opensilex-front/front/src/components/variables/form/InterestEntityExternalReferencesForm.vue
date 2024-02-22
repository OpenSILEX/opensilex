<template>
    <ValidationObserver ref="interestEntityValidatorRef">
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
    // @ts-ignore
    import {InterestEntityCreationDTO} from "opensilex-core/index";
    import InterestEntityCreate from "./InterestEntityCreate.vue";

    @Component
    export default class InterestEntityExternalReferencesForm extends Vue {

        selectedOntologies: string[] = InterestEntityCreate.selectedOntologies;

        @PropSync("form")
        interestEntityDto: InterestEntityCreationDTO;

    }
</script>

<style scoped>

</style>