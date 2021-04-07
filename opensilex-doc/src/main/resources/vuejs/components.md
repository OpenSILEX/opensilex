OpenSilex Vue.JS components
===========================

- [OpenSilex Vue.JS components](#opensilex-vuejs-components)
  - [Icons](#icons)
  - [Buttons](#buttons)
    - [Generic button](#generic-button)
    - [Create button](#create-button)
    - [Delete button](#delete-button)
    - [Edit button](#edit-button)
    - [Details button](#details-button)
    - [Add child button](#add-child-button)
    - [HelpButton](#helpbutton)
  - [Forms fields components](#forms-fields-components)
    - [Generic form field label](#generic-form-field-label)
    - [Generic form field](#generic-form-field)
    - [Input form field](#input-form-field)
    - [Textarea form field](#textarea-form-field)
    - [Checkbox form field](#checkbox-form-field)
    - [Select form field](#select-form-field)
    - [URI form field](#uri-form-field)
    - [Type form field](#type-form-field)
    - [CSV file input](#csv-file-input)
    - [File input form field](#file-input-form-field)
    - [Tags input form field](#tags-input-form-field)
    - [Datetime input form field](#datetime-input-form-field)
  - [Forms containers](#forms-containers)
    - [Modal form](#modal-form)
    - [Wizard form](#wizard-form)
  - [View component](#view-component)
    - [Uri link](#uri-link)
    - [TableView](#tableview)
    - [TableAsyncView](#tableasyncview)

## Icons

Identifier: **opensilex-Icon**

Description: 

Two icons libraries are included by default in OpenSilex:

- [Fontawesome](https://fontawesome.com/)
- [IconKit](https://iconkit.lavalite.org/)

Any icon of those libraries could be used (only open-source version of fontawesome)

Icons are referenced by their identifier:

- fa#icon-class: for Fontawesome icons
- ik#icon-class: for IconKit icons

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
icon | string | *ik#ik-folder* | Icon identifier in format library-id#icon-class
size | string | *sm* | Size class used by fontawesome (given class will also be added to IkonKit markup)

## Buttons

### Generic button

Identifier: **opensilex-Button**

Description: 

This component is the base for all buttons displayed in OpenSilex and is based on [Bootstrap-vue button.](https://bootstrap-vue.org/docs/components/button)

Other buttons described in this documentation are a predefined variation of this component.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
label | string | *-* | Translation key of the button label (should always be present even if not visible for accessibility purpose)
variant | string | *-* | [Bootstrap button variant](https://getbootstrap.com/docs/4.1/components/buttons/)
small | boolean | *true* | Flag to determine if button should be small (equivalent to bootstrap "sm" class)
disabled | boolean | *false* | Flag to determine if button is disabled or not
icon | string | *-* | Button icon identifier see [icon component](#Icons) for more information

Slots:
Name | Props | Description
--- | --- | ---
icon | *-* | This slot allows you to completely replace the button icon (usefull if you need to switch icon depending of some other variable).

Events:
ID | Parameter | Description
--- | --- | ---
click | *-* | Event called after button click

### Create button

Identifier: **opensilex-CreateButton**

Description: 

Button component for "Create" action.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
label | string | *-* | Translation key of the button label (should always be present even if not visible for accessibility purpose)
small | boolean | *true* | Flag to determine if button should be small (equivalent to bootstrap "sm" class)
disabled | boolean | *false* | Flag to determine if button is disabled or not

Events:
ID | Parameter | Description
--- | --- | ---
@click | *-* | Event called after button click

### Delete button

Identifier: **opensilex-DeleteButton**

Description: 

Button component for "Delete" action, this component displays a confirmation message box before emitting "click" event.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
label | string | *-* | Translation key of the button label (should always be present even if not visible for accessibility purpose)
small | boolean | *true* | Flag to determine if button should be small (equivalent to bootstrap "sm" class)
disabled | boolean | *false* | Flag to determine if button is disabled or not

Events:
ID | Parameter | Description
--- | --- | ---
@click | *-* | Event called after button click and user confirmation

### Edit button

Identifier: **opensilex-EditButton**

Description: 

Button component for "Edit" action.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
label | string | *-* | Translation key of the button label (should always be present even if not visible for accessibility purpose)
small | boolean | *true* | Flag to determine if button should be small (equivalent to bootstrap "sm" class)
disabled | boolean | *false* | Flag to determine if button is disabled or not

Events:
ID | Parameter | Description
--- | --- | ---
@click | *-* | Event called after button click

### Details button

Identifier: **opensilex-DetailButton**

Description: 

Button component for "Show details/Hide details" action.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
label | string | *-* | Translation key of the button label (should always be present even if not visible for accessibility purpose)
small | boolean | *true* | Flag to determine if button should be small (equivalent to bootstrap "sm" class)
disabled | boolean | *false* | Flag to determine if button is disabled or not
detailVisible | boolean | *false* | Flag to determine detail button state

Events:
ID | Parameter | Description
--- | --- | ---
click | *-* | Event called after button click

### Add child button

Identifier: **opensilex-AddChildButton**

Description: 

Button component for "Add child" action, this component could be usefull for tree interactions.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
label | string | *-* | Translation key of the button label (should always be present even if not visible for accessibility purpose)
small | boolean | *true* | Flag to determine if button should be small (equivalent to bootstrap "sm" class)
disabled | boolean | *false* | Flag to determine if button is disabled or not

Events:
ID | Parameter | Description
--- | --- | ---
@click | *-* | Event called after button click

### HelpButton

Identifier: **opensilex-HelpButton**

Description: 

Button component for "Help" action, this component could be usefull to get help about a concept.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
label | string | *-* | Translation key of the button label (should always be present even if not visible for accessibility purpose)
small | boolean | *true* | Flag to determine if button should be small (equivalent to bootstrap "sm" class)
disabled | boolean | *false* | Flag to determine if button is disabled or not

Events:
ID | Parameter | Description
--- | --- | ---
@click | *-* | Event called after button click

## Forms fields components

### Generic form field label

Identifier: **opensilex-FormInputLabelHelper**

Description: 

Component used to display form label in a consistent way with optional help message.

This component should not be use directly, prefer use of [FormField](#Generic%20form%20field) component.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
label | string | *-* | Translation key of the form label
labelFor | string | *-* | Name of the corresponding input (should always be present for accessibility purpose, "for" property)
helpMessage | string | *-* | Optional translation key for tooltip help message

### Generic form field

Identifier: **opensilex-FormField**

Description:

Generic component used to display form field with label, help message and validation messages.

This component is an abstraction which should be used by all form components implementations, currently:

- [Input](#Input%20form%20field)
- [Select](#Select%20form%20field)
- [Textarea](#Textarea%20form%20field)
- [Checkbox](#Checkbox%20form%20field)
- [URI](#URI%20form%20field)
- [Type](#Type%20form%20field)
- [File](#file-input-form-field)
- [Tags](#tags-input-form-field)

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
label | string | *-* | Translation key of the form label
helpMessage | string | *-* | Optional translation key for tooltip help message
required | boolean | *false* | Flag to determine if form field is required
rules | string or function | *-* | [vee-validate rules](https://logaretm.github.io/vee-validate/guide/rules.html#rules) or function returning validation rules

Slots:
Name | Props | Description
--- | --- | ---
field | *id*: form input id, *validator*: reference vee-validate validator | This slot allows to define real form field implementation (input, textarea, ...)

### Input form field

Identifier: **opensilex-InputForm**

Description:

Component used to display a form input field.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
value | string | *-* | Input field value (generally used with "sync" flag)
type | string | *text* | HTML input type
label | string | *-* | Translation key of the input form label
helpMessage | string | *-* | Optional translation key for tooltip help message
placeholder | string | *-* | Optional translation key for input placeholder
disabled | boolean | *false* | Flag to determine if form field is disabled or not
required | boolean | *false* | Flag to determine if form field is required
rules | string or function | *-* | [vee-validate rules](https://logaretm.github.io/vee-validate/guide/rules.html#rules) or function returning validation rules
autocomplete | string | *-* | HTML input field ["autocomplete" property value](https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/autocomplete)

### Textarea form field

Identifier: **opensilex-TextAreaForm**

Description:

Component used to display a form textarea field.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
value | string | *-* | Textarea field value (generally used with "sync" flag)
label | string | *-* | Translation key of the textarea form label
helpMessage | string | *-* | Optional translation key for tooltip help message
placeholder | string | *-* | Optional translation key for textarea placeholder
disabled | boolean | *false* | Flag to determine if form field is disabled or not
required | boolean | *false* | Flag to determine if form field is required
rules | string or function | *-* | [vee-validate rules](https://logaretm.github.io/vee-validate/guide/rules.html#rules) or function returning validation rules
autocomplete | string | *-* | HTML textarea field ["autocomplete" property value](https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/autocomplete)

### Checkbox form field

Identifier: **opensilex-CheckboxForm**

Description:

Component used to display a form checkbox field.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
value | string | *-* | Checkbox field value (generally used with "sync" flag)
title | string | *-* | Translation key of the checkbox title (Text of the checkbox option)
label | string | *-* | Translation key of the checkbox form label
helpMessage | string | *-* | Optional translation key for tooltip help message
placeholder | string | *-* | Optional translation key for checkbox placeholder
disabled | boolean | *false* | Flag to determine if form field is disabled or not
required | boolean | *false* | Flag to determine if form field is required
rules | string or function | *-* | [vee-validate rules](https://logaretm.github.io/vee-validate/guide/rules.html#rules) or function returning validation rules

### Select form field

Identifier: **opensilex-SelectForm**

Description:

Component used to display a form select field.

Field can be multiple or single selection and represent either a flat or a tree list.

This component is based on [vue-treeselect](https://vue-treeselect.js.org/) component.

Options are represented by node, see [vue-treeselect documentation](https://vue-treeselect.js.org/#node) for more information.

This component supports different options loading mechanisms :

- options: Directly define fixed array of options (see UserForm language selection for an example)
- optionsLoadingMethod: Asyncronously load options from a webservice when component is loaded (see ExperimentForm1 species selection for an example)
- searchMethod: Asyncronously load options corresponding to user search (see UserSelector for an example)

If result of async calls are not returning vue-treeselect nodes, you must define "conversionMethod" property to transform results into the correct format.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
selection | string or array | *-* | Select field value string is expected if multiple="true" and array otherwise
multiple | boolean | *false* | Determine if this select allow multiple item selection or not
label | string | *-* | Translation key of the select form label
helpMessage | string | *-* | Optional translation key for tooltip help message
placeholder | string | *-* | Optional translation key for select placeholder
disabled | boolean | *false* | Flag to determine if form field is disabled or not
required | boolean | *false* | Flag to determine if form field is required
rules | string or function | *-* | [vee-validate rules](https://logaretm.github.io/vee-validate/guide/rules.html#rules) or function returning validation rules
options | array | *-* | Array of static nodes options
optionsLoadingMethod | function | *-* | Method reference to async call for options list loading
searchMethod | function | *-* | Method reference to async call for options list searching to use in conjonction with *itemLoadingMethod*
itemLoadingMethod | function | *-* | Method reference to async call for selected item loading (used to display elements labels for select using *searchMethod* to get options)
conversionMethod | function | *identity()* | Method used to transform async loaded options and selected items into vue-treeselect nodes
noResultsText | string | *-* | Translation key for message when no results are found with *searchMethod*
flat | boolean | true | Option to determine tree selection behavior (unused when selecting a simple list), see [vue-treeselect documentation](https://vue-treeselect.js.org/#flat-mode-and-sort-values) for more information.

### URI form field

Identifier: **opensilex-UriForm**

Description:

Component used to display optional URI field for semantic element creation or update.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
uri | string | *-* | URI value (generally used with "sync" flag)
editMode | boolean | *false* | Flag to determine if URI field is displayed for creation (possibility to be changed) or edition (simply display disabled input with URI value)
label | string | *-* | Translation key of the checkbox form label
uriGenerated | boolean | *-* | Flag to determine if URI value must be generated or is user defined (generally used with "sync" flag)

### Type form field

Identifier: **opensilex-TypeForm**

Description:

Component used to display a single selection of an Ontological concept subclass.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
type | string | *-* | Ontology selected type URI value (generally used with "sync" flag)
baseType | string | *-* | Ontology type URI to get all subclasses options
placeholder | string | *-* | Optional translation key for type placeholder
disabled | boolean | *false* | Flag to determine if form field is disabled or not
required | boolean | *false* | Flag to determine if form field is required
rules | string or function | *-* | [vee-validate rules](https://logaretm.github.io/vee-validate/guide/rules.html#rules) or function returning validation rules

### CSV file input

Identifier: **opensilex-CSVInputFile**

Description: 

Component used to load csv data.
Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
headersToCheck | Array[String] | [] | Headers to check

Events:

ID | Type | Default value | Description
--- | --- | --- | ---
updated | Array[Any] | *-* | Emit only if is the data has been loaded

Usage:

* In template :
  
```html
<opensilex-CSVInputFile :headersToCheck="['name','comment']" v-on:updated="uploaded"></opensilex-CSVInputFile>
```

* Component callback function :
  
```javascript
 uploaded(data) {
    // use loaded data
 }
```

### File input form field

Identifier: **opensilex-FileInputForm**

Description:

Component used to display a form file input field.

Properties:

Inherited properties of [Generic form field](#generic-form-field)

ID | Type | Default value | Description
--- | --- | --- | ---
file | File | *-* | file value (generally used with "sync" flag)

* In template :
  
```html
   <opensilex-FileInputForm
      :required="true"
      :file.sync="form.file"
      label="data-analysis.application.file"
   ></opensilex-FileInputForm>
```

### Tags input form field 

Identifier: **opensilex-TagInputForm**

Description:

Component used to display a form tags input field.

Properties:

Inherited properties of [Generic form field](#generic-form-field)

ID | Type | Default value | Description
--- | --- | --- | ---
value | Array[String] | *-* | Input field value (generally used with "sync" flag)
label | string | *-* | Translation key of the input form label
helpMessage | string | *-* | Optional translation key for tooltip help message
placeholder | string | *-* | Optional translation key for input placeholder
disabled | boolean | *false* | Flag to determine if form field is disabled or not
required | boolean | *false* | Flag to determine if form field is required
variant | string | - | [Bootstrap color variant](https://getbootstrap.com/docs/4.1/components/buttons/)

* In template :
  
```html
   <opensilex-TagInputForm
      :value.sync="form.tags"
      label="Form.tags"
      helpMessage="Form.tags-help"
      variant="primary"
   ></opensilex-FileInputForm>
```


### Datetime input form field 

**Identifier**: opensilex-DateTimeForm


**Description**: Component used to display a datetime input field.

Based on vue-datetime component (https://github.com/mariomka/vue-datetime).

**Events:**

ID | Parameter | Description
--- | --- | ---
input | - | Event called after input
close | - | Event called after pickup close

**Properties:**

Inherited properties of [Generic form field](#generic-form-field)

ID | Type | Default value | Description
--- | --- | --- | ---
value | string | *-* | Input field value (generally used with "sync" flag)
label | string | *-* | Translation key of the input form label
helpMessage | string | *-* | Optional translation key for tooltip help message
disabled | boolean | *false* | Flag to determine if form field is disabled or not
required | boolean | *false* | Flag to determine if form field is required
minDate | Date | *-* | Used to deactivate the days before the specified date
maxDate | Date | *-* | Used to disable days after the specified date
variant | string | - | [Bootstrap color variant](https://getbootstrap.com/docs/4.1/components/buttons/)

Overloaded properties of vue-datetime

ID | Type | Default value | Description
--- | --- | --- | ---
type | string | datetime | Picker type: date, datetime or time.
value-zone | string | local | Time zone for the value.
zone | string | local | Time zone for the picker.
format | string | dd-MMM-yyyy HH:mm ZZ | Input date format. Luxon presets or tokens. ([Moment formatting with tokens](https://moment.github.io/luxon/docs/manual/formatting.html#formatting-with-tokens--strings-for-cthulhu-))




* In template :
  
```html
   <opensilex-DateTimeForm
      :value.sync="form.date"
      label="Form.date"
      helpMessage="Form.date-help"
      :required="true"
    ></opensilex-DateTimeForm>
```


## Forms containers

### Modal form

Identifier: **opensilex-ModalForm**

Description: 

Component used to display a validated form component in a modal box for creation and update.

Form component should implement the following properties:
- editMode: boolean flag to define if form is displayed in create or update mode
- form: syncronized form object

Form component must implements the following methods:
- getEmptyForm: Method returning an empty form corresponding to form component fields
- reset: Method reseting form component values before modal display (for both creation and update)

Form component could also implements the following methods:
- create: Method taking "form" object as parameter to realize an action when form is validated and edit mode is false
- update: Method taking "form" object as parameter to realize an action when form is validated and edit mode is true

If these methods are not defined "createAction" and/or "updateAction" properties must be defined.

To interact with ModalForm component, use "showCreateForm()" and "showEditForm(form)" methods.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
component | string | *-* | Form component to display in modal for edition and creation
createTitle | string | *-* | Translation key for modal creation form title
editTitle | string | *-* | Translation key for modal edition form title
icon | string | *-* | Icon identifier for modal title, see [IconForm](#Icons) form more information
modalSize | string | *md* | Bootstrap vue modal size ('sm', 'md', 'lg', or 'xl')
createAction | function | *-* | Optional method reference taking "form" object as parameter to realize action when form is validated and edit mode is false, use component "create" method by default.
updateAction | function | *-* | Method reference taking "form" object as parameter to realize action when form is validated and edit mode is true, use component "update" method by default.
initForm | function | *identity()* | Optional method reference taking "form" object as parameter to correctly initialize this object on form creation if needed (add constant values by example)

Events:
ID | Parameter | Description
--- | --- | ---
onCreate | *form* | Event called after sucessfully create action
onUpdate | *form* | Event called after sucessfully update action

### Wizard form

Identifier: **opensilex-WizardForm**

Description: 

Component used to display a multi-steps wizard form components in a modal box for creation and update.

Each form step component should implement the following properties:
- editMode: boolean flag to define if form is displayed in create or update mode
- form: synchronized form object

Form component could implements the following methods:
- reset: Method reseting form step component values before modal display (for both creation and update)
- validate: Method called to validate form step (must return a Promise of boolean corresponding to validation result)

To interact with WizardForm component, use "showCreateForm()" and "showEditForm(form)" methods.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
steps | array | *-* | Array of {title, component} representing wizard steps where title is the options translation key for step title and component is the component id of the step.
createTitle | string | *-* | Translation key for modal creation form title
editTitle | string | *-* | Translation key for modal edition form title
icon | string | *-* | Icon identifier for modal title, see [IconForm](#Icons) form more information
modalSize | string | *md* | Bootstrap vue modal size ('sm', 'md', 'lg', or 'xl')
initForm | function | *-* | Method reference returning empty "form" object to display in steps
createAction | function | *-* | Method reference taking "form" object as parameter to realize action when all steps are validated and edit mode is false.
updateAction | function | *-* | Method reference taking "form" object as parameter to realize action when all steps are validated and edit mode is true.

Events:
ID | Parameter | Description
--- | --- | ---
onCreate | *form* | Event called after sucessfully create action
onUpdate | *form* | Event called after sucessfully update action

## View component

### Uri link

Identifier: **opensilex-UriLink**

Description:

Component used to display a value identifier (uri or other). It allows the user to copy the uri linked to his identifier in this clipboard.

Interaction:  

The clickable link appears by mouse hovering.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
uri | string | *-* | Uri to copy or/and to show
value | string | *-* | Identifier value to show if null uri will be showed
url | string | *-* | External Url to go to
to | string | *-* | Vue Router path to go to

Events:
ID | Parameter | Description
--- | --- | ---
click | *uri* | Event called after click on copy icon.

* In template :

```html 
      <opensilex-UriLink
         :uri="data.item.uri"
         :to="{path: '/experiment/'+ encodeURIComponent(data.item.uri)}"
         ></opensilex-UriLink>
      </template>
```

### TableView

Identifier: **opensilex-TableView**

Description:

Component used to display a list of element without 
calling the webservice API.
See [TableViewAsync](#tableasyncview) for API and asynchronous data.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
fields | array | *-* | b-table fields format
items | array | [] | b-table items format 

Events:

 
* In template :

```html 
  <opensilex-TableView 
      :items="items"
      :fields="fields"
   >
      // Example when formatting uri cell 
      // with a name value
      <template v-slot:cell(name)="{ data }">
         <opensilex-UriLink
         :uri="data.item.uri"
         :value="data.item.name"
         ></opensilex-UriLink>
      </template>
   </opensilex-TableView>
```


### TableAsyncView

// TODO
