
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
import AccountForm from './account/AccountForm.vue';
components["opensilex-AccountForm"] = AccountForm;
import AccountList from './account/AccountList.vue';
components["opensilex-AccountList"] = AccountList;
import AccountView from './account/AccountView.vue';
components["opensilex-AccountView"] = AccountView;
import AccountSelector from './account/AccountSelector.vue';
components["opensilex-AccountSelector"] = AccountSelector;

//Persons
import PersonView from "./persons/PersonView.vue";
components["opensilex-PersonView"] = PersonView;
import PersonList from "./persons/PersonList.vue";
components["opensilex-PersonList"] = PersonList;
import PersonForm from "./persons/PersonForm.vue";
components["opensilex-PersonForm"] = PersonForm;
import PersonSelector from './persons/PersonSelector.vue';
components["opensilex-PersonSelector"] = PersonSelector;
import PersonContact from "./persons/PersonContact.vue";
components["opensilex-PersonContact"] = PersonContact;
import ContactsList from "./persons/ContactsList.vue";
components["opensilex-ContactsList"] = ContactsList;
import OrcidSuggestionModal from "./persons/OrcidSuggestionModal.vue";
components["opensilex-OrcidSuggestionModal"] = OrcidSuggestionModal;


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

// Organizations
import OrganizationView from './organizations/OrganizationView.vue';
components["opensilex-OrganizationView"] = OrganizationView;
import OrganizationList from "./organizations/OrganizationList.vue";
components["opensilex-OrganizationList"] = OrganizationList;
import OrganizationDetailView from './organizations/OrganizationDetailView.vue';
components["opensilex-OrganizationDetailView"] = OrganizationDetailView;
import OrganizationForm from './organizations/OrganizationForm.vue';
components["opensilex-OrganizationForm"] = OrganizationForm;
import OrganizationDetail from './organizations/OrganizationDetail.vue';
components["opensilex-OrganizationDetail"] = OrganizationDetail;
import OrganizationSelector from './organizations/OrganizationSelector.vue';
components["opensilex-OrganizationSelector"] = OrganizationSelector;

//sites
import SiteView from './organizations/site/SiteView.vue';
components["opensilex-SiteView"] = SiteView;
import SiteDetailView from './organizations/site/SiteDetailView.vue';
components["opensilex-SiteDetailView"] = SiteDetailView;
import SiteDetail from './organizations/site/SiteDetail.vue';
components["opensilex-SiteDetail"] = SiteDetail;
import SiteForm from './organizations/site/SiteForm.vue';
components["opensilex-SiteForm"] = SiteForm;
import SiteSelector from './organizations/site/SiteSelector.vue';
components["opensilex-SiteSelector"] = SiteSelector;
import SiteList from './organizations/site/SiteList.vue';
components["opensilex-SiteList"] = SiteList;


// Facilities
import FacilityModalForm from './facilities/FacilityModalForm.vue';
components["opensilex-FacilityModalForm"] = FacilityModalForm;
import FacilitiesView from './facilities/FacilitiesView.vue';
components["opensilex-FacilitiesView"] = FacilitiesView;
import FacilitiesTypes from './facilities/FacilitiesTypes.vue';
components["opensilex-FacilitiesTypes"] = FacilitiesTypes;
import FacilityPropertyView from './facilities/FacilityPropertyView.vue';
components["opensilex-FacilityPropertyView"] = FacilityPropertyView;
import FacilityPropertySelector from './facilities/FacilityPropertySelector.vue';
components["opensilex-FacilityPropertySelector"] = FacilityPropertySelector;
import FacilitySelector from './facilities/FacilitySelector.vue';
components["opensilex-FacilitySelector"] = FacilitySelector;
import FacilityForm from './facilities/FacilityForm.vue';
components["opensilex-FacilityForm"] = FacilityForm;
import FacilityView from "./facilities/FacilityView.vue";
components["opensilex-FacilityView"] = FacilityView;
import FacilityDetails from "./facilities/views/FacilityDetails.vue";
components["opensilex-FacilityDetails"] = FacilityDetails;
import FacilityDescription from "./facilities/views/FacilityDescription.vue";
components["opensilex-FacilityDescription"] = FacilityDescription;
import FacilityMonitoringView from "./facilities/views/FacilityMonitoringView.vue";
components["opensilex-FacilityMonitoringView"] = FacilityMonitoringView;
import FacilityListView from "./facilities/FacilityListView.vue";
components["opensilex-FacilityListView"] = FacilityListView;
import FacilitiesModalList from "./facilities/FacilitiesModalList.vue";
components["opensilex-FacilitiesModalList"] = FacilitiesModalList;


