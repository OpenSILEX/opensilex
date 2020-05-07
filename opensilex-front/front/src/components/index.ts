let components = {};

// Layout
import DefaultFooterComponent from './layout/DefaultFooterComponent.vue';
components["opensilex-DefaultFooterComponent"] = DefaultFooterComponent;
import DefaultHeaderComponent from './layout/DefaultHeaderComponent.vue';
components["opensilex-DefaultHeaderComponent"] = DefaultHeaderComponent;
import DefaultLoginComponent from './layout/DefaultLoginComponent.vue';
components["opensilex-DefaultLoginComponent"] = DefaultLoginComponent;
import DefaultMenuComponent from './layout/DefaultMenuComponent.vue';
components["opensilex-DefaultMenuComponent"] = DefaultMenuComponent;
import DefaultHomeComponent from './layout/DefaultHomeComponent.vue';
components["opensilex-DefaultHomeComponent"] = DefaultHomeComponent;
import DefaultNotFoundComponent from './layout/DefaultNotFoundComponent.vue';
components["opensilex-DefaultNotFoundComponent"] = DefaultNotFoundComponent;
import PageHeader from './layout/PageHeader.vue'
components["opensilex-PageHeader"] = PageHeader;
import PageActions from './layout/PageActions.vue'
components["opensilex-PageActions"] = PageActions;
import PageContent from './layout/PageContent.vue'
components["opensilex-PageContent"] = PageContent;
import Overlay from './layout/Overlay.vue'
components["opensilex-Overlay"] = Overlay;
import ToDoComponent from './layout/ToDoComponent.vue';
components["opensilex-ToDoComponent"] = ToDoComponent;

// Users
import UserForm from './users/UserForm.vue';
components["opensilex-UserForm"] = UserForm;
import UserList from './users/UserList.vue';
components["opensilex-UserList"] = UserList;
import UserView from './users/UserView.vue';
components["opensilex-UserView"] = UserView;
import UserSelector from './users/UserSelector.vue';
components["opensilex-UserSelector"] = UserSelector;

// Profiles
import ProfileForm from './profiles/ProfileForm.vue';
components["opensilex-ProfileForm"] = ProfileForm;
import ProfileView from './profiles/ProfileView.vue';
components["opensilex-ProfileView"] = ProfileView;
import ProfileList from './profiles/ProfileList.vue';
components["opensilex-ProfileList"] = ProfileList;

// Groups
import GroupForm from './groups/GroupForm.vue';
components["opensilex-GroupForm"] = GroupForm;
import GroupUserProfileForm from './groups/GroupUserProfileForm.vue';
components["opensilex-GroupUserProfileForm"] = GroupUserProfileForm;
import GroupView from './groups/GroupView.vue';
components["opensilex-GroupView"] = GroupView;
import GroupList from './groups/GroupList.vue';
components["opensilex-GroupList"] = GroupList;
import GroupSelector from './groups/GroupSelector.vue';
components["opensilex-GroupSelector"] = GroupSelector;

// Infrastructures
import InfrastructureView from './infrastructures/InfrastructureView.vue';
components["opensilex-InfrastructureView"] = InfrastructureView;
import InfrastructureTree from './infrastructures/InfrastructureTree.vue';
components["opensilex-InfrastructureTree"] = InfrastructureTree;
import InfrastructureForm from './infrastructures/InfrastructureForm.vue';
components["opensilex-InfrastructureForm"] = InfrastructureForm;
import InfrastructureFacilityForm from './infrastructures/InfrastructureFacilityForm.vue';
components["opensilex-InfrastructureFacilityForm"] = InfrastructureFacilityForm;
import InfrastructureFacilitiesView from './infrastructures/InfrastructureFacilitiesView.vue';
components["opensilex-InfrastructureFacilitiesView"] = InfrastructureFacilitiesView;
import InfrastructureGroupsView from './infrastructures/InfrastructureGroupsView.vue';
components["opensilex-InfrastructureGroupsView"] = InfrastructureGroupsView;
import InfrastructureDetail from './infrastructures/InfrastructureDetail.vue';
components["opensilex-InfrastructureDetail"] = InfrastructureDetail;
import InfrastructureSelector from './infrastructures/InfrastructureSelector.vue';
components["opensilex-InfrastructureSelector"] = InfrastructureSelector;

