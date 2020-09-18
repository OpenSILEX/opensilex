<template>
    <div class="container-fluid">
        <opensilex-PageHeader
                icon="fa#sun"
                title="component.menu.projects"
                description="ProjectDetails.title"
        ></opensilex-PageHeader>
        <opensilex-NavBar returnTo="/projects">
            <template v-slot:linksLeft>
                <li class="active">
                    <b-button
                            class="mb-2 mr-2"
                            variant="outline-primary"
                    >{{$t('ProjectDetails.title')}}
                    </b-button>
                </li>
            </template>
        </opensilex-NavBar>
        <div class="container-fluid">
            <b-row>
                <b-col>
                    <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">
                        <template v-slot:body>
                            <opensilex-UriView :uri="project.uri" :url="project.uri"></opensilex-UriView>
                            <opensilex-StringView label="component.project.shortname" :value="project.name"></opensilex-StringView>
                            <opensilex-StringView label="component.project.name" :value="project.longName"></opensilex-StringView>
                            <opensilex-TextView label="component.common.description" :value="project.comment"></opensilex-TextView>
                        </template>
                    </opensilex-Card>
                </b-col>
            
            </b-row>
            <b-row>
                <b-col>
                    <opensilex-Card label="ProjectDetails.advanced" icon="ik#ik-clipboard">
                        <template v-slot:body>
                            <opensilex-StringView label="..." :value="project.startDate"></opensilex-StringView>
                        </template>
                    </opensilex-Card>
                </b-col>
              
            </b-row>
        </div>
    </div>
</template>

<script lang="ts">
    import {Component, Prop, PropSync} from "vue-property-decorator";
    import Vue from "vue";
    import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
    import {ProjectsService} from "opensilex-core/api/projects.service";
    import {ProjectGetDetailDTO} from "opensilex-core/model/projectGetDetailDTO";

    @Component
    export default class ProjectDetails extends Vue {
        $opensilex: any;
        $store: any;
        $route: any;
        $t: any;
        $i18n: any;
        service: ProjectsService;

        get user() {
            return this.$store.state.user;
        }

        
        project: ProjectGetDetailDTO = {
            startDate:""
        } ;

        created() {
            this.service = this.$opensilex.getService("opensilex.ProjectsService");
            this.loadVariable(this.$route.params.uri);
        }

        loadVariable(uri: string) {
            console.log("ho");
            this.service.getProject(uri)
                .then((http: HttpResponse<OpenSilexResponse<ProjectGetDetailDTO>>) => {
                    this.project = http.response.result;
                })
                .catch(this.$opensilex.errorHandler);
        }
    }
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    ProjectDetails:
        title: Detailled project view
        advanced: Advanced informations
fr:
    ProjectDetails:
        title: Vue détaillée du projet
        advanced: Informations avancées
</i18n>