import DatePeriodPicker from "./facilities/DatePeriodPicker.vue";
components["opensilex-DatePeriodPicker"] = DatePeriodPicker;
import FacilityHistogramSettings from "./facilities/FacilityHistogramSettings.vue";
components["opensilex-FacilityHistogramSettings"] = FacilityHistogramSettings;


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
import  FactorCategoryTypes from './experiments/factors/FactorCategoryTypes.vue';
components["opensilex-FactorCategoryTypes"] = FactorCategoryTypes;

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

import ModalFormSelector from './common/forms/ModalFormSelector.vue'
components["opensilex-ModalFormSelector"] = ModalFormSelector;
import FormSelector from './common/forms/FormSelector.vue'
components["opensilex-FormSelector"] = FormSelector;
import CustomTreeselect from './common/forms/CustomTreeselect.vue'
components["opensilex-CustomTreeselect"] = CustomTreeselect;
import CustomTreeselectOptionLabel from './common/forms/CustomTreeselectOptionLabel.vue'
components["opensilex-CustomTreeselectOptionLabel"] = CustomTreeselectOptionLabel;
import CustomTreeselectValueLabel from './common/forms/CustomTreeselectValueLabel.vue'
components["opensilex-CustomTreeselectValueLabel"] = CustomTreeselectValueLabel;
import CustomTreeselectRefineSearchMessage from './common/forms/CustomTreeselectRefineSearchMessage.vue'
components["opensilex-CustomTreeselectRefineSearchMessage"] = CustomTreeselectRefineSearchMessage;

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
import AddressForm from './common/forms/AddressForm.vue'
components["opensilex-AddressForm"] = AddressForm;

import DateRangePickerForm from './common/forms/DateRangePickerForm.vue'
components["opensilex-DateRangePickerForm"] = DateRangePickerForm;
import DateTimeRangeForm from './common/forms/DateTimeRangeForm.vue'
components["opensilex-DateTimeRangeForm"] = DateTimeRangeForm;

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
import AddressView from './common/views/AddressView.vue'
components["opensilex-AddressView"] = AddressView;

import TextView from './common/views/TextView.vue'
components["opensilex-TextView"] = TextView;
import DateView from './common/views/DateView.vue'
components["opensilex-DateView"] = DateView;
import MetadataView from './common/views/MetadataView.vue'
components["opensilex-MetadataView"] = MetadataView;
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
import NbElementPerPageSelector from "./common/views/NbElementPerPageSelector.vue";
components["opensilex-NbElementPerPageSelector"] = NbElementPerPageSelector;
import Card from './common/views/Card.vue'
components["opensilex-Card"] = Card;
import ModalListBuilder from './common/views/ModalListBuilder.vue'
components["opensilex-ModalListBuilder"] = ModalListBuilder;

//#region External references
import ExternalReferencesModalForm from './common/external-references/ExternalReferencesModalForm.vue'
components["opensilex-ExternalReferencesModalForm"] = ExternalReferencesModalForm;
import ExternalReferencesForm from './common/external-references/ExternalReferencesForm.vue'
components["opensilex-ExternalReferencesForm"] = ExternalReferencesForm;
import ExternalReferencesDetails from './common/external-references/ExternalReferencesDetails.vue'
components["opensilex-ExternalReferencesDetails"] = ExternalReferencesDetails;
//#region Skos
import SkosSelector from "./common/external-references/skos/SkosSelector.vue";
components["opensilex-SkosSelector"] = SkosSelector;
import SkosRelationInput from "./common/external-references/skos/SkosRelationInput.vue";
components["opensilex-SkosRelationInput"] = SkosRelationInput;
import SkosRelationTable from "./common/external-references/skos/SkosRelationTable.vue";
components["opensilex-SkosRelationTable"] = SkosRelationTable;
//#endregion
//#endregion

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
import FavoriteButton from './common/buttons/FavoriteButton.vue'
components["opensilex-FavoriteButton"] = FavoriteButton;

import Dropdown from './common/dropdown/Dropdown.vue'
components["opensilex-Dropdown"] = Dropdown;

