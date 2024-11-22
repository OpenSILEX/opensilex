<template>
    <b-modal ref="modalRef" size="lg" hide-footer :static="true">
        <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
        <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>
        <template v-slot:modal-title>{{$t('component.skos.link-external')}}</template>

        <div>
            <opensilex-ExternalReferencesForm
                    :references.sync="skosReferences"
                    :includeAgroportalSearch="includeAgroportalSearch"
                    :displayInsertButton="false"
                    @onUpdate="update"
                    @onAdd="update"
                    @onDelete="update"
            ></opensilex-ExternalReferencesForm>
        </div>

    </b-modal>
</template>

<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import ExternalReferencesForm from "./ExternalReferencesForm.vue";

    @Component
    export default class ExternalReferencesModalForm extends Vue {
        $opensilex: any;
        $store: any;
        $t: any;
        $i18n: any;

        currentRelation: string = "";
        currentExternalUri: string = "";

        @PropSync("references")
        skosReferences: any;

        @Prop({default: false})
        includeAgroportalSearch: boolean;

        @Ref("validatorRef") readonly validatorRef!: any;

        @Ref("modalRef") readonly modalRef!: any;

        ontologiesToSelect: string[] = [];

        show() {
            this.modalRef.show();
        }

        hide() {
            this.modalRef.hide();
        }

        options: any[] = [
            {
                value: "",
                text: "component.skos.no-relation",
                disabled: true
            }
        ];

        created() {
        }

        resetForm() {
            this.currentRelation = "";
            this.currentExternalUri = "";
        }

        resetExternalUriForm() {
            this.currentExternalUri = "";
            this.$nextTick(() => this.validatorRef.reset());
        }

        async update(skosReferences: any) {
            return new Promise((resolve, reject) => {
                this.$emit("onUpdate", skosReferences,result => {
                    if (result instanceof Promise) {
                        result.then(resolve).catch(reject);
                    } else {
                        resolve(result);
                    }
                });
            });
        }
    }
</script>