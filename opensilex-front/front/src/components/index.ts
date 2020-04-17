import DefaultFooterComponent from './layout/DefaultFooterComponent.vue';
import DefaultHeaderComponent from './layout/DefaultHeaderComponent.vue';
import DefaultLoginComponent from './layout/DefaultLoginComponent.vue';
import DefaultMenuComponent from './layout/DefaultMenuComponent.vue';
import DefaultHomeComponent from './layout/DefaultHomeComponent.vue';
import DefaultNotFoundComponent from './layout/DefaultNotFoundComponent.vue';
import UserForm from './users/UserForm.vue';
import UserList from './users/UserList.vue';
import UserView from './users/UserView.vue';

import ProfileForm from './profiles/ProfileForm.vue';
import ProfileView from './profiles/ProfileView.vue';
import ProfileList from './profiles/ProfileList.vue';
import GroupForm from './groups/GroupForm.vue';
import GroupUserProfileForm from './groups/GroupUserProfileForm.vue';
import GroupView from './groups/GroupView.vue';
import GroupList from './groups/GroupList.vue';

import ToDoComponent from './layout/ToDoComponent.vue';
// infrastructures
import InfrastructureView from './infrastructures/InfrastructureView.vue';
import InfrastructureTree from './infrastructures/InfrastructureTree.vue';
import InfrastructureForm from './infrastructures/InfrastructureForm.vue';
import InfrastructureDeviceForm from './infrastructures/InfrastructureDeviceForm.vue';

// factors levels
import FactorView from './factors/FactorView.vue';
import FactorList from './factors/FactorList.vue';
import FactorForm from './factors/FactorForm.vue';
import FactorDetails from './factors/FactorDetails.vue';

// factors
import FactorLevelView from './factors/FactorLevelView.vue';
import FactorLevelList from './factors/FactorLevelList.vue';
import FactorLevelForm from './factors/FactorLevelForm.vue';
//common
import FormInputLabelHelper from './common/FormInputLabelHelper.vue'
import ExternalReferencesForm from './common/ExternalReferencesForm.vue'
import ExternalReferencesDetails from './common/ExternalReferencesDetails.vue'
import PageHeader from './common/PageHeader.vue'

import ExperimentForm from './experiments/form/ExperimentForm.vue';
import ExperimentForm2 from './experiments/form/ExperimentForm2.vue';
import ExperimentForm3 from './experiments/form/ExperimentForm3.vue';
import UserListSelector from './experiments/form/UserListSelector.vue';
import ProjectsListSelector from './experiments/form/ProjectsListSelector.vue';
import GroupsListSelector from './experiments/form/GroupsListSelector.vue';
import InfrastructuresListSelector from './experiments/form/InfrastructuresListSelector.vue';


import ExperimentList from './experiments/ExperimentList.vue';
import ExperimentCreate from './experiments/ExperimentCreate.vue';
import ExperimentView from './experiments/ExperimentView.vue';

import GermplasmView from './germplasm/GermplasmView.vue';
import GermplasmForm from './germplasm/GermplasmForm.vue';
import GermplasmList from './germplasm/GermplasmList.vue';

import ImageList from './phis/images/ImageList.vue';
import ImageView from './phis/images/ImageView.vue';
import ImageSearch from './phis/images/ImageSearch.vue';
import ImageGrid from './phis/images/viewComponents/ImageGrid.vue';
import ImageSingle from './phis/images/viewComponents/ImageSingle.vue';
import ImageCarousel from './phis/images/viewComponents/ImageCarousel.vue';
import SciObjectURISearch from './phis/images/searchComponents/SciObjectURISearch.vue';
import SciObjectAliasSearch from './phis/images/searchComponents/SciObjectAliasSearch.vue';
import ImageTypeSearch from './phis/images/searchComponents/ImageTypeSearch.vue';
import ExperimentSearch from './phis/images/searchComponents/ExperimentSearch.vue';
import SciObjectTypeSearch from './phis/images/searchComponents/SciObjectTypeSearch.vue';
import TimeSearch from './phis/images/searchComponents/TimeSearch.vue';

