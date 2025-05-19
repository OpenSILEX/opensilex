<template>
  <div class="p-4">
    <!-- <h2 class="text-lg font-bold mb-4">{{ props.editMode ? 'Edit Variable' : 'Create Variable' }}</h2> -->
     <h2 class="text-lg font-bold mb-4">{{ props.editMode ? t('VariableForm.edit') : t('VariableForm.add') }}</h2>
    <n-input v-model:value="form.name" placeholder="Variable name" />
  </div>
</template>

<script setup lang="ts">
import { ref, defineExpose } from 'vue'
import { NInput } from 'naive-ui'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

interface VariableFormModel {
  uri?: string
  name?: string
}

// Déclaration de la prop `editMode` reçue de ModalForm
const props = defineProps<{
  editMode: boolean
}>()

const form = ref<VariableFormModel>({})

// Méthodes internes pour le formulaire
function getEmptyForm(): VariableFormModel {
  return {
    name: '',
  }
}

function showCreateForm() {
  console.log("showCreate")
  form.value = getEmptyForm()
}

function showEditForm(dto: VariableFormModel) {
  console.log("showEdit")
  form.value = { ...dto }
}

function create(): VariableFormModel {
  return form.value
}

function update(): VariableFormModel {
  return form.value
}

function reset() {
  form.value = getEmptyForm()
}

function handleSubmitError(err: any) {
  console.error("Erreur lors de la soumission :", err)
}

// Expose des méthodes pour être utilisées dans ModalForm
defineExpose({
  getEmptyForm,
  showCreateForm,
  showEditForm,
  create,
  update,
  reset,
  handleSubmitError,
  t,
})
</script>

