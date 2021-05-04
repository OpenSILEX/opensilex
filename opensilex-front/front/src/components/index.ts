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
import ResetPassword from './layout/ResetPassword.vue';
components["opensilex-ResetPassword"] = ResetPassword;
import ForgotPassword from './layout/ForgotPassword.vue';
components["opensilex-ForgotPassword"] = ForgotPassword;


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
import InfrastructureDetailView from './infrastructures/InfrastructureDetailView.vue';
components["opensilex-InfrastructureDetailView"] = InfrastructureDetailView;
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
import InfrastructureFacilitiesTypes from './infrastructures/InfrastructureFacilitiesTypes.vue';
components["opensilex-InfrastructureFacilitiesTypes"] = InfrastructureFacilitiesTypes;
import InfrastructureFacilityPropertyView from './infrastructures/InfrastructureFacilityPropertyView.vue';
components["opensilex-InfrastructureFacilityPropertyView"] = InfrastructureFacilityPropertyView;
import InfrastructureFacilityPropertySelector from './infrastructures/InfrastructureFacilityPropertySelector.vue';
components["opensilex-InfrastructureFacilityPropertySelector"] = InfrastructureFacilityPropertySelector;
import InfrastructureFacilitySelector from './infrastructures/InfrastructureFacilitySelector.vue';
components["opensilex-InfrastructureFacilitySelector"] = InfrastructureFacilitySelector;
import InfrastructureUriView from './infrastructures/InfrastructureUriView.vue';
components["opensilex-InfrastructureUriView"] = InfrastructureUriView;
import InfrastructureFacilityView from './infrastructures/InfrastructureFacilityView.vue';
components["opensilex-InfrastructureFacilityView"] = InfrastructureFacilityView;

// Factors
import FactorView from './experiments/factors/FactorView.vue';
components["opensilex-FactorView"] = FactorView;
import ExperimentFactors from './experiments/views/ExperimentFactors.vue';
components["opensilex-ExperimentFactors"] = ExperimentFactors;
import FactorList from './experiments/factors/FactorList.vue';
components["opensilex-FactorList"] = FactorList;
import FactorForm from './experiments/factors/FactorForm.vue';
components["opensilex-FactorForm"] = FactorForm;
import FactorDetails from './experiments/factors/FactorDetails.vue';
components["opensilex-FactorDetails"] = FactorDetails;
import FactorLevelTable from './experiments/factors/FactorLevelTable.vue';
components["opensilex-FactorLevelTable"] = FactorLevelTable;
import FactorSelector from './experiments/factors/FactorSelector.vue';
components["opensilex-FactorSelector"] = FactorSelector;
import FactorCategorySelector from './experiments/factors/FactorCategorySelector.vue';
components["opensilex-FactorCategorySelector"] = FactorCategorySelector;
import FactorsHelp from './experiments/factors/FactorsHelp.vue';
components["opensilex-FactorsHelp"] = FactorsHelp;
import FactorLevelPropertySelector from './experiments/factors/FactorLevelPropertySelector.vue';
components["opensilex-FactorLevelPropertySelector"] = FactorLevelPropertySelector;
import FactorLevelPropertyView from './experiments/factors/FactorLevelPropertyView.vue';
components["opensilex-FactorLevelPropertyView"] = FactorLevelPropertyView;
import AssociatedExperiments from './experiments/factors/AssociatedExperiments.vue';
components["opensilex-AssociatedExperiments"] = AssociatedExperiments;
import FactorModalList from './experiments/factors/FactorModalList.vue';
components["opensilex-FactorModalList"] = FactorModalList;
import FactorLevelSelector from './experiments/factors/FactorLevelSelector.vue';
components["opensilex-FactorLevelSelector"] = FactorLevelSelector;

// Geometry
import GeometrySelector from './geometry/GeometrySelector.vue';
components["opensilex-GeometrySelector"] = GeometrySelector;