export default {
    // Common layout
    'opensilex-DefaultFooterComponent': DefaultFooterComponent,
    'opensilex-DefaultHeaderComponent': DefaultHeaderComponent,
    'opensilex-DefaultLoginComponent': DefaultLoginComponent,
    'opensilex-DefaultMenuComponent': DefaultMenuComponent,
    'opensilex-DefaultHomeComponent': DefaultHomeComponent,
    'opensilex-DefaultNotFoundComponent': DefaultNotFoundComponent,

    // Common components
    'opensilex-FormInputLabelHelper': FormInputLabelHelper,
    'opensilex-ExternalReferencesForm': ExternalReferencesForm,
    'opensilex-ExternalReferencesDetails': ExternalReferencesDetails,
    'opensilex-PageHeader': PageHeader,
    'opensilex-ToDoComponent': ToDoComponent,

    // Security
    'opensilex-UserForm': UserForm,
    'opensilex-UserList': UserList,
    'opensilex-UserView': UserView,

    'opensilex-ProfileForm': ProfileForm,
    'opensilex-ProfileView': ProfileView,
    'opensilex-ProfileList': ProfileList,
    'opensilex-GroupForm': GroupForm,
    'opensilex-GroupUserProfileForm': GroupUserProfileForm,
    'opensilex-GroupView': GroupView,
    'opensilex-GroupList': GroupList,

    // Experiments
    'opensilex-ExperimentForm': ExperimentForm,
    'opensilex-ExperimentForm2': ExperimentForm2,
    'opensilex-ExperimentForm3' : ExperimentForm3,
    'opensilex-ExperimentList': ExperimentList,
    'opensilex-ExperimentCreate': ExperimentCreate,
    'opensilex-ExperimentView': ExperimentView,
    'opensilex-UserListSelector': UserListSelector,
    'opensilex-ProjectsListSelector': ProjectsListSelector,
    'opensilex-GroupsListSelector': GroupsListSelector,
    'opensilex-InfrastructuresListSelector': InfrastructuresListSelector,

    

    // Germplasm
    'opensilex-GermplasmView': GermplasmView,
    'opensilex-GermplasmForm': GermplasmForm,
    'opensilex-GermplasmList': GermplasmList,

    // Images
    'opensilex-ImageList': ImageList,
    'opensilex-ImageView': ImageView,
    'opensilex-ImageSearch': ImageSearch,
    'opensilex-ImageGrid': ImageGrid,
    'opensilex-ImageSingle': ImageSingle,
    'opensilex-ImageCarousel': ImageCarousel,
    'opensilex-SciObjectURISearch': SciObjectURISearch,
    'opensilex-SciObjectAliasSearch': SciObjectAliasSearch,
    'opensilex-ImageTypeSearch': ImageTypeSearch,
    'opensilex-ExperimentSearch': ExperimentSearch,
    'opensilex-SciObjectTypeSearch': SciObjectTypeSearch,
    'opensilex-TimeSearch': TimeSearch,

    // Infrastructures
    "opensilex-InfrastructureView": InfrastructureView,
    "opensilex-InfrastructureTree": InfrastructureTree,
    "opensilex-InfrastructureForm": InfrastructureForm,
    "opensilex-InfrastructureDeviceForm": InfrastructureDeviceForm,

    // Factors
    "opensilex-FactorView": FactorView,
    "opensilex-FactorList": FactorList,
    "opensilex-FactorForm": FactorForm,
    "opensilex-FactorDetails": FactorDetails,

    // Factors levels
    "opensilex-FactorLevelView": FactorLevelView,
    "opensilex-FactorLevelList": FactorLevelList,
    "opensilex-FactorLevelForm": FactorLevelForm

};