import ItemsPropertiesSelector from './common/filters/ItemsPropertiesSelector.vue';
components["opensilex-ItemsPropertiesSelector"] = ItemsPropertiesSelector;

// Vocabulary/Ontology
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
import OntologyCsvImporter from './ontology/csv/OntologyCsvImporter.vue';
components["opensilex-OntologyCsvImporter"] = OntologyCsvImporter;
import OntologyCsvTemplateGenerator from './ontology/csv/OntologyCsvTemplateGenerator.vue';
components["opensilex-OntologyCsvTemplateGenerator"] = OntologyCsvTemplateGenerator;

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
import OntologyObjectProperties from './ontology/OntologyObjectProperties.vue'
components["opensilex-OntologyObjectProperties"] = OntologyObjectProperties;
import OntologyObjectForm from './ontology/OntologyObjectForm.vue'
components["opensilex-OntologyObjectForm"] = OntologyObjectForm;
import OntologyRelationsForm from './ontology/OntologyRelationsForm.vue'
components["opensilex-OntologyRelationsForm"] = OntologyRelationsForm;

import SharedResourceInstanceSelector from './sharedResourceInstances/SharedResourceInstanceSelector.vue';
components["opensilex-SharedResourceInstanceSelector"] = SharedResourceInstanceSelector;

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

// Namespaces
import NamespacesView from './namespaces/NamespacesView.vue'
components["opensilex-NamespacesView"] = NamespacesView;

