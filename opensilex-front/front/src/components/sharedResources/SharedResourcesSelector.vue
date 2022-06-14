<template>
  <div>
    <opensilex-SelectForm
        :label="label"
        :selected.sync="resourcesURI"
        :multiple="multiple"
        :optionsLoadingMethod="loadSharedResources"
        :conversionMethod="sharedResourcesToSelectNode"
        placeholder="component.sharedResources.selector-placeholder"
        @clear="$emit('clear')"
        @select="select"
        @deselect="deselect"
    ></opensilex-SelectForm>
  </div>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { ProjectGetDTO } from "opensilex-core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import {SharedResourcesDTO} from "opensilex-core/model/sharedResourcesDTO";
import {SpeciesDTO} from "opensilex-core/model/speciesDTO";

@Component
export default class SharedResourcesSelector extends Vue {
  $opensilex: any;

  @PropSync("resources")
  resourcesURI;

  @Prop()
  label;

  @Prop({default: false})
  multiple;

  resourcesList:Array<SharedResourcesDTO>; // liste des ressources partagées dans opensilex.yml

  // fonction servant à renvoyer la liste des ressources (dont locale) dans la liste déroulante pour la remonter au parent SelectForm au dessus
  loadSharedResources() {
    return this.$opensilex // objet du plugin qui permet de recup le service
        .getService("opensilex.OntologyService") // renvoie nom du service
        .getAllSharedResources() // fonction getAllSharedResources de ce service, renvoie une promesse
        .then( // dit ce qu'il se passe (fonction) quand promesse résolue = quand req http finie
            (http: HttpResponse<OpenSilexResponse<Array<SharedResourcesDTO>>>) => { // fonction lambda passée en paramètre du then, anonyme, http = paramètre
              let localResourceDto;
              let selectedDto; // ressource sélectionnée dans le filtre au chargement de la page soit locale si 1ere fois soit ressource sélectionnée avant
              for (let resource of http.response.result) {  // http.response.result = liste des SharedResourcesDTO
                if (resource.isLocal) { // si c'est l'instance locale
                  localResourceDto = resource; // on récupère le dto de cette instance dans la variable localResourceDto
                }
                if (resource.uri === this.resourcesURI) {
                  selectedDto = resource; // récupère dto de la ressource sélectionnée avant rechargement sinon locale tout le temps
                }
              }
              if (!this.resourcesURI) {  // s'il n'y a pas encore de ressource séletionnée (avant pré-sélection auto de la locale)
                this.resourcesURI = localResourceDto.uri; // l'uri sélectionnée est celle de la locale
                selectedDto = localResourceDto; // le dto de la ressource sélectionnée est celui de la locale
              }
              this.$emit("loaded", selectedDto); // emet l'evnt dont le nom est loaded et dont les données assoicées sont selectedDto (ne "renvoie" pas)
              this.resourcesList = http.response.result; // sert à attribuer la valeur liste pour la fonction select après
              return http.response.result; // liste des ressources pour les afficher dans la liste déroulante
            }
        );
  }

  sharedResourcesToSelectNode(dto: SharedResourcesDTO) { // format renvoyé quand on sélectionne ?
    return {
      id: dto.uri,
      label: this.$t(dto.label)
    };
  }

  select(value) { // quand une valeur est sélectionnée --> notifiie dans variableList
    for (let resourceDTO of this.resourcesList){ // boucle sur toutes les ressources
      if (resourceDTO.uri === value.id){ // si l'uri de l'élément est le même que l'uri de la sélection
        this.$emit("select", resourceDTO); // valeur associée = dto sélectionné
      }
    }
  }

  deselect(value) {
    this.$emit("deselect", value);
  }
}
</script>

<style scoped lang="scss">
</style>