// Common
import FormInputLabelHelper from './common/forms/FormInputLabelHelper.vue'
components["opensilex-FormInputLabelHelper"] = FormInputLabelHelper;
import FormField from './common/forms/FormField.vue'
components["opensilex-FormField"] = FormField;
import InputForm from './common/forms/InputForm.vue'
components["opensilex-InputForm"] = InputForm;
import Tutorial from './common/views/Tutorial.vue'
components["opensilex-Tutorial"] = Tutorial;
import TextAreaForm from './common/forms/TextAreaForm.vue'
components["opensilex-TextAreaForm"] = TextAreaForm;
import SelectForm from './common/forms/SelectForm.vue'
components["opensilex-SelectForm"] = SelectForm;
import CheckboxForm from './common/forms/CheckboxForm.vue'
components["opensilex-CheckboxForm"] = CheckboxForm;
import TranslatedNameInputForm from './common/forms/TranslatedNameInputForm.vue'
components["opensilex-TranslatedNameInputForm"] = TranslatedNameInputForm;
import CSVInputFile from './common/forms/CSVInputFile.vue'
components["opensilex-CSVInputFile"] = CSVInputFile;
import FileInputForm from './common/forms/FileInputForm.vue'
components["opensilex-FileInputForm"] = FileInputForm;
import TagInputForm from './common/forms/TagInputForm.vue'
components["opensilex-TagInputForm"] = TagInputForm;
import IconForm from './common/forms/IconForm.vue'
components["opensilex-IconForm"] = IconForm;
import DateTimeForm from './common/forms/DateTimeForm.vue'
components["opensilex-DateTimeForm"] = DateTimeForm;
import DateForm from './common/forms/DateForm.vue'
components["opensilex-DateForm"] = DateForm;
import GeometryForm from './common/forms/GeometryForm.vue'
components["opensilex-GeometryForm"] = GeometryForm;

import DateRangePickerForm from './common/forms/DateRangePickerForm.vue'
components["opensilex-DateRangePickerForm"] = DateRangePickerForm;

import WizardForm from './common/forms/WizardForm.vue'
components["opensilex-WizardForm"] = WizardForm;
import ModalForm from './common/forms/ModalForm.vue'
components["opensilex-ModalForm"] = ModalForm;
import UriForm from './common/forms/UriForm.vue'
components["opensilex-UriForm"] = UriForm;
import TypeForm from './common/forms/TypeForm.vue'
components["opensilex-TypeForm"] = TypeForm;

import CSVSelectorInputForm from './common/forms/CSVSelectorInputForm.vue'
components["opensilex-CSVSelectorInputForm"] = CSVSelectorInputForm;

import LabelUriView from './common/views/LabelUriView.vue'
components["opensilex-LabelUriView"] = LabelUriView;
import StringView from './common/views/StringView.vue'
components["opensilex-StringView"] = StringView;
import ListView from './common/views/ListView.vue'
components["opensilex-ListView"] = ListView;
import UriListView from './common/views/UriListView.vue'
components["opensilex-UriListView"] = UriListView;

import TextView from './common/views/TextView.vue'
components["opensilex-TextView"] = TextView;
import DateView from './common/views/DateView.vue'
components["opensilex-DateView"] = DateView;
import BooleanView from './common/views/BooleanView.vue'
components["opensilex-BooleanView"] = BooleanView;
import IconView from './common/views/IconView.vue'
components["opensilex-IconView"] = IconView;
import GeometryView from './common/views/GeometryView.vue'
components["opensilex-GeometryView"] = GeometryView;
import GeometryCopy from './common/views/GeometryCopy.vue'
components["opensilex-GeometryCopy"] = GeometryCopy;

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
import TreeViewAsync from './common/views/TreeViewAsync.vue'
components["opensilex-TreeViewAsync"] = TreeViewAsync;
import TableView from './common/views/TableView.vue'
components["opensilex-TableView"] = TableView;
import TableAsyncView from './common/views/TableAsyncView.vue'
components["opensilex-TableAsyncView"] = TableAsyncView;
import Card from './common/views/Card.vue'
components["opensilex-Card"] = Card;

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
import HelpButton from './common/buttons/HelpButton.vue'
components["opensilex-HelpButton"] = HelpButton;
import DeprecatedButton from './common/buttons/DeprecatedButton.vue'
components["opensilex-DeprecatedButton"] = DeprecatedButton;

