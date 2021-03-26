<template>
    <div>
        <p @click="visible = !visible" style="cursor: pointer">
            <strong>{{ $t("ScientificObjectImportHelp.exceptedFormat") }} </strong>
            <opensilex-Icon v-if="!visible" icon="fa#eye" class="text-primary" />
            <opensilex-Icon v-if="visible" icon="fa#eye-slash" class="text-primary" />
        </p>
        <b-collapse id="collapse-4" v-model="visible" class="mt-2">
            <b-table-simple responsive>
                <b-thead>
                    <b-tr>
                        <b-th>1</b-th>
                        <b-th>URI</b-th>
                        <b-th>type</b-th>
                        <b-th>isInstant<span class="required"> *</span></b-th>
                        <b-th>start</b-th>
                        <b-th>end</b-th>
                        <b-th>Target<span class="required"> *</span></b-th>
                        <b-th>description</b-th>
                        <b-th class="uri-field">From</b-th>
                        <b-th class="uri-field">To</b-th>
                        <b-th>coordinates</b-th>
                        <b-th>x</b-th>
                        <b-th>y</b-th>
                        <b-th>z</b-th>
                        <b-th>TextualPosition</b-th>
                    </b-tr>
                </b-thead>
                <b-tbody>
                    <b-tr>
                        <b-th>2</b-th>
                        <b-td>{{ $t("Move.uri-help") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.type-help") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.is-instant-help") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.start-help") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.end-help") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.target-help") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.description") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.from-help") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.to-help") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.coordinates-help") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.x-help") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.y-help") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.z-help") }}</b-td>
                        <b-td>{{ $t("MoveHelpTableView.textual-position-help") }}</b-td>

                    </b-tr>
                    <b-tr class="alert alert-info">
                        <b-th>3</b-th>
                        <b-td
                                colspan="10"
                                v-html="$t('MoveHelpTableView.text-help', {decimalSeparator: '.', })"
                        ></b-td>
                    </b-tr>
                </b-tbody>
            </b-table-simple>
        </b-collapse>
    </div>
</template>

<script lang="ts">
    import { Component } from "vue-property-decorator";
    import Vue from "vue";

    @Component
    export default class MoveHelpTableView extends Vue {
        $opensilex: any;
        $route: any;
        $store: any;
        $t: any;
        visible: boolean = true;

        getDataTypeLabel(dataTypeUri: string): string {
            if (!dataTypeUri) {
                return undefined;
            }
            let label = this.$t(this.$opensilex.getDatatype(dataTypeUri).labelKey);
            return label.charAt(0).toUpperCase() + label.slice(1);
        }
    }
</script>

<style scoped lang="scss">
    table.b-table-selectable tbody tr td span {
        line-height: 24px;
        text-align: center;
        position: relative;
        margin-bottom: 0;
        vertical-align: top;
    }

    table.b-table-selectable tbody tr td span.checkbox,
    .custom-control.custom-checkbox {
        top: -4px;
        line-height: unset;
    }

    .modal .custom-control-label:after,
    .modal .custom-control-label:before {
        left: 0rem;
    }

    .custom-checkbox {
        padding-left: 12px;
    }

    .modal table.b-table-selectable tbody tr td span.checkbox:after,
    .modal table.b-table-selectable tbody tr td span.checkbox:before {
        left: 0.75rem;
        width: 1rem;
        height: 1rem;
        content: "";
    }

    table.b-table-selectable tbody tr td span.checkbox:after,
    table.b-table-selectable tbody tr td span.checkbox:before {
        position: absolute;
        top: 0.25rem;
        left: 0;
        display: block;
        width: 1rem;
        height: 1rem;
        content: "";
    }

    table.b-table-selectable tbody tr td span.checkbox:before {
        border-radius: 4px;
        pointer-events: none;
        background-color: #fff;
        border: 1px solid #adb5bd;
    }

    table.b-table-selectable tbody tr.b-table-row-selected td span.checkbox:before {
        color: #fff;
        border-color: #007bff;
        background-color: #007bff;
    }

    table.b-table-selectable tbody tr.b-table-row-selected td span.checkbox:after {
        background-image: none;
        content: "\e83f";
        line-height: 16px;
        font-family: "iconkit";
        color: #fff;
    }
</style>


<i18n>
en:
    MoveHelpTableView:
        text-help: "Your can insert your data from this row. <br /> \n
                First two rows of CSV content will be ignored. <br /> \n
                Column orders matter.<br /> \n
                CSV separator is <strong>\",\"</strong><br /> \n
                Decimal separator is  <strong>\"{decimalSeparator}\"</strong><br /> \n
                <br /> \n
                <strong>Blank values will be ignored. <br>
                <strong>Any unknown column identifier will be considered as an error.  <br>"

        type-help: "Move type URI."
        is-instant-help: "Indicate if the move is instantaneous or not (boolean). "
        start-help: "Begin of the move, only if the move is not instantaneous (datetime)."
        end-help: "End of the move, required if the move is instantaneous (datetime)."
        target-help: "URI of the object concerned by the move (The object must exists)."
        description: "Description."
        from-help: "Starting facility URI (The facility must exists)."
        to-help: "Arrival facility URI (The facility must exists)."
        coordinates-help: "Arrival coordinates at WKT format."
        x-help: "1th dimension of a custom coordinate system (number)."
        y-help: "2nd dimension of a custom coordinate system (number)."
        z-help: "3rd dimension of a custom coordinate system (number)."
        textual-position-help: "Description of the position with text."

fr:
    MoveHelpTableView:
        text-help:  "Vous pouvez insérer vos données à partir de cette ligne. <br /> \n
                Les deux premières lignes de contenu CSV seront ignorées. <br /> \n
                L'ordre des colonnes doit être respecté.<br /> \n
                Le séparateur CSV est le suivant :<strong>\",\"</strong><br /> \n
                Le séparateur décimal est le suivant : <strong>\"{decimalSeparator}\"</strong><br /> \n
                <br /> \n
                <strong>Les valeurs vides seront ignorées.<br>
                <strong>Tout identifiant de colonne inconnu sera considéré comme une erreur.<br>"

        type-help: "URI du type de déplacement."
        is-instant-help: "Indique si le déplacement est instantané ou non (booléen);"
        start-help: "Début du déplacement, uniquement si celui-ci n'est pas instantané (datetime)."
        end-help: "Fin du déplacement, requis si celui-ci est instantané. (datetime)."
        target-help: "URI de l'objet deplacé (L'objet doit exister)."
        description: "Description."
        from-help: "URI de l'installation technique de départ (L'installation doit exister)."
        to-help: "URI de l'installation technique d'arrivée (L'installation doit exister)."
        coordinates-help: "Coordonnées d'arrivée au format WKT."
        x-help: "1ère dimension d'un système de coordonnées. (Nombre entier)"
        y-help: "2ème dimension d'un système de coordonnées. (Nombre entier)"
        z-help: "3ème dimension d'un système de coordonnées. (Nombre entier)"
        textual-position-help: "Description textuelle de la position."
</i18n>