// Experiments
import ExperimentForm from './experiments/form/ExperimentForm.vue';
components["opensilex-ExperimentForm"] = ExperimentForm;
import ExperimentForm1 from './experiments/form/ExperimentForm1.vue';
components["opensilex-ExperimentForm1"] = ExperimentForm1;
import ExperimentForm2 from './experiments/form/ExperimentForm2.vue';
components["opensilex-ExperimentForm2"] = ExperimentForm2;
import ExperimentDataVisuForm from './experiments/form/ExperimentDataVisuForm.vue';
components["opensilex-ExperimentDataVisuForm"] = ExperimentDataVisuForm;
import ExperimentDataVisualisationForm from './experiments/form/ExperimentDataVisualisationForm.vue';
components["opensilex-ExperimentDataVisualisationForm"] = ExperimentDataVisualisationForm;
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
import ExperimentDataVisualisationView from './experiments/views/ExperimentDataVisualisationView.vue';
components["opensilex-ExperimentDataVisualisationView"] = ExperimentDataVisualisationView;
import ExperimentSelector from './experiments/ExperimentSelector.vue';
components["opensilex-ExperimentSelector"] = ExperimentSelector;
import AssociatedExperimentsList from './experiments/AssociatedExperimentsList.vue';
components["opensilex-AssociatedExperimentsList"] = AssociatedExperimentsList;
import ExperimentSimpleList from "./experiments/ExperimentSimpleList.vue";
components["opensilex-ExperimentSimpleList"] = ExperimentSimpleList;
import ExperimentDataVisualisation from './experiments/ExperimentDataVisualisation.vue';
components["opensilex-ExperimentDataVisualisation"] = ExperimentDataVisualisation;
import ExperimentsModalList from "./experiments/ExperimentsModalList.vue";
components["opensilex-ExperimentsModalList"] = ExperimentsModalList;


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
import AttributesTable from './common/forms/AttributesTable.vue';
components["opensilex-AttributesTable"] = AttributesTable;
import GermplasmPropertyView from './germplasm/GermplasmPropertyView.vue';
components["opensilex-GermplasmPropertyView"] = GermplasmPropertyView;
import GermplasmPropertySelector from './germplasm/GermplasmPropertySelector.vue';
components["opensilex-GermplasmPropertySelector"] = GermplasmPropertySelector;
import GermplasmHelp from './germplasm/GermplasmHelp.vue';
components["opensilex-GermplasmHelp"] = GermplasmHelp;
import GermplasmSelector from './germplasm/GermplasmSelector.vue';
components["opensilex-GermplasmSelector"] = GermplasmSelector;
import GermplasmAttributesSelector from './germplasm/GermplasmAttributesSelector.vue';
components["opensilex-GermplasmAttributesSelector"] = GermplasmAttributesSelector;
import GermplasmGroup from './germplasmGroup/GermplasmGroup.vue';
components["opensilex-GermplasmGroup"] = GermplasmGroup;
import GermplasmGlobalView from './germplasm/GermplasmGlobalView.vue';
components["opensilex-GermplasmGlobalView"] = GermplasmGlobalView;
import GermplasmGroupStructureDetails from './germplasmGroup/GermplasmGroupStructureDetails.vue';
components["opensilex-GermplasmGroupStructureDetails"] = GermplasmGroupStructureDetails;
import GroupGermplasmForm from './germplasmGroup/GroupGermplasmForm.vue';
components["opensilex-GroupGermplasmForm"] = GroupGermplasmForm;
import GermplasmGroupList from './germplasmGroup/GermplasmGroupList.vue';
components["opensilex-GermplasmGroupList"] = GermplasmGroupList;
import GermplasmSelectorWithFilter from './germplasm/GermplasmSelectorWithFilter.vue';
components["opensilex-GermplasmSelectorWithFilter"] = GermplasmSelectorWithFilter;
import GermplasmGroupContentList from './germplasmGroup/GermplasmGroupContentList.vue';
components["opensilex-GermplasmGroupContentList"] = GermplasmGroupContentList;
import GermplasmGroupSelector from './germplasmGroup/GermplasmGroupSelector.vue';
components["opensilex-GermplasmGroupSelector"] = GermplasmGroupSelector;
import AssociatedGermplasmGroupsList from './germplasmGroup/AssociatedGermplasmGroupsList.vue';
components["opensilex-AssociatedGermplasmGroupsList"] = AssociatedGermplasmGroupsList;
import GermplasmGroupHelp from './germplasmGroup/GermplasmGroupHelp.vue';
components["opensilex-GermplasmGroupHelp"] = GermplasmGroupHelp;
import GermplasmAttributesValueSelector from './germplasm/GermplasmAttributesValueSelector.vue';
components["opensilex-GermplasmAttributesValueSelector"] = GermplasmAttributesValueSelector;
import GermplasmAddColumnModal from './germplasm/addColumnComponents/GermplasmAddColumnModal.vue';
components["opensilex-GermplasmAddColumnModal"] = GermplasmAddColumnModal;
import GermplasmControlledAttributesSelector from './germplasm/addColumnComponents/GermplasmControlledAttributesSelector.vue';
components["opensilex-GermplasmControlledAttributesSelector"] = GermplasmControlledAttributesSelector;
import GermplasmParentsModalFormField from './germplasm/GermplasmParentsModalFormField.vue';
components["opensilex-GermplasmParentsModalFormField"] = GermplasmParentsModalFormField;
import GermplasmParentsModalFormFieldLine from './germplasm/GermplasmParentsModalFormFieldLine.vue';
components["opensilex-GermplasmParentsModalFormFieldLine"] = GermplasmParentsModalFormFieldLine;

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
import ScientificObjectVisualizationTab from './scientificObjects/ScientificObjectVisualizationTab.vue';
components["opensilex-ScientificObjectVisualizationTab"] = ScientificObjectVisualizationTab;
import ScientificObjectVisualizationForm from './scientificObjects/visualization/ScientificObjectVisualizationForm.vue';
components["opensilex-ScientificObjectVisualizationForm"] = ScientificObjectVisualizationForm;
import ScientificObjectDataFiles from './scientificObjects/ScientificObjectDataFiles.vue';
components["opensilex-ScientificObjectDataFiles"] = ScientificObjectDataFiles;
import ScientificObjectModalList from './scientificObjects/ScientificObjectModalList.vue';
components["opensilex-ScientificObjectModalList"] = ScientificObjectModalList;
import ScientificObjectModalListByExp from './scientificObjects/ScientificObjectModalListByExp.vue';
components["opensilex-ScientificObjectModalListByExp"] = ScientificObjectModalListByExp;
import ScientificObjectsView from './scientificObjects/ScientificObjectsView.vue';
components["opensilex-ScientificObjectsView"] = ScientificObjectsView;
import UsedScientificObjectSelector from './scientificObjects/views/UsedScientificObjectSelector.vue';
components["opensilex-UsedScientificObjectSelector"] = UsedScientificObjectSelector;
import CriteriaSearchModalCreator from './scientificObjects/CriteriaSearchModalCreator.vue';
components["opensilex-CriteriaSearchModalCreator"] = CriteriaSearchModalCreator;
import CriteriaOperatorSelector from './scientificObjects/CriteriaOperatorSelector.vue';
components["opensilex-CriteriaOperatorSelector"] = CriteriaOperatorSelector;
import CriteriaSearchModalLine from './scientificObjects/CriteriaSearchModalLine.vue';
components["opensilex-CriteriaSearchModalLine"] = CriteriaSearchModalLine;

