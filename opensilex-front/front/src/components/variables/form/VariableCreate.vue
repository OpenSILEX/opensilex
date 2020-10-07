<template>
    <opensilex-ModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
        ref="variableForm"
        modalSize="lg"
        :tutorial="true"
        component="opensilex-VariableForm"
        createTitle="VariableForm.add"
        editTitle="VariableForm.edit"
        icon="fa#vials"
        :createAction="create"
        :updateAction="update"
        :successMessage="successMessage"
    ></opensilex-ModalForm>

</template>

<script lang="ts">
    import {Component, Prop, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import HttpResponse, {
        OpenSilexResponse
    } from "opensilex-security/HttpResponse";
    import { VariablesService } from "opensilex-core/index";
    import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";
    import {VariableGetDTO} from "opensilex-core/model/variableGetDTO";
    import {VariableCreationDTO} from "opensilex-core/model/variableCreationDTO";
    import ModalForm from "../../common/forms/ModalForm.vue";
    import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
    import {VariableUpdateDTO} from "opensilex-core/model/variableUpdateDTO";

    @Component
    export default class VariableCreate extends Vue {

        $opensilex: OpenSilexVuePlugin;
        $store: any;
        service: VariablesService;
        $i18n: any;

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        @Ref("variableForm") readonly variableForm!: ModalForm;

        created() {
            this.service = this.$opensilex.getService("opensilex.VariablesService");
        }

        showCreateForm() {
            this.variableForm.showCreateForm();
        }

        showEditForm(form : VariableGetDTO) {
            this.variableForm.showEditForm(form);
        }

        @Prop()
        editMode;

        create(variable) {

            if(variable.dataType && variable.dataType.uri){
                variable.dataType = variable.dataType.uri
            }
            this.service.createVariable(variable).then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                this.$emit("onCreate", http.response.result.toString());
            }).catch(this.$opensilex.errorHandler);
        }

        successMessage(variable : VariableCreationDTO){
            return this.$i18n.t("VariableView.name") + " " + variable.name;
        }

        static formatVariableBeforeUpdate(variable) : VariableUpdateDTO{
            if(! variable){
                return undefined;
            }

            let formattedVariable = JSON.parse(JSON.stringify(variable));

            if(formattedVariable.dataType && formattedVariable.dataType.uri){
                formattedVariable.dataType = formattedVariable.dataType.uri
            }
            formattedVariable.entity = formattedVariable.entity.uri ? formattedVariable.entity.uri : formattedVariable.entity;
            formattedVariable.quality = formattedVariable.quality.uri ? formattedVariable.quality.uri : formattedVariable.quality;
            formattedVariable.method = formattedVariable.method.uri ? formattedVariable.method.uri : formattedVariable.method;
            formattedVariable.unit = formattedVariable.unit.uri ? formattedVariable.unit.uri : formattedVariable.unit;

            return formattedVariable;
        }

        update(variable) {
            let formattedVariable = VariableCreate.formatVariableBeforeUpdate(variable);

            this.service.updateVariable(formattedVariable).then(() => {
                this.$emit("onUpdate", formattedVariable.uri);
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
        edit: Edit variable
        altName: Alternative name
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
        no-entity: No entity found
        no-quality: No quality found
        no-method: No method found
        no-unit: No unit found
        trait-button: Trait
        trait-button-placeholder: Add an existing variable trait
        datatype-help: Format of data recorded for this variable
        datatype-placeholder: Select a datatype
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
        edit: Éditer une variable
        altName: Nom alternatif
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
        no-entity: Aucune entité correspondante
        no-quality: Aucune qualité correspondante
        no-method: Aucune méthode correspondante
        no-unit: Aucune unité correspondante
        trait-button: Trait
        trait-button-placeholder: Ajouter un trait existant pour cette variable
        datatype-help: Format des données enregistrées pour cette variable
        datatype-placeholder: Selectionnez un type de donnée
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

