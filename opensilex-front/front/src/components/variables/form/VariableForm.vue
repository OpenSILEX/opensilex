<template>
    <opensilex-WizardForm
            ref="wizardRef"
            :steps="steps"
            createTitle="Variables.add"
            editTitle="Variables.edit"
            icon="fa#vials"
            modalSize="lg"
            :initForm="getEmptyForm"
            :createAction="create"
            :updateAction="update"
    >
        <template v-slot:icon></template>
    </opensilex-WizardForm>
</template>

<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import HttpResponse, {
        OpenSilexResponse
    } from "opensilex-security/HttpResponse";
    import { VariablesService } from "opensilex-core/index";
    import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";
    import {VariableGetDTO} from "opensilex-core/model/variableGetDTO";

    @Component
    export default class VariableCreate extends Vue {

        $opensilex: any;
        service: VariablesService;

        @Ref("wizardRef") readonly wizardRef!: any;

        created() {
            this.service = this.$opensilex.getService("opensilex.VariablesService");
        }

        showCreateForm() {
            this.wizardRef.showCreateForm();
        }

        showEditForm(form : VariableGetDTO) {
            this.wizardRef.showEditForm(form);
        }

        steps = [
            {component: "opensilex-VariableForm1"},
            {component: "opensilex-VariableForm2"}
        ];

        @Prop()
        editMode;

        getEmptyForm() {
            return {
                uri: undefined,
                entity: {
                    uri: undefined,
                    name: undefined
                },
                quality: {
                    uri: undefined,
                    name: undefined
                },
                longName: undefined,
                synonym: undefined,
                label: undefined,
                comment: undefined,
                dimension: undefined,
                traitUri: undefined,
                traitName: undefined,
                method: {
                    uri: undefined,
                    name: undefined
                },
                unit: {
                    uri: undefined,
                    name: undefined
                },
                lowerBound: undefined,
                upperBound: undefined
            };
        }

        create(variable) {
            this.$emit("onCreate", variable);
        }

        update(variable) {
            this.$emit("onUpdate", variable);
        }
    }
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
    VariableForm:
        longName: long name
        quality-placeholder: Select a quality
        entity-placeholder: Select an entity
        method-placeholder: Select a method
        unit-placeholder: Select an unit
        dimension: integration dimension
        dimension-help: 'The dimension on which variable data are recorded. E.g. :  a sensor which send data each minute, here, the dimension is the time (in minute)'
        dimension-placeholder: Select a dimension
        synonym: Synonym(s)
        trait-name: Trait name
        trait-name-help: Variable trait name (Describe the trait name if a trait uri has been specified)
        trait-name-placeholder:  Number of grains per square meter
        trait-uri: trait uri
        trait-uri-help: Variable trait unique identifier (Can be used to link to an existing trait for interoperability)
        trait-uri-placeholder: http://purl.obolibrary.org/obo/WTO_0000171
        class-placeholder: Select a type
        dimension-values:
                volume: volume
                m3: cubic metre
                liter: liter
                surface: surface
                m2: square metre
                hectare: hectare
                time: time
                second: second
                minute: minutes
                hour: hour
                day: day
                length: length
                cm: cm
                m: m
                km: km
fr:
    VariableForm:
        longName: Nom detaillé
        quality-placeholder: Sélectionnez une qualité,
        entity-placeholder: Sélectionnez une entité
        method-placeholder: Sélectionnez une méthode
        unit-placeholder: Sélectionnez une unité
        dimension: Dimension d'intégration
        dimension-help: Définis la dimension sur laquelle les valeurs d'une variable vont être insérées. Par exemple un capteur qui envoie une donnée chaque minute , ici la dimension est le temps (en minutes)
        dimension-placeholder: Sélectionnez une dimension
        synonym : Synonyme(s)
        trait-name: Nom du trait
        trait-name-help: Nom du trait (si une URI décrivant un trait a étée saisie)
        trait-name-placeholder:  Nombre de grains par mètre carré
        trait-uri: URI du trait
        trait-uri-help: Identifiant unique d'un trait (Peut être utilisé pour lier cette variable avec un trait existant)
        trait-uri-placeholder: http://purl.obolibrary.org/obo/WTO_0000171
        class-placeholder: Sélectionnez un type
    dimension-values:
            volume: volume
            m3: mètre cube
            liter: litre
            surface: surface
            m2: mètre carré
            hectare: hectare
            time: durée
            second: seconde
            minute: minute
            hour: heure
            day: jour
            length: longueur
            cm: cm
            m: m
            km: km
</i18n>