import OntologyClassDetail from './ontology/OntologyClassDetail.vue'
components["opensilex-OntologyClassDetail"] = OntologyClassDetail;
import OntologyClassTreeView from './ontology/OntologyClassTreeView.vue'
components["opensilex-OntologyClassTreeView"] = OntologyClassTreeView;
import OntologyClassForm from './ontology/OntologyClassForm.vue'
components["opensilex-OntologyClassForm"] = OntologyClassForm;
import OntologyClassView from './ontology/OntologyClassView.vue'
components["opensilex-OntologyClassView"] = OntologyClassView;
import OntologyClassPropertyForm from './ontology/OntologyClassPropertyForm.vue'
components["opensilex-OntologyClassPropertyForm"] = OntologyClassPropertyForm;
import OntologyCsvImporter from './ontology/OntologyCsvImporter.vue';
components["opensilex-OntologyCsvImporter"] = OntologyCsvImporter;
import OntologyPropertyTreeView from './ontology/OntologyPropertyTreeView.vue'
components["opensilex-OntologyPropertyTreeView"] = OntologyPropertyTreeView;
import OntologyPropertyDetail from './ontology/OntologyPropertyDetail.vue'
components["opensilex-OntologyPropertyDetail"] = OntologyPropertyDetail;
import OntologyPropertyForm from './ontology/OntologyPropertyForm.vue'
components["opensilex-OntologyPropertyForm"] = OntologyPropertyForm;
import OntologyPropertyView from './ontology/OntologyPropertyView.vue'
components["opensilex-OntologyPropertyView"] = OntologyPropertyView;
import OntologyTypesView from './ontology/OntologyTypesView.vue'
components["opensilex-OntologyTypesView"] = OntologyTypesView;
import OntologyObjectForm from './ontology/OntologyObjectForm.vue'
components["opensilex-OntologyObjectForm"] = OntologyObjectForm;


import XSDBooleanInput from './ontology/XSDBooleanInput.vue'
components["opensilex-XSDBooleanInput"] = XSDBooleanInput;
import XSDBooleanView from './ontology/XSDBooleanView.vue'
components["opensilex-XSDBooleanView"] = XSDBooleanView;
import XSDDateInput from './ontology/XSDDateInput.vue'
components["opensilex-XSDDateInput"] = XSDDateInput;
import XSDDateTimeInput from './ontology/XSDDateTimeInput.vue'
components["opensilex-XSDDateTimeInput"] = XSDDateTimeInput;
import XSDDateTimeView from './ontology/XSDDateTimeView.vue'
components["opensilex-XSDDateTimeView"] = XSDDateTimeView;
import XSDDateView from './ontology/XSDDateView.vue'
components["opensilex-XSDDateView"] = XSDDateView;
import XSDDecimalInput from './ontology/XSDDecimalInput.vue'
components["opensilex-XSDDecimalInput"] = XSDDecimalInput;
import XSDIntegerInput from './ontology/XSDIntegerInput.vue'
components["opensilex-XSDIntegerInput"] = XSDIntegerInput;
import XSDRawView from './ontology/XSDRawView.vue'
components["opensilex-XSDRawView"] = XSDRawView;
import XSDStringInput from './ontology/XSDStringInput.vue'
components["opensilex-XSDStringInput"] = XSDStringInput;
import XSDLongStringInput from './ontology/XSDLongStringInput.vue'
components["opensilex-XSDLongStringInput"] = XSDLongStringInput;
import XSDUriInput from './ontology/XSDUriInput.vue'
components["opensilex-XSDUriInput"] = XSDUriInput;
import XSDUriView from './ontology/XSDUriView.vue'
components["opensilex-XSDUriView"] = XSDUriView;
import XSDLongStringView from './ontology/XSDLongStringView.vue'
components["opensilex-XSDLongStringView"] = XSDLongStringView;
import XSDNumberView from './ontology/XSDNumberView.vue'
components["opensilex-XSDNumberView"] = XSDNumberView;

