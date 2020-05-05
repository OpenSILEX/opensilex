OpenSilex Vue.JS components
===========================

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

This component is the base for all button displayed in OpenSilex based on [Bootstrap-vue button.](https://bootstrap-vue.org/docs/components/button)

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
icon | *-* | This slot allow you to completly replace button icon (usefull if you need to switch icon depending of some other variable).

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

Button component for "Delete" action, this component display a confirmation message box before emitting "click" event.

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
field | *id*: form input id, *validator*: reference vee-validate validator | This slot allow to define real form field implementation (input, textarea, ...)

### Input form field

Identifier: **opensilex-InputForm**

Description: 

Component used to display a form input field.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
value | string | *-* | Input field value (generally used whith "sync" flag)
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
value | string | *-* | Textarea field value (generally used whith "sync" flag)
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
value | string | *-* | Checkbox field value (generally used whith "sync" flag)
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

This component support different options loading mechanisms :
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
uri | string | *-* | URI value (generally used whith "sync" flag)
editMode | boolean | *false* | Flag to determine if URI field is displayed for creation (possibility to be changed) or edition (simply display disabled input with URI value)
label | string | *-* | Translation key of the checkbox form label
uriGenerated | boolean | *-* | Flag to determine if URI value must be generated or is user defined (generally used whith "sync" flag)

### Type form field

Identifier: **opensilex-TypeForm**

Description: 

Component used to display a single selection of an Ontological concept subclass.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
type | string | *-* | Ontology selected type URI value (generally used whith "sync" flag)
baseType | string | *-* | Ontology type URI to get all subclasses options
placeholder | string | *-* | Optional translation key for type placeholder
disabled | boolean | *false* | Flag to determine if form field is disabled or not
required | boolean | *false* | Flag to determine if form field is required
rules | string or function | *-* | [vee-validate rules](https://logaretm.github.io/vee-validate/guide/rules.html#rules) or function returning validation rules

### Input local name field

Identifier: **opensilex-LocalNameInputForm**

Description: 

Component used to display a form local name input field.

Properties:

ID | Type | Default value | Description
--- | --- | --- | ---
value | string | *-* | Input field value (generally used whith "sync" flag)
type | string | *text* | HTML input type
label | string | *-* | Translation key of the input form label
helpMessage | string | *-* | Optional translation key for tooltip help message
placeholder | string | *-* | Optional translation key for input placeholder
disabled | boolean | *false* | Flag to determine if form field is disabled or not
required | boolean | *false* | Flag to determine if form field is required
rules | string or function | *-* | [vee-validate rules](https://logaretm.github.io/vee-validate/guide/rules.html#rules) or function returning validation rules
autocomplete | string | HTML input field ["autocomplete" property value](https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/autocomplete)


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

## Forms containers

### Modal form

Identifier: **opensilex-ModalForm**

Description: 

Component used to display a validated form component in a modal box for creation and update.

Form component should implements the following properties:
- editMode: boolean flag to define if form is displayed in create or update mode
- form: syncronized form object

Form component must implements the following methods:
- getEmptyForm: Method returning an empty form corresponding to form component fields
- reset: Method reseting form component values before modal display (for both creation and update)

Form component could also implements the following methods:
- create: Method taking "form" object as parameter to realize action when form is validated and edit mode is false
- update: Method taking "form" object as parameter to realize action when form is validated and edit mode is true

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

Component used to display a multi-steps wizard forms components in a modal box for creation and update.

Each form step component should implements the following properties:
- editMode: boolean flag to define if form is displayed in create or update mode
- form: syncronized form object

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