// Variables
import VariableList from './variables/VariableList.vue';
components["opensilex-VariableList"] = VariableList;
import VariablesView from './variables/VariablesView.vue';
components["opensilex-VariablesView"] = VariablesView;
import VariableDetails from './variables/VariableDetails.vue';
components["opensilex-VariableDetails"] = VariableDetails;
import VariableView from './variables/views/VariableView.vue';
components["opensilex-VariableView"] = VariableView;
import VariableVisualizationTab from './variables/views/VariableVisualizationTab.vue';
components["opensilex-VariableVisualizationTab"] = VariableVisualizationTab;
import VariableVisualizationTile from './variables/views/VariableVisualizationTile.vue';
components["opensilex-VariableVisualizationTile"] = VariableVisualizationTile;
import VariableVisualizationForm from './variables/form/VariableVisualizationForm.vue';
components["opensilex-VariableVisualizationForm"] = VariableVisualizationForm;
import VariableDevicesSelector from './variables/form/VariableDevicesSelector.vue';
components["opensilex-VariableDevicesSelector"] = VariableDevicesSelector;
import VariableDataTypeSelector from './variables/form/VariableDataTypeSelector.vue';
components["opensilex-VariableDataTypeSelector"] = VariableDataTypeSelector;
import VariableTimeIntervalSelector from './variables/form/VariableTimeIntervalSelector.vue';
components["opensilex-VariableTimeIntervalSelector"] = VariableTimeIntervalSelector;
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
import VariablePropertySelector from './variables/views/VariablePropertySelector.vue';
components["opensilex-VariablePropertySelector"] = VariablePropertySelector;

import VariableSelectorWithFilter from './variables/views/VariableSelectorWithFilter.vue';
components["opensilex-VariableSelectorWithFilter"] = VariableSelectorWithFilter;
import VariableSelector from './variables/views/VariableSelector.vue';
components["opensilex-VariableSelector"] = VariableSelector;

// GroupVariables
import GroupVariablesForm from './groupVariable/GroupVariablesForm.vue';
components["opensilex-GroupVariablesForm"] = GroupVariablesForm;
import GroupVariablesHelp from './groupVariable/GroupVariablesHelp.vue';
components["opensilex-GroupVariablesHelp"] = GroupVariablesHelp;
import GroupVariablesList from './groupVariable/GroupVariablesList.vue';
components["opensilex-GroupVariablesList"] = GroupVariablesList;
import GroupVariablesModalList from './groupVariable/GroupVariablesModalList.vue';
components["opensilex-GroupVariablesModalList"] = GroupVariablesModalList;
import GroupVariablesSelector from './groupVariable/GroupVariablesSelector.vue';
components["opensilex-GroupVariablesSelector"] = GroupVariablesSelector;
import GroupVariablesDescription from './groupVariable/GroupVariablesDescription.vue';
components["opensilex-GroupVariablesDescription"] = GroupVariablesDescription;
import GroupVariablesDetails from './groupVariable/GroupVariablesDetails.vue';
components["opensilex-GroupVariablesDetails"] = GroupVariablesDetails;

// Entity
import VariableStructureList from './variables/views/VariableStructureList.vue';
components["opensilex-VariableStructureList"] = VariableStructureList;
import VariableStructureDetails from './variables/views/VariableStructureDetails.vue';
components["opensilex-VariableStructureDetails"] = VariableStructureDetails;
import EntitySelector from './variables/form/EntitySelector.vue';
components["opensilex-EntitySelector"] = EntitySelector;