// Experiments
import ExperimentForm from './experiments/form/ExperimentForm.vue';
components["opensilex-ExperimentForm"] = ExperimentForm;
import ExperimentForm1 from './experiments/form/ExperimentForm1.vue';
components["opensilex-ExperimentForm1"] = ExperimentForm1;
import ExperimentForm2 from './experiments/form/ExperimentForm2.vue';
components["opensilex-ExperimentForm2"] = ExperimentForm2;
import ExperimentDataVisuForm from './experiments/form/ExperimentDataVisuForm.vue';
components["opensilex-ExperimentDataVisuForm"] = ExperimentDataVisuForm;
import ExperimentVariableSelector from './experiments/form/ExperimentVariableSelector.vue';
components["opensilex-ExperimentVariableSelector"] = ExperimentVariableSelector;
import ExperimentList from './experiments/ExperimentList.vue';
components["opensilex-ExperimentList"] = ExperimentList;
import ExperimentListView from './experiments/ExperimentListView.vue';
components["opensilex-ExperimentListView"] = ExperimentListView;
import ExperimentView from './experiments/ExperimentView.vue';
components["opensilex-ExperimentView"] = ExperimentView;
import ExperimentDetail from './experiments/views/ExperimentDetail.vue';
components["opensilex-ExperimentDetail"] = ExperimentDetail;
import ExperimentScientificObjects from './experiments/views/ExperimentScientificObjects.vue';
components["opensilex-ExperimentScientificObjects"] = ExperimentScientificObjects;
import ExperimentData from './experiments/views/ExperimentData.vue';
components["opensilex-ExperimentData"] = ExperimentData;
import ExperimentDataVisuView from './experiments/views/ExperimentDataVisuView.vue';
components["opensilex-ExperimentDataVisuView"] = ExperimentDataVisuView;
import ExperimentModalList from './experiments/ExperimentModalList.vue';
components["opensilex-ExperimentModalList"] = ExperimentModalList;
import ExperimentSelector from './experiments/ExperimentSelector.vue';
components["opensilex-ExperimentSelector"] = ExperimentSelector;
import ExperimentFacilitySelector from './experiments/ExperimentFacilitySelector.vue';
components["opensilex-ExperimentFacilitySelector"] = ExperimentFacilitySelector;
import ExperimentDatasetForm from './experiments/data/ExperimentDatasetForm.vue';
components["opensilex-ExperimentDatasetForm"] = ExperimentDatasetForm;
import AssociatedExperimentsList from './experiments/AssociatedExperimentsList.vue';
components["opensilex-AssociatedExperimentsList"] = AssociatedExperimentsList;



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
import GermplasmPropertyView from './germplasm/GermplasmPropertyView.vue';
components["opensilex-GermplasmPropertyView"] = GermplasmPropertyView;
import GermplasmPropertySelector from './germplasm/GermplasmPropertySelector.vue';
components["opensilex-GermplasmPropertySelector"] = GermplasmPropertySelector;
import GermplasmHelp from './germplasm/GermplasmHelp.vue';
components["opensilex-GermplasmHelp"] = GermplasmHelp;
import GermplasmSelector from './germplasm/GermplasmSelector.vue';
components["opensilex-GermplasmSelector"] = GermplasmSelector;