// Factors
import FactorView from './factors/FactorView.vue';
components["opensilex-FactorView"] = FactorView;
import FactorList from './factors/FactorList.vue';
components["opensilex-FactorList"] = FactorList;
import FactorForm from './factors/FactorForm.vue';
components["opensilex-FactorForm"] = FactorForm;
import FactorDetails from './factors/FactorDetails.vue';
components["opensilex-FactorDetails"] = FactorDetails;
import FactorLevelTable from './factors/FactorLevelTable.vue';
components["opensilex-FactorLevelTable"] = FactorLevelTable;

// Common
import FormInputLabelHelper from './common/forms/FormInputLabelHelper.vue'
components["opensilex-FormInputLabelHelper"] = FormInputLabelHelper;
import FormField from './common/forms/FormField.vue'
components["opensilex-FormField"] = FormField;
import InputForm from './common/forms/InputForm.vue'
components["opensilex-InputForm"] = InputForm;
import TextAreaForm from './common/forms/TextAreaForm.vue'
components["opensilex-TextAreaForm"] = TextAreaForm;
import SelectForm from './common/forms/SelectForm.vue'
components["opensilex-SelectForm"] = SelectForm;
import CheckboxForm from './common/forms/CheckboxForm.vue'
components["opensilex-CheckboxForm"] = CheckboxForm;
import LocalNameInputForm from './common/forms/LocalNameInputForm.vue'
components["opensilex-LocalNameInputForm"] = LocalNameInputForm;
import CSVInputFile from './common/forms/CSVInputFile.vue'
components["opensilex-CSVInputFile"] = CSVInputFile;

import WizardForm from './common/forms/WizardForm.vue'
components["opensilex-WizardForm"] = WizardForm;
import ModalForm from './common/forms/ModalForm.vue'
components["opensilex-ModalForm"] = ModalForm;
import UriForm from './common/forms/UriForm.vue'
components["opensilex-UriForm"] = UriForm;
import TypeForm from './common/forms/TypeForm.vue'
components["opensilex-TypeForm"] = TypeForm;

import StringView from './common/views/StringView.vue'
components["opensilex-StringView"] = StringView;
import DateView from './common/views/DateView.vue'
components["opensilex-DateView"] = DateView;

import UriView from './common/views/UriView.vue'
components["opensilex-UriView"] = UriView;
import UriLink from './common/views/UriLink.vue'
components["opensilex-UriLink"] = UriLink;
import TypeView from './common/views/TypeView.vue'
components["opensilex-TypeView"] = TypeView;
import Icon from './common/views/Icon.vue'
components["opensilex-Icon"] = Icon;
import TreeView from './common/views/TreeView.vue'
components["opensilex-TreeView"] = TreeView;
import TableAsyncView from './common/views/TableAsyncView.vue'
components["opensilex-TableAsyncView"] = TableAsyncView;

import ExternalReferencesForm from './common/external-references/ExternalReferencesForm.vue'
components["opensilex-ExternalReferencesForm"] = ExternalReferencesForm;
import ExternalReferencesDetails from './common/external-references/ExternalReferencesDetails.vue'
components["opensilex-ExternalReferencesDetails"] = ExternalReferencesDetails;

import StringFilter from './common/filters/StringFilter.vue'
components["opensilex-StringFilter"] = StringFilter;

import Button from './common/buttons/Button.vue'
components["opensilex-Button"] = Button;
import CreateButton from './common/buttons/CreateButton.vue'
components["opensilex-CreateButton"] = CreateButton;
import EditButton from './common/buttons/EditButton.vue'
components["opensilex-EditButton"] = EditButton;
import DetailButton from './common/buttons/DetailButton.vue'
components["opensilex-DetailButton"] = DetailButton;
import DeleteButton from './common/buttons/DeleteButton.vue'
components["opensilex-DeleteButton"] = DeleteButton;
import AddChildButton from './common/buttons/AddChildButton.vue'
components["opensilex-AddChildButton"] = AddChildButton;

