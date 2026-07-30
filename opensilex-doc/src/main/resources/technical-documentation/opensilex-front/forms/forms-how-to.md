# Technical documentation : [front-end, form, modalform] How forms are done in OpenSILEX

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)          | OpenSILEX version  | Comment           |
|------------|--------------------|--------------------|-------------------|
| 30-07-2026 | yvan.roux@inrae.fr | vue3 futur version | Document creation |

> ⚠️ _WARNING_ : This document is not about wizard forms (forms with multiple steps). To find the documentation for wizard forms see [wizard section of components.md](../components.md#wizard-form)

## Table of contents

<!-- TOC -->
* [Technical documentation : [`{category}`] `{title}`](#technical-documentation--category-title)
  * [Table of contents](#table-of-contents)
  * [Definitions](#definitions)
  * [functional requirements](#functional-requirements)
  * [Non-functional requirements](#non-functional-requirements)
  * [Solution](#solution)
  * [Technical specifications](#technical-specifications)
    * [Technical definitions](#technical-definitions)
    * [Detailed explanations](#detailed-explanations)
    * [Tests](#tests)
    * [Environment](#environment)
  * [Limitations and improvements](#limitations-and-improvements)
  * [Documentation](#documentation)
<!-- TOC -->

## About this documentation

This documentation aims to help developers to understand how forms are done in OpenSILEX.

Forms are everywhere in OpenSILEX, it is important to try keeping them as much homogeneous as possible, as it will reduce maintenance complexity.

## Solution

### useModalFormLogic

useModalFormLogic.ts is a composable allowing factorizing the logic of a form. It handles :
- form validation before submission
- error handling on submission failure
- loader usage during submission

#### Validation before submission

thanks to the reference to the n-form component, form validation is handled by the useFormValidation composable.
To define the form validation rules, use the `rules` prop of the n-form component and the `path` prop of n-item components.

see naive UI documentation for more details or just check for examples in any form of openSILEX.

### Life cycle of a form handled by useModalFormLogic

- before the form shows up (edit or create), useModalFormLogic will call the `reset` callback function you gave him.
You can freely use the isUpdateMode property of the useModalFormLogic composable as it will be updated before calling the `reset` callback
- in creation mode only, before the form shows up, form data will be initialized with the default value returned by the
`getEmptyForm` callback ONLY if you called `showCreateForm` without argument (or with a null argument).

### Keep standard forms

In order to reduce the maintenance complexity, try to keep standard forms as much as possible.
- do not try to handle API call errors, let the useModalFormLogic composable handle it.
- keep standard emits when possible : onCreate, onUpdate, onSuccess.

## Limitations and improvements

- Typing is not very coherent for now. Using same DTO for create and update is incoherent. Due to this, correct typing of `onCreate` and `onUpdate` emits is not possible.
- By decoupling the form and the modal, it should be possible to keep the same standard form for Wizard forms steps.

## Documentation

- see the Naive UI official documentation on form validation and components: https://www.naiveui.com/en-US/os-theme/components/form