// Projects
import ProjectForm from './projects/ProjectForm.vue';
components["opensilex-ProjectForm"] = ProjectForm;
import ProjectForm1 from './projects/ProjectForm1.vue';
components["opensilex-ProjectForm1"] = ProjectForm1;
import ProjectForm2 from './projects/ProjectForm2.vue';
components["opensilex-ProjectForm2"] = ProjectForm2;
import ProjectList from './projects/ProjectList.vue';
components["opensilex-ProjectList"] = ProjectList;
import ProjectView from './projects/ProjectView.vue';
components["opensilex-ProjectView"] = ProjectView;
import ProjectSelector from './projects/ProjectSelector.vue';
components["opensilex-ProjectSelector"] = ProjectSelector;
import ProjectDetails from './projects/ProjectDetails.vue';
components["opensilex-ProjectDetails"] = ProjectDetails;
import ProjectDescription from './projects/details/ProjectDescription.vue';
components["opensilex-ProjectDescription"] = ProjectDescription;

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
import ScientificObjectForm from './scientificObjects/ScientificObjectForm.vue';
components["opensilex-ScientificObjectForm"] = ScientificObjectForm;
import ScientificObjectDetail from './scientificObjects/ScientificObjectDetail.vue';
components["opensilex-ScientificObjectDetail"] = ScientificObjectDetail;
import ScientificObjectDetailProperties from './scientificObjects/ScientificObjectDetailProperties.vue';
components["opensilex-ScientificObjectDetailProperties"] = ScientificObjectDetailProperties;
import ScientificObjectDetailMap from './scientificObjects/ScientificObjectDetailMap.vue';
components["opensilex-ScientificObjectDetailMap"] = ScientificObjectDetailMap;
import ScientificObjectTypes from './scientificObjects/ScientificObjectTypes.vue';
components["opensilex-ScientificObjectTypes"] = ScientificObjectTypes;
import ScientificObjectTypeSelector from './scientificObjects/ScientificObjectTypeSelector.vue';
components["opensilex-ScientificObjectTypeSelector"] = ScientificObjectTypeSelector;
import ScientificObjectDetailView from './scientificObjects/ScientificObjectDetailView.vue';
components["opensilex-ScientificObjectDetailView"] = ScientificObjectDetailView;
import ScientificObjectParentPropertySelector from './scientificObjects/ScientificObjectParentPropertySelector.vue';
components["opensilex-ScientificObjectParentPropertySelector"] = ScientificObjectParentPropertySelector;
import ScientificObjectImportHelp from './scientificObjects/ScientificObjectImportHelp.vue';
components["opensilex-ScientificObjectImportHelp"] = ScientificObjectImportHelp;
import ScientificObjectCSVImporter from './scientificObjects/ScientificObjectCSVImporter.vue';
components["opensilex-ScientificObjectCSVImporter"] = ScientificObjectCSVImporter;
import ScientificObjectCSVTemplateGenerator from './scientificObjects/ScientificObjectCSVTemplateGenerator.vue';
components["opensilex-ScientificObjectCSVTemplateGenerator"] = ScientificObjectCSVTemplateGenerator;
import ScientificObjectVisualizationTab from './scientificObjects/ScientificObjectVisualizationTab.vue';
components["opensilex-ScientificObjectVisualizationTab"] = ScientificObjectVisualizationTab;
import ScientificObjectVisualizationForm from './scientificObjects/visualization/ScientificObjectVisualizationForm.vue';
components["opensilex-ScientificObjectVisualizationForm"] = ScientificObjectVisualizationForm;
import ScientificObjectVariableSelector from './scientificObjects/visualization/ScientificObjectVariableSelector.vue';
components["opensilex-ScientificObjectVariableSelector"] = ScientificObjectVariableSelector;
import ScientificObjectDataFiles from './scientificObjects/ScientificObjectDataFiles.vue';
components["opensilex-ScientificObjectDataFiles"] = ScientificObjectDataFiles;
import ScientificObjectUriView from './scientificObjects/ScientificObjectUriView.vue';
components["opensilex-ScientificObjectUriView"] = ScientificObjectUriView;

// Variables
import VariableList from './variables/VariableList.vue';
components["opensilex-VariableList"] = VariableList;
import VariablesView from './variables/VariablesView.vue';
components["opensilex-VariablesView"] = VariablesView;
import VariableDetails from './variables/VariableDetails.vue';
components["opensilex-VariableDetails"] = VariableDetails;
import VariableView from './variables/views/VariableView.vue';
components["opensilex-VariableView"] = VariableView;
import VariableHelp from './variables/views/VariableHelp.vue';
components["opensilex-VariableHelp"] = VariableHelp;

import VariableCreate from './variables/form/VariableCreate.vue';
components["opensilex-VariableCreate"] = VariableCreate;
import VariableForm from './variables/form/VariableForm.vue';
components["opensilex-VariableForm"] = VariableForm;
import TraitForm from './variables/form/TraitForm.vue';
components["opensilex-TraitForm"] = TraitForm;
import VariableModalList from './variables/VariableModalList.vue';
components["opensilex-VariableModalList"] = VariableModalList;
import VariableSelector from './variables/views/VariableSelector.vue';
components["opensilex-VariableSelector"] = VariableSelector;