//#region Agroportal
//#region Selector
import AgroportalSearch from './common/external-references/agroportal/AgroportalSearch.vue';
components["opensilex-AgroportalSearch"] = AgroportalSearch;
import AgroportalResults from './common/external-references/agroportal/AgroportalResults.vue';
components["opensilex-AgroportalResults"] = AgroportalResults;
import AgroportalResultItem from './common/external-references/agroportal/AgroportalResultItem.vue';
components["opensilex-AgroportalResultItem"] = AgroportalResultItem;
import AgroportalTermSelector from './common/external-references/agroportal/AgroportalTermSelector.vue';
components["opensilex-AgroportalTermSelector"] = AgroportalTermSelector;
//#endregion
//#region Form
import AgroportalSearchFormPart from './common/external-references/agroportal/wizard/AgroportalSearchFormPart.vue';
components["opensilex-AgroportalSearchFormPart"] = AgroportalSearchFormPart;
import AgroportalCreateFormPart from './common/external-references/agroportal/wizard/AgroportalCreateFormPart.vue';
components["opensilex-AgroportalCreateFormPart"] = AgroportalCreateFormPart;
import AgroportalMappingFormPart from './common/external-references/agroportal/wizard/AgroportalMappingFormPart.vue';
components["opensilex-AgroportalMappingFormPart"] = AgroportalMappingFormPart;
import AgroportalCreateForm from './common/external-references/agroportal/wizard/AgroportalCreateForm.vue';
components["opensilex-AgroportalCreateForm"] = AgroportalCreateForm;
//#endregion
//#region Variables
import AgroportalEntityForm from './variables/agroportal/AgroportalEntityForm.vue';
components["opensilex-AgroportalEntityForm"] = AgroportalEntityForm;
import AgroportalEntityOfInterestForm from './variables/agroportal/AgroportalEntityOfInterestForm.vue';
components["opensilex-AgroportalEntityOfInterestForm"] = AgroportalEntityOfInterestForm;
import AgroportalCharacteristicForm from "./variables/agroportal/AgroportalCharacteristicForm.vue";
components["opensilex-AgroportalCharacteristicForm"] = AgroportalCharacteristicForm;
import AgroportalMethodForm from "./variables/agroportal/AgroportalMethodForm.vue";
components["opensilex-AgroportalMethodForm"] = AgroportalMethodForm;
import AgroportalUnitForm from "./variables/agroportal/AgroportalUnitForm.vue";
components["opensilex-AgroportalUnitForm"] = AgroportalUnitForm;
//#endregion
//#endregion

// Entity of interest
import InterestEntitySelector from './variables/form/InterestEntitySelector.vue';
components["opensilex-InterestEntitySelector"] = InterestEntitySelector;

// Method
import MethodSelector from './variables/form/MethodSelector.vue';
components["opensilex-MethodSelector"] = MethodSelector;


//Characteristic
import CharacteristicSelector from './variables/form/CharacteristicSelector.vue';
components["opensilex-CharacteristicSelector"] = CharacteristicSelector;

// Unit
import UnitDetails from './variables/views/UnitDetails.vue';
components["opensilex-UnitDetails"] = UnitDetails;
import UnitSelector from './variables/form/UnitSelector.vue';
components["opensilex-UnitSelector"] = UnitSelector;


// Data
import DataView from './data/DataView.vue';
components["opensilex-DataView"] = DataView;
import ProvenanceSelector from './data/ProvenanceSelector.vue';
components["opensilex-ProvenanceSelector"] = ProvenanceSelector;
import DatafileProvenanceSelector from './data/DatafileProvenanceSelector.vue';
components["opensilex-DatafileProvenanceSelector"] = DatafileProvenanceSelector;
import DataProvenanceSelector from './data/DataProvenanceSelector.vue';
components["opensilex-DataProvenanceSelector"] = DataProvenanceSelector;
import ResultModalView from './data/ResultModalView.vue';
components["opensilex-ResultModalView"] = ResultModalView;
import ProvenanceDetails from './data/ProvenanceDetails.vue';
components["opensilex-ProvenanceDetails"] = ProvenanceDetails;
import BatchDetails from './data/BatchDetails.vue';
components["opensilex-BatchDetails"] = BatchDetails;
import GenerateDataTemplateFrom from './data/form/GenerateDataTemplateFrom.vue';
components["opensilex-GenerateDataTemplateFrom"] = GenerateDataTemplateFrom;
import ProvenanceForm from './data/form/ProvenanceForm.vue';
components["opensilex-ProvenanceForm"] = ProvenanceForm;
import ProvenanceAgentForm from './data/form/ProvenanceAgentForm.vue';
components["opensilex-ProvenanceAgentForm"] = ProvenanceAgentForm;
import DataValidationReport from './data/form/DataValidationReport.vue';
components["opensilex-DataValidationReport"] = DataValidationReport;
import DataHelpTableView from './data/form/DataHelpTableView.vue';
components["opensilex-DataHelpTableView"] = DataHelpTableView;
import DataProvenanceModalView from './data/DataProvenanceModalView.vue';
components["opensilex-DataProvenanceModalView"] = DataProvenanceModalView;
import ImageModal from './data/ImageModal.vue';
components["opensilex-ImageModal"] = ImageModal;
import DataFilesView from './data/DataFilesView.vue';
components["opensilex-DataFilesView"] = DataFilesView;
import ProvenanceView from './data/ProvenanceView.vue';
components["opensilex-ProvenanceView"] = ProvenanceView;
import ProvenanceDetailsPage from './data/ProvenanceDetailsPage.vue';
components["opensilex-ProvenanceDetailsPage"] = ProvenanceDetailsPage;
import ProvenanceList from './data/ProvenanceList.vue';
components["opensilex-ProvenanceList"] = ProvenanceList;
import AgentTypeSelector from './data/AgentTypeSelector.vue';
components["opensilex-AgentTypeSelector"] = AgentTypeSelector;
import DataList from './data/DataList.vue';
components["opensilex-DataList"] = DataList;
import DataFilesList from './data/DataFilesList.vue';
components["opensilex-DataFilesList"] = DataFilesList;

