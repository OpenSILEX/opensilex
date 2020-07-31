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
import UserNameView from './users/UserNameView.vue';
components["opensilex-UserNameView"] = UserNameView;


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
import NameInputForm from './common/forms/NameInputForm.vue'
components["opensilex-NameInputForm"] = NameInputForm;
import CSVInputFile from './common/forms/CSVInputFile.vue'
components["opensilex-CSVInputFile"] = CSVInputFile;
import SearchForm from './common/forms/SearchForm.vue'
components["opensilex-SearchForm"] = SearchForm;
import FileInputForm from './common/forms/FileInputForm.vue'
components["opensilex-FileInputForm"] = FileInputForm;
import TagInputForm from './common/forms/TagInputForm.vue'
components["opensilex-TagInputForm"] = TagInputForm;


import WizardForm from './common/forms/WizardForm.vue'
components["opensilex-WizardForm"] = WizardForm;
import ModalForm from './common/forms/ModalForm.vue'
components["opensilex-ModalForm"] = ModalForm;
import UriForm from './common/forms/UriForm.vue'
components["opensilex-UriForm"] = UriForm;
import TypeForm from './common/forms/TypeForm.vue'
components["opensilex-TypeForm"] = TypeForm;

import LabelUriView from './common/views/LabelUriView.vue'
components["opensilex-LabelUriView"] = LabelUriView;
import StringView from './common/views/StringView.vue'
components["opensilex-StringView"] = StringView;
import ListView from './common/views/ListView.vue'
components["opensilex-ListView"] = ListView;
import TextView from './common/views/TextView.vue'
components["opensilex-TextView"] = TextView;
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
import Card from './common/views/Card.vue'
components["opensilex-Card"] = Card;
import NavBar from './common/views/NavBar.vue'
components["opensilex-NavBar"] = NavBar;

import ExternalReferencesModalForm from './common/external-references/ExternalReferencesModalForm.vue'
components["opensilex-ExternalReferencesModalForm"] = ExternalReferencesModalForm;
import ExternalReferencesForm from './common/external-references/ExternalReferencesForm.vue'
components["opensilex-ExternalReferencesForm"] = ExternalReferencesForm;
import ExternalReferencesDetails from './common/external-references/ExternalReferencesDetails.vue'
components["opensilex-ExternalReferencesDetails"] = ExternalReferencesDetails;

import StringFilter from './common/filters/StringFilter.vue'
components["opensilex-StringFilter"] = StringFilter;
import FilterField from './common/filters/FilterField.vue'
components["opensilex-FilterField"] = FilterField;
import SearchFilterField from './common/filters/SearchFilterField.vue'
components["opensilex-SearchFilterField"] = SearchFilterField;

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
import InteroperabilityButton from './common/buttons/InteroperabilityButton.vue'
components["opensilex-InteroperabilityButton"] = InteroperabilityButton;
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
import ExperimentDetail from './experiments/views/ExperimentDetail.vue';
components["opensilex-ExperimentDetail"] = ExperimentDetail;
import ExperimentScientificObjects from './experiments/views/ExperimentScientificObjects.vue';
components["opensilex-ExperimentScientificObjects"] = ExperimentScientificObjects;
import ExperimentModalList from './experiments/ExperimentModalList.vue';
components["opensilex-ExperimentModalList"] = ExperimentModalList;

// Germplasm
import GermplasmView from './germplasm/GermplasmView.vue';
components["opensilex-GermplasmView"] = GermplasmView;
import GermplasmForm from './germplasm/GermplasmForm.vue';
components["opensilex-GermplasmForm"] = GermplasmForm;
import GermplasmList from './germplasm/GermplasmList.vue';
components["opensilex-GermplasmList"] = GermplasmList;
import GermplasmCreate from './germplasm/GermplasmCreate.vue';
components["opensilex-GermplasmCreate"] = GermplasmCreate;
import GermplasmTable from './germplasm/GermplasmTable.vue';
components["opensilex-GermplasmTable"] = GermplasmTable;
import GermplasmDetails from './germplasm/GermplasmDetails.vue';
components["opensilex-GermplasmDetails"] = GermplasmDetails;
import GermplasmModalList from './germplasm/GermplasmModalList.vue';
components["opensilex-GermplasmModalList"] = GermplasmModalList;
import GermplasmAttributesTable from './germplasm/GermplasmAttributesTable.vue';
components["opensilex-GermplasmAttributesTable"] = GermplasmAttributesTable;

