<template>

    <b-modal
            ref="modalRef"
            :size="modalSize"
            :static="true"
            no-close-on-backdrop
            no-close-on-esc
            ok-only
            :title="$t('Event.event')"
    >
        <div class="card-body" v-if="event">
            <opensilex-UriView :uri="event.uri"></opensilex-UriView>
            <opensilex-TypeView :type="event.rdf_type" :typeLabel="event.rdf_type_name"></opensilex-TypeView>
            <opensilex-TextView label="component.common.description" :value="event.description"></opensilex-TextView>

            <opensilex-StringView label="Event.start"  v-if="event.start" :value="new Date(event.start).toLocaleString()">
            </opensilex-StringView>

            <opensilex-StringView label="Event.end" v-if="event.end" :value="new Date(event.end).toLocaleString()">
            </opensilex-StringView>

            <opensilex-StringView label="Event.creator" :value="event.author">
            </opensilex-StringView>

          <opensilex-StringView class="overflow-auto" style="height: 100px" label="Event.targets" :uri="event.targets">
                <br>
                <span :key="targets" v-for="(targets) in event.targets">
                <opensilex-UriLink
                    :uri="targets"
                ></opensilex-UriLink>
                </span>
          </opensilex-StringView>

        </div>

        <div v-if="isMove()">
            <opensilex-MoveView :event="event"></opensilex-MoveView>
        </div>

        <div v-if="hasRelations(event)" class="card-body">
            <br>
            <p><b>  {{$t('Event.specific-properties')}} ({{event.rdf_type_name}})</b></p>
            <hr/>

            <div :key="index" v-for="(relation, index) in event.relations">
                <opensilex-TextView
                        :label="getPropertyName(relation.property)"
                        :value="relation.value"
                ></opensilex-TextView>
            </div>
        </div>

        <opensilex-DocumentTabList
            :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
            :uri="event.uri"
            :search=false
        ></opensilex-DocumentTabList>

        <template v-slot:modal-footer>
            <button
                type="button"
                class="btn greenThemeColor"
                v-on:click="hide(false)"
            >
                {{ $t('component.common.ok') }}
            </button>
        </template>


    </b-modal>
</template>

<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {VueJsOntologyExtensionService, VueRDFTypeDTO} from "../../../lib";
    import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
    import { EventDetailsDTO } from 'opensilex-core/index';

    @Component
    export default class EventModalView extends Vue {

        $opensilex: any;
        $vueJsOntologyService: VueJsOntologyExtensionService;

        @Ref("modalRef") readonly modalRef!: any;

        @Prop({default: "lg"})
        modalSize;

        @PropSync("dto")
        event: EventDetailsDTO;

        @PropSync("type")
        eventType: string;

        baseEventType: string;

        eventPropertyByUri: Map<string, VueRDFTypeDTO> = new Map();

        get credentials() {
          return this.$store.state.credentials;
        }

        static getEmptyForm(): EventDetailsDTO {
            return {
                uri: undefined,
                rdf_type: undefined,
                rdf_type_name: undefined,
                relations: [],
                start: undefined,
                end: undefined,
                targets: [],
                description: undefined,
                author: undefined,
                is_instant: true
            };
        }

        created() {

            this.baseEventType = this.$opensilex.Oeev.EVENT_TYPE_URI;
            this.$vueJsOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService")
            this.buildPropertyMap();
        }

        buildPropertyMap() {

            if(! this.eventType || this.$opensilex.Oeev.checkURIs(this.eventType,this.$opensilex.Oeev.EVENT_TYPE_URI)) {
                return;
            }

            this.$vueJsOntologyService.getRDFTypeProperties(this.eventType, this.baseEventType)
                .then((http: HttpResponse<OpenSilexResponse<VueRDFTypeDTO>>) => {
                    let typeModel = http.response.result;

                    this.eventPropertyByUri = new Map();
                    typeModel.data_properties.forEach(property => {
                        this.eventPropertyByUri.set(property.uri, property);
                    });
                    typeModel.object_properties.forEach(property => {
                        this.eventPropertyByUri.set(property.uri, property);
                    });
                }).catch(this.$opensilex.errorHandler);

        }

        show() {
            this.$nextTick(() => {
                this.buildPropertyMap();
                this.modalRef.show();
            });
        }

        getPropertyName(propertyUri: string) {

            if (!propertyUri) {
                return undefined;
            }

            let property = this.eventPropertyByUri.get(propertyUri);
            if (property) {
                return property.name;
            }
            return property;
        }

        hide() {
            this.modalRef.hide();
        }

        isMove(): boolean {
            if (!this.event.rdf_type) {
                return false;
            }

          return this.$opensilex.Oeev.checkURIs(this.event.rdf_type,this.$opensilex.Oeev.MOVE_TYPE_URI)
        }

        hasRelations(event): boolean {
            return event && event.relations && event.relations.length > 0;
        }
    }

</script>

<style scoped lang="scss">
    .icon-title {
        margin-right: 5px;
    }

    ::v-deep .full-screen-modal-form > .modal-dialog {
        max-width: 95%;
    }
</style>;