// Entity
import EntityForm from './variables/form/EntityForm.vue';
components["opensilex-EntityForm"] = EntityForm;
import EntityCreate from './variables/form/EntityCreate.vue';
components["opensilex-EntityCreate"] = EntityCreate;
import EntityExternalReferencesForm from './variables/form/EntityExternalReferencesForm.vue';
components["opensilex-EntityExternalReferencesForm"] = EntityExternalReferencesForm;
import VariableStructureList from './variables/views/VariableStructureList.vue';
components["opensilex-VariableStructureList"] = VariableStructureList;
import VariableStructureDetails from './variables/views/VariableStructureDetails.vue';
components["opensilex-VariableStructureDetails"] = VariableStructureDetails;


// Method
import MethodCreate from './variables/form/MethodCreate.vue';
components["opensilex-MethodCreate"] = MethodCreate;
import MethodForm from './variables/form/MethodForm.vue';
components["opensilex-MethodForm"] = MethodForm;
import MethodExternalReferencesForm from './variables/form/MethodExternalReferencesForm.vue';
components["opensilex-MethodExternalReferencesForm"] = MethodExternalReferencesForm;

//Characteristic
import CharacteristicForm from './variables/form/CharacteristicForm.vue';
components["opensilex-CharacteristicForm"] = CharacteristicForm;
import CharacteristicModalForm from './variables/form/CharacteristicModalForm.vue';
components["opensilex-CharacteristicModalForm"] = CharacteristicModalForm;
import CharacteristicExternalReferencesForm from './variables/form/CharacteristicExternalReferencesForm.vue';
components["opensilex-CharacteristicExternalReferencesForm"] = CharacteristicExternalReferencesForm;

// Unit
import UnitForm from './variables/form/UnitForm.vue';
components["opensilex-UnitForm"] = UnitForm;
import UnitCreate from './variables/form/UnitCreate.vue';
components["opensilex-UnitCreate"] = UnitCreate;
import UnitExternalReferencesForm from './variables/form/UnitExternalReferencesForm.vue';
components["opensilex-UnitExternalReferencesForm"] = UnitExternalReferencesForm;
import UnitDetails from './variables/views/UnitDetails.vue';
components["opensilex-UnitDetails"] = UnitDetails;


// Data
import DataView from './data/DataView.vue';
components["opensilex-DataView"] = DataView;
import ProvenanceSelector from './data/ProvenanceSelector.vue';
components["opensilex-ProvenanceSelector"] = ProvenanceSelector;
import DatafileProvenanceSelector from './data/DatafileProvenanceSelector.vue';
components["opensilex-DatafileProvenanceSelector"] = DatafileProvenanceSelector;
import ResultModalView from './data/ResultModalView.vue';
components["opensilex-ResultModalView"] = ResultModalView;
import ProvenanceDetails from './data/ProvenanceDetails.vue';
components["opensilex-ProvenanceDetails"] = ProvenanceDetails;
import GenerateDataTemplateFrom from './data/form/GenerateDataTemplateFrom.vue';
components["opensilex-GenerateDataTemplateFrom"] = GenerateDataTemplateFrom;
import ExpProvenanceForm from './data/form/ExpProvenanceForm.vue';
components["opensilex-ExpProvenanceForm"] = ExpProvenanceForm;
import DataValidationReport from './data/form/DataValidationReport.vue';
components["opensilex-DataValidationReport"] = DataValidationReport;
import DataHelpTableView from './data/form/DataHelpTableView.vue';
components["opensilex-DataHelpTableView"] = DataHelpTableView;
import DataProvenanceModalView from './data/DataProvenanceModalView.vue';
components["opensilex-DataProvenanceModalView"] = DataProvenanceModalView;
import ImageModal from './data/ImageModal.vue';
components["opensilex-ImageModal"] = ImageModal;
import DataForm from './data/form/DataForm.vue';
components["opensilex-DataForm"] = DataForm;
import DataForm1 from './data/form/DataForm1.vue';
components["opensilex-DataForm1"] = DataForm1;
import DataForm2 from './data/form/DataForm2.vue';
components["opensilex-DataForm2"] = DataForm2;
import ProvenanceForm from './data/form/ProvenanceForm.vue';
components["opensilex-ProvenanceForm"] = ProvenanceForm;

