<template>
  <opensilex-WizardForm
    ref="wizardRef"
    :steps="steps"
    createTitle="DatasetForm.import"
    icon="fa#vials"
    modalSize="lg"
    :initForm="getEmptyForm"
    :createAction="create"
    :static="false"
  >
    <template v-slot:icon></template>
  </opensilex-WizardForm>
</template>


<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import Oeso from "../../../ontologies/Oeso";
import moment from "moment";

import {
  AgentModel,
  ProvenanceGetDTO,
} from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import { ExperimentGetDTO } from "opensilex-core/index";

@Component
export default class DataForm extends Vue {
  $opensilex: any;

  @Ref("modalRef") readonly modalRef!: any;
  @Ref("validatorRef") readonly validatorRef!: any;

  steps = [
      {component : "opensilex-DataForm1"},
      {component : "opensilex-DataForm2"}
  ];

  @Ref("wizardRef") readonly wizardRef!: any;

  created(){
  }

  showCreateForm() {
      this.wizardRef.showCreateForm();
  }

  showEditForm(form) {
      this.wizardRef.showEditForm(form);
  }   

  create() {
  }

  getEmptyForm() {
    return {
      provenanceURI: undefined,
      experiments: []
      };        
  }

}
</script>
<style scoped lang="scss">
</style>
<i18n>
en:
  DatasetForm:
    create: Add Data
    update: Update
    experiment: Choose experiment
    describe: Describe information involved in the production of data
    provenance: 
      label: Provenance
      operators: Operators
      operators-help: Who have generated these data ?
      name: Name 
      name-search: Data provenance name in this experiment
      name-help: Give a name
      description : Description
      description-help : Other comment(s) about your data environment parameters
      description-placeholder : Add description
      title: Create a new provenance
      title-create : Create a new provenance
      title-use : Use an already existing provenance
      already-exists: Provenance already existing
      success-message : Provenance has been successfully created
    choose-experiment: First choose an experiment
    choosen-experiment: Choosen experiment
    choosen-provenance: Choosen provenance
    selected-file: Selected file
    no-selected-file: No selected file
    csv-file-placeholder: Drop CSV Data file or select a file...
    csv-file-drop-placeholder: Drop CSV Data file here...
    data-duplicated: Duplicated data
    data-not-imported: Data has not been imported. An error has occured during the importation process
    error: Erreur 
    message: Message
    reset-file: Reset file
    dataFile : Import data CSV
    
fr:
  DatasetForm:
    create: Ajouter des données
    update: Modifier
    experiment: Choisir une experimentation
    describe: Décrivez les informations impliquées dans la production des données
    provenance:
      label: Provenance
      operators: Operateurs
      agent-help: Qui a produit ces données ?
      name: Nom 
      name-search: Nom de la provenance des données de l'expérimentation
      name-help: Nommez votre provenance
      description: Description
      description-help: Autres commentaires à propos de vos données ?
      description-placeholder: Ajouter une description
      title-create: Créer la provenance des données
      title-use: Utilisation de la provenance des données
      already-exists: La provenance existe déjà
      success-message : La provenance a été créé avec succès
    choose-experiment: Choisissez une expérimentation
    choosen-experiment: Expérimentation choisie
    choosen-provenance: Provenance choisie
    selected-file: Fichier sélectionné
    no-selected-file: Aucun fichier sélectionné
    csv-file-placeholder: Déposez le CSV de données ici ou sélectionner un fichier..
    csv-file-drop-placeholder: Déposez le CSV de données ici ...    
    data-duplicated: Données dupliquées
    data-not-imported: Données non importées. Une erreur s'est produite durant le processus d'importation
    error: Error
    message: Message
    reset-file: Reinitialiser fichier
    dataFile : Importez des données
    
</i18n>