<i18n>
en:
    VariableForm:
        variable: The variable
        add: Add variable
        edit: Edit variable
        altName: Alternative name
        entity-help: "Observed entity or event. e.g : Leaf, canopy, wind"
        entity-placeholder: Search and select an entity
        interestEntity-label: Entity of interest
        interestEntity-help: "Optional, must be provided if its different from the observed entity. It's the entity level that is characterised. e.g : plot, plant, area, genotype..."
        interestEntity-placeholder: Search and select an observation level
        characteristic-help: "Define what is measured/observed. e.g: temperature, infection level, weight, area"
        characteristic-placeholder: Search and select a characteristic
        method-placeholder: Search and select a method
        method-help : How it was measured. If you don't want to specify a method, select the standard method.
        unit-help: "Scale for ordinal variable (such as good, medium, bad...). e.g : kg/m2"
        unit-placeholder: Search and select an unit
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
        no-entity: Unknown entity. Add one with the + button.
        no-interestEntity: Unknown entity of interest. Add one with the + button.
        no-characteristic: Unknown characteristic. Add one with the + button.
        no-method: Unknown method. Add one with the + button.
        no-unit: Unknown unit. Add one with the + button.
        trait-form-create-title: Add trait
        trait-form-edit-title: Edit trait
        trait-button: Trait already existing in an ontology
        trait-button-help: Add a trait (entity and characteristic) already existing in an ontology
        datatype-help: Format of data recorded for this variable. (Can't be updated while they are some data linked to this variable).
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
            year: Year
            mm: Millimeter
            cm: Centimeter
            m: Meter
            km: Kilometer
            field: Field
            region: Region
        already-exist: the variable already exist
        tutorial:
            global: "Create a variable : Before creating a new variable, make sur you check the existing ones in order to avoid duplicates. For example 'grain yield at harvest'."
            entity: "Select the entity that is the object of the observation/measurement. Here 'Grain'."
            entity-check: "If the entity is not already present in the list you can add it. Double check if there is no other spelling - seed, crop, etc."
            entityOfInterest: "Select the entity of interest that is the object of the observation/measurement."
            entityOfInterest-check: "If the entity of interest is not already present in the list you can add it. Double check if there is no other spelling."
            characteristic: "Select the measured characteristic. Here 'Yield' "
            characteristic-check: "If the characteristic is not in the list you can add it. Double check if it is not already present under another name."
            method: "Select the method that is associated with this variable. In our case this is a yield sensor onboard the harvester."
            method-check: "If the method is not present you can add it. Don't neglect the description as it is especially important for methods."
            unit: "Select the unit in which the variable is measured. What should I do if the unit is different from what I have measured ? I can select kg/ha, but my measurements are in t/ha.
                1 - I convert the measurements I have into the appropriate unit.
                2 - I declare a new Unit. This is highly advised to not create too many units and prefer convert into the existing units."
            name: "Precise the variable name. By default this field is auto filled according the entity and characteristic name, but it can be filled manually."
            altName: "Precise the alternative variable name if it exist. By default this field is auto filled according the entity, characteristic, method and unit names, but this field can be filled manually."
            time-interval: "Precise the time interval which associated with this variable. Here we obtained the grain yield each month."
            sampling-interval: "Precise the sample interval which is associated with this variable. Here we obtained the grain yield by harvesting experimental microplot (10m * 2.5m)."
            datatype: "Precise the data type. Here we are using decimal numbers."
            description: "Finalize the variable with some text description of it."
            species: "Select the species that is associated with this variable. Here rice."
        example:
            entity: "Seed"
            characteristic: "Yield"
            method: "Harvest yield sensor"
            unit: "Kilogram per hectare"
            name: "grain_yield"
            altName: "grain_yield_harvest_yield_sensor_kilogram_per_hectare"
            time-interval: "Month"
            sampling-interval: "Meter"
            datatype: "http://www.w3.org/2001/XMLSchema#decimal"
            description: "Grain yield obtained after harvesting an experimental microplot"
            species: "Rice"
fr:
    VariableForm:
        variable: La variable
        add: Ajouter une variable
        edit: Éditer une variable
        altName: Nom alternatif
        entity-help: "Entité observée ou évènement sur lequel porte la mesure/l'observation. ex : Feuille, canopée, vent"
        entity-placeholder: Rechercher et sélectionner une entité
        interestEntity-label: Entité d'intérêt
        interestEntity-help: "Optionnelle, doit être spécifiée si différente de l'entité observée. C'est le niveau d'entité qui est caractérisé. ex : parcelle, plante, zone, génotype..."
        interestEntity-placeholder: Rechercher et sélectionner un niveau d'observation
        characteristic-help: "Ce qui est mesurée/observé. ex : Température, taux d'infection, masse, surface"
        characteristic-placeholder: Rechercher et sélectionner une caractéristique
        method-help: Définir comment la mesure/l'observation a été effectuée. Si vous ne voulez pas spécifier de méthode, veuillez sélectionner la méthode standard.
        method-placeholder: Rechercher et sélectionner une méthode
        unit-help: "Echelle de la variable ordinale (tel que bon, moyen, mauvais...). ex: kg/m2"
        unit-placeholder: Rechercher et sélectionner une unité
        time-interval: Intervalle de temps
        time-interval-placeholder: Sélectionner un intervalle
        time-interval-help: Durée entre deux enregistrements de données
        sampling-interval: Échantillonnage
        sampling-interval-placeholder: Sélectionner un intervalle
        sampling-interval-help: Granularité de l'échantillonage
        synonym : Synonyme
        trait-name: Nom du trait
        trait-name-help: Nom du trait (si une URI décrivant un trait a été saisie)
        trait-name-placeholder:  Nombre de grains par mètre carré
        trait-uri: URI du trait
        trait-uri-help: Identifiant unique d'un trait (Peut être utilisé pour lier cette variable avec l'URI d'un trait existant)
        trait-uri-placeholder: http://purl.obolibrary.org/obo/WTO_0000171
        class-placeholder: Sélectionner un type
        no-entity: Entité inconnue. L'ajouter avec le bouton +.
        no-interestEntity: Entité d'intérêt inconnue. L'ajouter avec le bouton +.
        no-characteristic: Caractéristique inconnue. L'ajouter avec le bouton +.
        no-method: Méthode inconnue. L'ajouter avec le bouton +.
        no-unit: Unité inconnue. L'ajouter avec le bouton +.
        trait-form-create-title: Ajouter un trait
        trait-form-edit-title: Éditer un trait
        trait-button: Trait existant déjà dans une ontologie
        trait-button-help: Ajouter un trait (entité et caractéristique) existant déjà dans une ontologie
        datatype-help: Format des données enregistrées pour cette variable. (Ne peut être mis à jour si des données sont liées à cette variable).
        datatype-placeholder: Sélectionner un type de donnée
        dimension-values:
            unique: Enregistrement unique
            millisecond : Milliseconde
            second: Seconde
            minute: Minute
            hour: Heure
            day: Jour
            week: Semaine
            month: Mois
            year: Année
            mm: Millimètre
            cm: Centimètre
            m: Mètre
            km: Kilomètre
            field: Champ
            region: Région
        already-exist: la variable existe déjà
        tutorial:
            global: "Création de variable : Avant de créer une variable, soyez bien sûr d'avoir vérifié la liste existante pour ne pas introduire de doublon. Par exemple 'Rendement du grain à la récolte'."
            entity: "Sélectionner l'entité sur laquelle la variable est mesurée/observée. Ici le 'grain'."
            entity-check: "Si l'entité n'est pas dans la liste, vous pouvez la créer. Vérifier toutefois des orthographes alternatives - seed, crop, etc."
            entityOfInterest: "Sélectionner l'entité d'intérêt sur laquelle la variable est mesurée/observée."
            entityOfInterest-check: "Si l'entité d'intérêt n'est pas dans la liste, vous pouvez la créer. Vérifier toutefois des orthographes alternatives."
            characteristic: "Sélectionner la caractéristique mesurée. Ici 'rendement'."
            characteristic-check: "Si la caractéristique n'est pas dans la liste, vous pouvez l'ajouter. Vérifier encore une fois que la caractéristique n'est pas présente sous un autre nom."
            method: " Sélectionner la méthode qui vous a permis de réaliser cette variable. Dans notre cas, un capteur embarqué à bord de la moissoneuse-batteuse."
            method-check: "Si la méthode n'est pas présente, vous pouvez l'ajouter. Ne pas oublier de bien renseigner la description, c'est particulièrement important pour la méthode."
            unit: "Sélectionner l'unité dans laquelle est exprimée la variable. Que faire si l'unité proposée ne correspond pas à ma mesure ? On me propose kg/ha, mais j'ai des mesures en t/ha ?
                1 - Je convertie ma variable dans la bonne unité.
                2 - Je crée une nouvelle unité. Il vaut mieux limiter la création de multiples unités, privilégier la conversion."

            name: "Renseigner le nom de cette variable. Par défault ce champ est rempli automatiquement en fonction de l'entité et de la caractéristique, mais il peut être rempli manuellement."
            altName: "Renseigner le nom alternatif de cette variable si il existe. Par défault ce champ est rempli automatiquement en fonction de l'entité, de la caractéristique, de la méthode et de l'unité, mais il peut être rempli manuellement."
            time-interval: "Renseigner le pas-de-temps qui a permis d'obtenir cette variable. Ici le rendement est mesuré chaque mois."
            sampling-interval: "Renseigner l'échantillonnage qui a permis d'obtenir cette variable. Ici on a obtenu le rendement sur une microparcelle expérimentale de taille standard (2.5m*10m)."
            datatype: "Renseigner le type de données. Ici nous avons des nombre décimaux."
            description: "Finaliser la variable avec une description textuelle de la variable."
            species: "Sélectionner l'espèce associée à la variable variable. Ici le 'riz'."
        example:
            entity: "Grain"
            characteristic: "Rendement"
            method: "Capteur de rendement de la moissoneuse-batteuse"
            unit: "Kilogramme par hectare"
            name: "rendement_grain"
            altName: "rendement_grain_capteur_rendement_moissoneuse_batteuse_kilogramme_par_hectare"
            time-interval: "Mois"
            sampling-interval: "Mètre"
            datatype: "http://www.w3.org/2001/XMLSchema#decimal"
            description: "Rendement du grain obtenu après récolte d'une microparcelle expérimentale."
            species: "Riz"
</i18n>