// PHIS
//IMAGES
// import ImageList from './phis/images/ImageList.vue';
// components["opensilex-ImageList"] = ImageList;
// import ImageView from './phis/images/ImageView.vue';
// components["opensilex-ImageView"] = ImageView;
// import ImageSearch from './phis/images/ImageSearch.vue';
// components["opensilex-ImageSearch"] = ImageSearch;
// import ImageGrid from './phis/images/viewComponents/ImageGrid.vue';
// components["opensilex-ImageGrid"] = ImageGrid;
// import ImageSingle from './phis/images/viewComponents/ImageSingle.vue';
// components["opensilex-ImageSingle"] = ImageSingle;
// import ImageCarousel from './phis/images/viewComponents/ImageCarousel.vue';
// components["opensilex-ImageCarousel"] = ImageCarousel;
// import SciObjectURISearch from './phis/images/searchComponents/SciObjectURISearch.vue';
// components["opensilex-SciObjectURISearch"] = SciObjectURISearch;
// import SciObjectAliasSearch from './phis/images/searchComponents/SciObjectAliasSearch.vue';
// components["opensilex-SciObjectAliasSearch"] = SciObjectAliasSearch;
// import ImageTypeSearch from './phis/images/searchComponents/ImageTypeSearch.vue';
// components["opensilex-ImageTypeSearch"] = ImageTypeSearch;
// import ExperimentSearch from './phis/images/searchComponents/ExperimentSearch.vue';
// components["opensilex-ExperimentSearch"] = ExperimentSearch;
// import SciObjectTypeSearch from './phis/images/searchComponents/SciObjectTypeSearch.vue';
// components["opensilex-SciObjectTypeSearch"] = SciObjectTypeSearch;
// import TimeSearch from './phis/images/searchComponents/TimeSearch.vue';
// components["opensilex-TimeSearch"] = TimeSearch;

// DOCUMENT
import DocumentView from './documents/DocumentView.vue';
components["opensilex-DocumentView"] = DocumentView;
import DocumentForm from './documents/DocumentForm.vue';
components["opensilex-DocumentForm"] = DocumentForm;
import DocumentList from './documents/DocumentList.vue';
components["opensilex-DocumentList"] = DocumentList;
import DocumentDetails from './documents/DocumentDetails.vue';
components["opensilex-DocumentDetails"] = DocumentDetails;
import DocumentTabList from './documents/DocumentTabList.vue';
components["opensilex-DocumentTabList"] = DocumentTabList;

// Map
import MapView from './geometry/MapView.vue';
components["opensilex-MapView"] = MapView;
import AreaForm from "./geometry/AreaForm.vue";
components["opensilex-AreaForm"] = AreaForm;
import AreaDetails from "./geometry/AreaDetails.vue";
components["opensilex-AreaDetails"] = AreaDetails;
import DisplayInformationAboutItem from "./geometry/DisplayInformationAboutItem.vue";
components["opensilex-DisplayInformationAboutItem"] = DisplayInformationAboutItem;

//VISUALIZATION
import VisuPage from './visualization/VisuPage.vue';
components["opensilex-VisuPage"] = VisuPage;
import VisuView from './visualization/VisuView.vue';
components["opensilex-VisuView"] = VisuView;
import VisuGraphic from './visualization/VisuGraphic.vue';
components["opensilex-VisuGraphic"] = VisuGraphic;
import VisuImages from './visualization/VisuImages.vue';
components["opensilex-VisuImages"] = VisuImages;
import VisuForm from './visualization/VisuForm.vue';
components["opensilex-VisuForm"] = VisuForm;
import VisuImageGrid from './visualization/VisuImageGrid.vue';
components["opensilex-VisuImageGrid"] = VisuImageGrid;
import VisuImageCarousel from './visualization/VisuImageCarousel.vue';
components["opensilex-VisuImageCarousel"] = VisuImageCarousel;
import VisuImageSingle from './visualization/VisuImageSingle.vue';
components["opensilex-VisuImageSingle"] = VisuImageSingle;

import DataVisuGraphic from './visualization/DataVisuGraphic.vue';
components["opensilex-DataVisuGraphic"] = DataVisuGraphic;
import DataVisuHelp from './visualization/DataVisuHelp.vue';
components["opensilex-DataVisuHelp"] = DataVisuHelp;