import DataFilesImagesList from './data/DataFilesImagesList.vue';
components["opensilex-DataFilesImagesList"] = DataFilesImagesList;
import DataExportModal from './data/DataExportModal.vue';
components["opensilex-DataExportModal"] = DataExportModal;
import DataImportForm from './data/form/DataImportForm.vue';
components["opensilex-DataImportForm"] = DataImportForm;
import DeleteByBatchModal from './data/DeleteByBatchModal.vue';
components["opensilex-DeleteByBatchModal"] = DeleteByBatchModal;

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
import FilterMap from "./geometry/FilterMap.vue";
components["opensilex-FilterMap"] = FilterMap;
import Timeline from './geometry/Timeline.vue'
components["opensilex-Timeline"] = Timeline;
import DisplayInformationAboutItem from "./geometry/DisplayInformationAboutItem.vue";
components["opensilex-DisplayInformationAboutItem"] = DisplayInformationAboutItem;
import ExportShapeModalList from "./geometry/ExportShapeModalList.vue";
components["opensilex-ExportShapeModalList"] = ExportShapeModalList;
import GlobalMapView from "./geometry/GlobalMapView.vue";
components["opensilex-GlobalMapView"] = GlobalMapView;
import GlobalMapMenu from "./geometry/GlobalMapMenu.vue";
components["opensilex-GlobalMapMenu"] = GlobalMapMenu;

//LOCATION
import LocationModalForm from "./location/form/LocationModalForm.vue";
components["opensilex-LocationModalForm"] = LocationModalForm;
import LocationForm from "./location/form/LocationForm.vue";
components["opensilex-LocationForm"] = LocationForm;
import LocationsForm from "./location/form/LocationsForm.vue";
components["opensilex-LocationsForm"] = LocationsForm;
import LocationList from "./location/list/LocationList.vue";
components["opensilex-LocationList"] = LocationList;

// VISUALIZATION

import ImageGrid from './visualization/ImageGrid.vue';
components["opensilex-ImageGrid"] = ImageGrid;
import ImageSingle from './visualization/ImageSingle.vue';
components["opensilex-ImageSingle"] = ImageSingle;

import VisuImages from './visualization/VisuImages.vue';
components["opensilex-VisuImages"] = VisuImages;
import VisuImageGrid from './visualization/VisuImageGrid.vue';
components["opensilex-VisuImageGrid"] = VisuImageGrid;
import VisuImageSingle from './visualization/VisuImageSingle.vue';
components["opensilex-VisuImageSingle"] = VisuImageSingle;
import ImageLightBox from './visualization/ImageLightBox.vue';
components["opensilex-ImageLightBox"] = ImageLightBox;


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
import AnnotationDetails from './annotations/form/AnnotationDetails.vue';
components["opensilex-AnnotationDetails"] = AnnotationDetails;

