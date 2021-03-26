import Component from "vue-class-component";

 <template>
  <span>
    <opensilex-CreateButton @click="show" label="ExperimentFacilitySelector.set-facilities"></opensilex-CreateButton>
    <b-modal ref="facilitySelector" @ok.prevent="update" size="md" :static="true">
      <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
      <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

      <template v-slot:modal-title>
        <i>
          <slot name="icon">
            <opensilex-Icon icon="fa#eye" class="icon-title" />
          </slot>
          <span>{{$t("ExperimentFacilitySelector.set-facilities")}}</span>
        </i>
      </template>
      <opensilex-SelectForm
        :selected.sync="selected"
        :options="facilititesInfraTree"
        :multiple="true"
        label="ExperimentFacilitySelector.label"
        placeholder="ExperimentFacilitySelector.placeholder"
      >
        <template v-slot:option-label="{node}">
          <opensilex-Icon :icon="$opensilex.getRDFIcon(node.raw.rdf_type)" />&nbsp;
          <span class="capitalize-first-letter">{{node.name}}</span>&nbsp;
          (
          <span class="capitalize-first-letter">{{node.raw.rdf_type_name}}</span>)
        </template>
        <template v-slot:value-label="{node}">
          <opensilex-Icon :icon="$opensilex.getRDFIcon(node.raw.rdf_type_name)" />&nbsp;
          <span class="capitalize-first-letter">{{node.name}}</span>
        </template>
      </opensilex-SelectForm>
    </b-modal>
  </span>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Ref } from "vue-property-decorator";
import Vue from "vue";
import {
  ExperimentsService,
  OrganisationsService,
  ResourceTreeDTO
} from "opensilex-core/index";
@Component
export default class ExperimentFacilitySelector extends Vue {
  $opensilex: any;
  xpService: ExperimentsService;
  infraService: OrganisationsService;

  @Ref("facilitySelector") readonly facilitySelector!: any;

  facilititesInfraTree = [];

  selected = [];

  @Prop()
  uri;

  show() {
    this.facilitySelector.show();
  }

  hide() {
    this.facilitySelector.hide();
  }

  mounted() {
    this.xpService = this.$opensilex.getService(
      "opensilex-core.ExperimentsService"
    );

    this.infraService = this.$opensilex.getService(
      "opensilex-core.OrganisationsService"
    );

    let availableFacilities;
    let facilities;
    let infrastructures;

    this.xpService.getExperiment(this.uri).then(http => {
      let infrastructures = http.response.result.organisations;
      let infrastructuresURI = [];
      infrastructures.forEach((infrastructure) => {
        infrastructuresURI.push(infrastructure.uri);
      });
      Promise.all([
        this.xpService.getAvailableFacilities(this.uri).then(http => {
          availableFacilities = http.response.result;
        }),
        this.xpService.getFacilities(this.uri).then(http => {
          facilities = http.response.result;
        }),
        this.infraService
          .searchInfrastructuresTree(undefined, infrastructuresURI)
          .then(http => {
            infrastructures = http.response.result;
          })
      ]).then(() => {
        let availableFacilitiesByInfra = {};
        for (let i in availableFacilities) {
          let availableFacility = availableFacilities[i];
          let infraURI = availableFacility.infrastructure;
          if (!availableFacilitiesByInfra[infraURI]) {
            availableFacilitiesByInfra[infraURI] = [];
          }

          availableFacilitiesByInfra[infraURI].push(availableFacility);
        }

        let selectedFacilities = {};
        for (let i in facilities) {
          let facility = facilities[i];
          selectedFacilities[facility.uri] = facility;
          this.selected.push(facility.uri);
        }

        let selectorTree = [];
        for (let i in infrastructures) {
          let infrastructure = infrastructures[i];
          let node = this.buildInfrastructureFacilitiesTree(
            infrastructure,
            availableFacilitiesByInfra,
            selectedFacilities
          );

          if (node != null) {
            selectorTree.push(node);
          }
        }

        this.facilititesInfraTree = selectorTree;
      });
    });
  }

  private buildInfrastructureFacilitiesTree(
    dto: ResourceTreeDTO,
    availableFacilitiesByInfra,
    selectedFacilities
  ) {
    let isLeaf = dto.children.length == 0;

    let childrenDTOs = [];
    if (!isLeaf) {
      for (let i in dto.children) {
        let childNode = this.buildInfrastructureFacilitiesTree(
          dto.children[i],
          availableFacilitiesByInfra,
          selectedFacilities
        );

        if (childNode != null) {
          childrenDTOs.push(childNode);
        }
      }
    }

    let facilitiesTree = [];

    if (availableFacilitiesByInfra[dto.uri]) {
      let availableInfraFacilities = availableFacilitiesByInfra[dto.uri];
      for (let i in availableInfraFacilities) {
        let availableInfraFacility = availableInfraFacilities[i];

        let facilityNode = {
          id: availableInfraFacility.uri,
          name: availableInfraFacility.name,
          type: availableInfraFacility.rdf_type,
          rdf_type_name: availableInfraFacility.rdf_type_name
        };

        facilitiesTree.push(facilityNode);
      }
    }

    let node = {
      id: dto.uri,
      name: dto.name,
      isDisabled: true,
      isDefaultExpanded: true,
      children: childrenDTOs.concat(facilitiesTree),
      type: dto.rdf_type,
      rdf_type_name: dto.rdf_type_name
    };

    if (node.children.length == 0) {
      return null;
    }

    return node;
  }

  update() {
  }
}
</script>

<style scoped lang="scss">
::v-deep .vue-treeselect__option--disabled > .vue-treeselect__label-container > .vue-treeselect__checkbox-container {
  display: none;
}
</style>


<i18n>

en:
  ExperimentFacilitySelector:
    set-facilities: Define facilities
    label: experiment facilities

fr:
  ExperimentFacilitySelector:
    set-facilities: Définir les installations techniques
    label: installations techniques
</i18n>