// ANNOTATIONS
import AnnotationModalForm from './annotations/form/AnnotationModalForm.vue';
components["opensilex-AnnotationModalForm"] = AnnotationModalForm;
import AnnotationForm from './annotations/form/AnnotationForm.vue';
components["opensilex-AnnotationForm"] = AnnotationForm;
import AnnotationList from './annotations/list/AnnotationList.vue';
components["opensilex-AnnotationList"] = AnnotationList;

// DEVICES
import DeviceView from './devices/DeviceView.vue';
components["opensilex-DeviceView"] = DeviceView;
import DeviceList from './devices/DeviceList.vue';
components["opensilex-DeviceList"] = DeviceList;
import DeviceDetails from './devices/DeviceDetails.vue';
components["opensilex-DeviceDetails"] = DeviceDetails;
import DeviceDescription from './devices/details/DeviceDescription.vue';
components["opensilex-DeviceDescription"] = DeviceDescription;
import DeviceCreate from './devices/DeviceCreate.vue';
components["opensilex-DeviceCreate"] = DeviceCreate;
import DeviceTable from './devices/DeviceTable.vue';
components["opensilex-DeviceTable"] = DeviceTable;
import DeviceAttributesTable from './devices/DeviceAttributesTable.vue';
components["opensilex-DeviceAttributesTable"] = DeviceAttributesTable;
import DeviceForm from './devices/DeviceForm.vue';
components["opensilex-DeviceForm"] = DeviceForm;
import DeviceVariablesTable from './devices/DeviceVariablesTable.vue';
components["opensilex-DeviceVariablesTable"] = DeviceVariablesTable;
import DeviceVisualizationTab from './devices/DeviceVisualizationTab.vue';
components["opensilex-DeviceVisualizationTab"] = DeviceVisualizationTab;
import DeviceVisualizationForm from './devices/visualization/DeviceVisualizationForm.vue';
components["opensilex-DeviceVisualizationForm"] = DeviceVisualizationForm;
import DeviceVariableSelector from './devices/visualization/DeviceVariableSelector.vue';
components["opensilex-DeviceVariableSelector"] = DeviceVariableSelector;
import DeviceDataFiles from './devices/details/DeviceDataFiles.vue';
components["opensilex-DeviceDataFiles"] = DeviceDataFiles;
import  DeviceTypes from './devices/DeviceTypes.vue';
components["opensilex-DeviceTypes"] = DeviceTypes;
import  DeviceSelector from './devices/DeviceSelector.vue';
components["opensilex-DeviceSelector"] = DeviceSelector;

// EVENTS
import EventModalForm from './events/form/EventModalForm.vue';
components["opensilex-EventModalForm"] = EventModalForm;
import EventForm from './events/form/EventForm.vue';
components["opensilex-EventForm"] = EventForm;
import EventList from './events/list/EventList.vue';
components["opensilex-EventList"] = EventList;
import EventModalView from './events/view/EventModalView.vue';
components["opensilex-EventModalView"] = EventModalView;
import EventsView from './events/EventsView.vue';
components["opensilex-EventsView"] = EventsView;
import EventTypes from './events/ontology/EventTypes.vue';
components["opensilex-EventTypes"] = EventTypes;

// EVENTS CSV
import EventCsvForm from './events/form/csv/EventCsvForm.vue';
components["opensilex-EventCsvForm"] = EventCsvForm;
import EventHelpTableView from './events/form/csv/EventHelpTableView.vue';
components["opensilex-EventHelpTableView"] = EventHelpTableView;
import GenerateEventTemplate from './events/form/csv/GenerateEventTemplate.vue';
components["opensilex-GenerateEventTemplate"] = GenerateEventTemplate;

// MOVE
import MoveForm from './events/form/MoveForm.vue';
components["opensilex-MoveForm"] = MoveForm;
import MoveView from './events/view/MoveView.vue';
components["opensilex-MoveView"] = MoveView;


// POSITION
import PositionForm from './positions/form/PositionForm.vue';
components["opensilex-PositionForm"] = PositionForm;
import PositionsView from './positions/view/PositionsView.vue';
components["opensilex-PositionsView"] = PositionsView;
import PositionList from './positions/list/PositionList.vue';
components["opensilex-PositionList"] = PositionList;

 
 
export default components;
