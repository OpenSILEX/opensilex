<template>
    <opensilex-WizardForm
            ref="wizardRef"
            :steps="steps"
            createTitle="VariableForm.add"
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
        $i18n: any;

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
            {component: "opensilex-VariableForm1"}
        ];

        @Prop()
        editMode;

        getEmptyForm() {
            return {
                uri: undefined,
                entity: undefined,
                quality: undefined,
                longName: undefined,
                synonym: undefined,
                label: undefined,
                comment: undefined,
                timeInterval: undefined,
                samplingInterval: undefined,
                dataType: undefined,
                traitUri: undefined,
                traitName: undefined,
                method: undefined,
                unit: undefined
            };
        }

        create(variable) {
            if(variable.dataType && variable.dataType.uri){
                variable.dataType = variable.dataType.uri
            }
            this.service.createVariable(variable).then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                let uri = http.response.result.toString();
                let message = this.$i18n.t("Variables.name") + " " + uri + " " + this.$i18n.t("component.common.success.update-success-message");
                this.$opensilex.showSuccessToast(message);
                this.$emit("onCreate", uri);
            }).catch(this.$opensilex.errorHandler);
        }

        update(variable) {
            if(variable.dataType && variable.dataType.uri){
                variable.dataType = variable.dataType.uri
            }
            variable.entity = variable.entity.uri ? variable.entity.uri : variable.entity;
            variable.quality = variable.quality.uri ? variable.quality.uri : variable.quality;
            variable.method = variable.method.uri ? variable.method.uri : variable.method;
            variable.unit = variable.unit.uri ? variable.unit.uri : variable.unit;

            this.service.updateVariable(variable).then(() => {
                let message = this.$i18n.t("Variables.name") + " " + variable.uri + " " + this.$i18n.t("component.common.success.update-success-message");
                this.$opensilex.showSuccessToast(message);
                this.$emit("onUpdate", variable.uri);
            }).catch(this.$opensilex.errorHandler);
        }
    }
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
    VariableForm:
        add: Add variable
        longName: Alternative name
        entity-help: "Involved object or event. e.g : Leaf, plant, rain fall"
        entity-placeholder: Select an entity
        quality-help: "Define what was measured/observed. e.g: temperature, infection level, weight, area"
        quality-placeholder: Select a quality
        method-placeholder: Select a method
        method-help : How it was measured
        unit-placeholder: Select an unit
        time-interval: Time interval
        time-interval-placeholder: Select an interval
        time-interval-help: Define the time between two data recording
        sampling-interval: Sample interval
        sampling-interval-placeholder: Select an interval
        sampling-interval-help: Granularity of sampling
        synonym: Synonym
        trait-name: Trait name
        trait-name-help: Variable trait name (Describe the trait name if a trait uri has been specified)
        trait-name-placeholder:  Number of grains per square meter
        trait-uri: trait uri
        trait-uri-help: Variable trait unique identifier (Can be used to link to an existing trait for interoperability)
        trait-uri-placeholder: http://purl.obolibrary.org/obo/WTO_0000171
        class-placeholder: Select a type
        dimension-values:
            unique: Unique measurement
            millisecond : Millisecond
            second: Second
            minute: Minutes
            hour: Hour
            day: Day
            week: Week
            month: Month
            mm: Millimeter
            cm: Centimer
            m: Meter
            km: Kilometer
            field: Field
            region: Region

fr:
    VariableForm:
        add: Ajouter une variable
        longName: Nom alternatif
        entity-help: "Objet ou évènement sur lequel porte la mesure/l'observation. ex : Feuille, plante, pluie"
        entity-placeholder: Sélectionnez une entité
        quality-help: "Ce qui est mesurée/observé. ex : Température, taux d'infection, masse, surface"
        quality-placeholder: Sélectionnez une qualité
        method-help: Définit comment la mesure/l'observation a été effectuée.
        method-placeholder: Sélectionnez une méthode
        unit-placeholder: Sélectionnez une unité
        time-interval: Intervalle de temps
        time-interval-placeholder: Selectionnez un intervalle
        time-interval-help: Durée entre deux enregistrement de données
        sampling-interval: Échantillonnage
        sampling-interval-placeholder: Selectionnez un intervalle
        sampling-interval-help: Granularité de l'échantillonage
        synonym : Synonyme
        trait-name: Nom du trait
        trait-name-help: Nom du trait (si une URI décrivant un trait a été saisie)
        trait-name-placeholder:  Nombre de grains par mètre carré
        trait-uri: URI du trait
        trait-uri-help: Identifiant unique d'un trait (Peut être utilisé pour lier cette variable avec l'URI d'un trait existant)
        trait-uri-placeholder: http://purl.obolibrary.org/obo/WTO_0000171
        class-placeholder: Sélectionnez un type
        dimension-values:
            unique: Enregistrement unique
            milliseconde : Milliseconde
            second: Seconde
            minute: Minute
            hour: Heure
            day: Jour
            week: Semaine
            month: Mois
            mm: Millimètre
            cm: Centimètre
            m: Mètre
            km: Kilomètre
            field: Champ
            region: Région
</i18n>