// Projects
import ProjectForm from './projects/ProjectForm.vue';
components["opensilex-ProjectForm"] = ProjectForm;
import ProjectList from './projects/ProjectList.vue';
components["opensilex-ProjectList"] = ProjectList;
import ProjectView from './projects/ProjectView.vue';
components["opensilex-ProjectView"] = ProjectView;
import ProjectSelector from './projects/ProjectSelector.vue';
components["opensilex-ProjectSelector"] = ProjectSelector;

// Species
import SpeciesSelector from './species/SpeciesSelector.vue';
components["opensilex-SpeciesSelector"] = SpeciesSelector;
import ProjectModalList from './projects/ProjectModalList.vue';
components["opensilex-ProjectModalList"] = ProjectModalList;

// Scientific Object
import ScientificObjectList from './scientificObjects/ScientificObjectList.vue';
components["opensilex-ScientificObjectList"] = ScientificObjectList;
import ScientificObjectPropertyList from './scientificObjects/ScientificObjectPropertyList.vue';
components["opensilex-ScientificObjectPropertyList"] = ScientificObjectPropertyList;
import ScientificObjectPropertyConfiguration from './scientificObjects/ScientificObjectPropertyConfiguration.vue';
components["opensilex-ScientificObjectPropertyConfiguration"] = ScientificObjectPropertyConfiguration;

// Variables
import VariableList from './variables/VariableList.vue';
components["opensilex-VariableList"] = VariableList;
import VariableView from './variables/VariableView.vue';
components["opensilex-VariableView"] = VariableView;
import VariableDetails from './variables/VariableDetails.vue';
components["opensilex-VariableDetails"] = VariableDetails;

import VariableForm from './variables/form/VariableForm.vue';
components["opensilex-VariableForm"] = VariableForm;
import VariableForm1 from './variables/form/VariableForm1.vue';
components["opensilex-VariableForm1"] = VariableForm1;
import VariableForm2 from './variables/form/VariableForm2.vue';
components["opensilex-VariableForm2"] = VariableForm2;

// Entity
import EntityForm from './variables/form/EntityForm.vue';
components["opensilex-EntityForm"] = EntityForm;
import EntityCreate from './variables/form/EntityCreate.vue';
components["opensilex-EntityCreate"] = EntityCreate;
import EntityExternalReferencesForm from './variables/form/EntityExternalReferencesForm.vue';
components["opensilex-EntityExternalReferencesForm"] = EntityExternalReferencesForm;

// Method
import MethodCreate from './variables/form/MethodCreate.vue';
components["opensilex-MethodCreate"] = MethodCreate;
import MethodForm from './variables/form/MethodForm.vue';
components["opensilex-MethodForm"] = MethodForm;
import MethodExternalReferencesForm from './variables/form/MethodExternalReferencesForm.vue';
components["opensilex-MethodExternalReferencesForm"] = MethodExternalReferencesForm;

//Quality
import QualityForm from './variables/form/QualityForm.vue';
components["opensilex-QualityForm"] = QualityForm;
import QualityCreate from './variables/form/QualityCreate.vue';
components["opensilex-QualityCreate"] = QualityCreate;
import QualityExternalReferencesForm from './variables/form/QualityExternalReferencesForm.vue';
components["opensilex-QualityExternalReferencesForm"] = QualityExternalReferencesForm;

// Unit
import UnitForm from './variables/form/UnitForm.vue';
components["opensilex-UnitForm"] = UnitForm;
import UnitCreate from './variables/form/UnitCreate.vue';
components["opensilex-UnitCreate"] = UnitCreate;
import UnitExternalReferencesForm from './variables/form/UnitExternalReferencesForm.vue';
components["opensilex-UnitExternalReferencesForm"] = UnitExternalReferencesForm;

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
