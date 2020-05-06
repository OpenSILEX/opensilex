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

## Forms

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
autocomplete | string | HTML input field ["autocomplete" property value](https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes/autocomplete)

### Select form field
### Textarea form field
### Checkbox form field
### URI form field
### Type form field


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
