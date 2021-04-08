<template>
    <ValidationObserver ref="validatorRef">

        <div class="row">
            <div class="col-lg-9">
                <opensilex-GeometryForm
                        :value.sync="form.point"
                        label="Position.coordinates"
                        helpMessage="Position.coordinates-help"
                        placeholder="Position.coordinates-placeholder"
                ></opensilex-GeometryForm>
            </div>

        </div>

        <div class="row">
            <div class="col-lg-3">
                <opensilex-InputForm
                        :value.sync="form.x"
                        label="Position.x"
                        helpMessage="Position.x-help"
                        placeholder="Position.x-placeholder"
                        type="number"
                ></opensilex-InputForm>
            </div>

            <div class="col-lg-3">
                <opensilex-InputForm
                        :value.sync="form.y"
                        label="Position.y"
                        helpMessage="Position.y-help"
                        placeholder="Position.y-placeholder"
                        type="number"
                ></opensilex-InputForm>
            </div>

            <div class="col-lg-3">
                <opensilex-InputForm
                        :value.sync="form.z"
                        label="Position.z"
                        helpMessage="Position.z-help"
                        placeholder="Position.z-placeholder"
                        type="number"
                ></opensilex-InputForm>
            </div>
        </div>

        <div class="row">
            <div class="col">
                <!-- Comment -->
                <opensilex-TextAreaForm
                        :value.sync="form.text"
                        label="Position.textual-position"
                        helpMessage="Position.textual-position-help"
                        placeholder="Position.textual-position-placeholder"
                ></opensilex-TextAreaForm>
            </div>
        </div>

    </ValidationObserver>
</template>

<script lang="ts">
    import {Component, Prop, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {EventCreationDTO} from "opensilex-core/model/eventCreationDTO";
    import {OntologyService} from "opensilex-core/api/ontology.service";
    import {PositionCreationDTO} from "opensilex-core/model/positionCreationDTO";

    @Component
    export default class PositionForm extends Vue {

        @Ref("validatorRef") readonly validatorRef!: any;

        @Prop({default: () => PositionForm.getEmptyForm()})
        form: PositionCreationDTO;

        static getEmptyForm() : PositionCreationDTO{
            return {
              point: undefined,
              x: undefined,
              y: undefined,
              z: undefined,
              text: undefined,
            };
        }

        getEmptyForm() {
            return PositionForm.getEmptyForm();
        }

    }
</script>

<i18n>
en:
    Position:
        title: Position
        coordinates: Coordinates
        coordinates-placeholder: "POINT Z(1 1 80)"
        coordinates-help: "Arrival coordinates at WKT format"
        custom-coordinates: Custom coordinates
        x-help: "1th dimension of a custom coordinate system"
        x: x
        x-placeholder: "10"
        y: y
        y-help: "2nd dimension of a custom coordinate system"
        y-placeholder: "10"
        z: z
        z-placeholder: "10"
        z-help: "3rd dimension of a custom coordinate system"
        textual-position: Textual position
        textual-position-placeholder: Beside the left door
        textual-position-help: Description of the position with text
        from: From
        from-placeholder: dev-infra:greenHouseA
        from-help: Starting facility
        to: To
        to-placeholder: dev-infra:greenHouseB
        to-help: Arrival facility
fr:
    Position:
        title: Position
        coordinates: Coordonnées
        coordinates-placeholder: "POINT Z(1 1 80)"
        coordinates-help: "Coordonnées d'arrivée au format WKT"
        custom-coordinates: "Coordonnées personnalisées"
        x-help: "1ère dimension d'un système de coordonnées"
        x: x
        x-placeholder: "10"
        y: y
        y-help: "2ème dimension d'un système de coordonnées"
        y-placeholder: "10"
        z: z
        z-placeholder: "10"
        z-help: "3ème dimension d'un système de coordonnées"
        textual-position: Positition textuelle
        textual-position-placeholder: "à côté de la porte gauche"
        textual-position-help: Description textuelle de la position
        from: De
        from-placeholder: dev-infra:serreA
        from-help: "Installation technique de départ"
        to: Vers
        to-placeholder: dev-infra:serreB
        to-help: "Installation technique d'arrivée"
</i18n>