// DEVICES
import DeviceView from './devices/DeviceView.vue';
components["opensilex-DeviceView"] = DeviceView;
import DeviceList from './devices/DeviceList.vue';
components["opensilex-DeviceList"] = DeviceList;
import DeviceDetails from './devices/DeviceDetails.vue';
components["opensilex-DeviceDetails"] = DeviceDetails;
import DeviceDescription from './devices/details/DeviceDescription.vue';
components["opensilex-DeviceDescription"] = DeviceDescription;
import DeviceForm from './devices/form/DeviceForm.vue';
components["opensilex-DeviceForm"] = DeviceForm;
import DeviceModalForm from './devices/form/DeviceModalForm.vue';
components["opensilex-DeviceModalForm"] = DeviceModalForm;

import DeviceVisualizationTab from './devices/DeviceVisualizationTab.vue';
components["opensilex-DeviceVisualizationTab"] = DeviceVisualizationTab;
import DeviceVisualizationForm from './devices/visualization/DeviceVisualizationForm.vue';
components["opensilex-DeviceVisualizationForm"] = DeviceVisualizationForm;
import DeviceDataFiles from './devices/details/DeviceDataFiles.vue';
components["opensilex-DeviceDataFiles"] = DeviceDataFiles;
import  DeviceTypes from './devices/DeviceTypes.vue';
components["opensilex-DeviceTypes"] = DeviceTypes;
import  DeviceSelector from './devices/DeviceSelector.vue';
components["opensilex-DeviceSelector"] = DeviceSelector;
import  DevicePropertySelector from './devices/DevicePropertySelector.vue';
components["opensilex-DevicePropertySelector"] = DevicePropertySelector;
import  DeviceCsvForm from './devices/csv/DeviceCsvForm.vue';
components["opensilex-DeviceCsvForm"] = DeviceCsvForm;
import DeviceImportHelp from './devices/csv/DeviceImportHelp.vue';
components["opensilex-DeviceImportHelp"] = DeviceImportHelp;
import DeviceDetailsMap from './devices/DeviceDetailsMap.vue';
components["opensilex-DeviceDetailsMap"] = DeviceDetailsMap;

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

// HOME
import HomeView from './home/HomeView.vue';
components["opensilex-HomeView"] = HomeView;
import Dashboard from './home/Dashboard.vue'
components["opensilex-Dashboard"] = Dashboard;
import Histogram from './home/dashboard/Histogram.vue'
components["opensilex-Histogram"] = Histogram;
import VisualisationGraphic from "./home/dashboard/VisualisationGraphic.vue"
components["opensilex-VisualisationGraphic"] = VisualisationGraphic;
import HistogramSettings from './home/dashboard/HistogramSettings.vue'
components["opensilex-HistogramSettings"] = HistogramSettings;
import Twitter from './home/dashboard/Twitter.vue'
components["opensilex-Twitter"] = Twitter;
import Favorites from './home/dashboard/Favorites.vue'
components["opensilex-Favorites"] = Favorites;
import FavoritesHelp from './home/dashboard/FavoritesHelp.vue'
components["opensilex-FavoritesHelp"] = FavoritesHelp;
import DataMonitoring from './home/dashboard/DataMonitoring.vue'
components["opensilex-DataMonitoring"] = DataMonitoring;

// POSITION
import PositionForm from './positions/form/PositionForm.vue';
components["opensilex-PositionForm"] = PositionForm;
import PositionsView from './positions/view/PositionsView.vue';
components["opensilex-PositionsView"] = PositionsView;
import PositionList from './positions/list/PositionList.vue';
components["opensilex-PositionList"] = PositionList;
import AssociatedPosition from './positions/list/AssociatedPositionList.vue';
components["opensilex-AssociatedPositionList"] = AssociatedPosition;

// Tools
import PackagesView from './tools/PackagesView.vue';
components["opensilex-PackagesView"] = PackagesView;
import SystemView from './tools/SystemView.vue';
components["opensilex-SystemView"] = SystemView;
import GDPR from './tools/GDPR.vue'
components["opensilex-GDPR"] = GDPR;

//GlobalUriSearch
import GlobalUriSearchResult from "./globalUriSearch/GlobalUriSearchResult.vue";
components["opensilex-GlobalUriSearchResult"] = GlobalUriSearchResult;
import GlobalUriSearchBox from "./globalUriSearch/GlobalUriSearchBox.vue";
components["opensilex-GlobalUriSearchBox"] = GlobalUriSearchBox;


export default components;