// Experiments
import ExperimentForm from './experiments/form/ExperimentForm.vue';
components["opensilex-ExperimentForm"] = ExperimentForm;
import ExperimentForm1 from './experiments/form/ExperimentForm1.vue';
components["opensilex-ExperimentForm1"] = ExperimentForm1;
import ExperimentForm2 from './experiments/form/ExperimentForm2.vue';
components["opensilex-ExperimentForm2"] = ExperimentForm2;
import ExperimentList from './experiments/ExperimentList.vue';
components["opensilex-ExperimentList"] = ExperimentList;
import ExperimentView from './experiments/ExperimentView.vue';
components["opensilex-ExperimentView"] = ExperimentView;

// Germplasm
import GermplasmView from './germplasm/GermplasmView.vue';
components["opensilex-GermplasmView"] = GermplasmView;
import GermplasmForm from './germplasm/GermplasmForm.vue';
components["opensilex-GermplasmForm"] = GermplasmForm;
import GermplasmList from './germplasm/GermplasmList.vue';
components["opensilex-GermplasmList"] = GermplasmList;

// Projects
import ProjectForm from './projects/ProjectForm.vue';
components["opensilex-ProjectForm"] = ProjectForm;
import ProjectList from './projects/ProjectList.vue';
components["opensilex-ProjectList"] = ProjectList;
import ProjectView from './projects/ProjectView.vue';
components["opensilex-ProjectView"] = ProjectView;

import CoordinatorsProjectForm from './projects/CoordinatorsProjectForm.vue';
components["opensilex-CoordinatorsProjectForm"] = CoordinatorsProjectForm;
import ScientificContactsProjectForm from './projects/ScientificContactsProjectForm.vue';
components["opensilex-ScientificContactsProjectForm"] = ScientificContactsProjectForm;
import AdminContactsProjectForm from './projects/AdminContactsProjectForm.vue';
components["opensilex-AdminContactsProjectForm"] = AdminContactsProjectForm;

// PHIS
import ImageList from './phis/images/ImageList.vue';
components["opensilex-ImageList"] = ImageList;
import ImageView from './phis/images/ImageView.vue';
components["opensilex-ImageView"] = ImageView;
import ImageSearch from './phis/images/ImageSearch.vue';
components["opensilex-ImageSearch"] = ImageSearch;
import ImageGrid from './phis/images/viewComponents/ImageGrid.vue';
components["opensilex-ImageGrid"] = ImageGrid;
import ImageSingle from './phis/images/viewComponents/ImageSingle.vue';
components["opensilex-ImageSingle"] = ImageSingle;
import ImageCarousel from './phis/images/viewComponents/ImageCarousel.vue';
components["opensilex-ImageCarousel"] = ImageCarousel;
import SciObjectURISearch from './phis/images/searchComponents/SciObjectURISearch.vue';
components["opensilex-SciObjectURISearch"] = SciObjectURISearch;
import SciObjectAliasSearch from './phis/images/searchComponents/SciObjectAliasSearch.vue';
components["opensilex-SciObjectAliasSearch"] = SciObjectAliasSearch;
import ImageTypeSearch from './phis/images/searchComponents/ImageTypeSearch.vue';
components["opensilex-ImageTypeSearch"] = ImageTypeSearch;
import ExperimentSearch from './phis/images/searchComponents/ExperimentSearch.vue';
components["opensilex-ExperimentSearch"] = ExperimentSearch;
import SciObjectTypeSearch from './phis/images/searchComponents/SciObjectTypeSearch.vue';
components["opensilex-SciObjectTypeSearch"] = SciObjectTypeSearch;
import TimeSearch from './phis/images/searchComponents/TimeSearch.vue';
components["opensilex-TimeSearch"] = TimeSearch;

export default components;
