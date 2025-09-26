# Specifications : [person - law] GDPR

**Document history (please add a line when you edit the document)**

| Date       | Editor(s) | OpenSILEX version     | Comment           |
|------------|-----------|-----------------------|-------------------|
| 20/10/2023 | Yvan Roux | 1.1.0 Blazing Basalt  | Document creation |

## Table of contents

<!-- TOC -->
* [Specifications : [person - law] GDPR](#specifications--person---law-gdpr)
  * [Table of contents](#table-of-contents)
  * [Needs](#needs)
    * [Non-functional requirements](#non-functional-requirements)
  * [Solution](#solution)
    * [Business logic](#business-logic)
  * [Technical specifications](#technical-specifications)
    * [Detailed explanations](#detailed-explanations)
      * [New web service](#new-web-service)
      * [VueJs component](#vuejs-component)
  * [Documentation](#documentation)
<!-- TOC -->

## Needs

In order to respect the GDPR, we have to inform our users on which personal data do we store and what for.

- Use case : As any type of user I want to know more about how my OpenSilex instance store my personal data.

### Non-functional requirements

- **Flexibility** : The GDPR text must be easily changed by administrators (from OpenSilex team), without having to re-deploy the instance (or even without having to restart).
- **Internationalization**: The GDPR text must be available in as many languages as necessary for each instance. 

## Solution

We decided to directly display PDF files into the GDPR page. The GDPR PDF files are stored on the server instance and send to the front using the API. This way, changing the RGPD text mean replacing a file with another one, having the exact same path and name.

Paths of GDPR files are written in the security part of the config. You can declare as many file as you want, in relation with its redaction language. See [GDPR_config.md](../installation/configuration/GDPR_config.md).

### Business logic

When clicking on the GDPR page, the component request the API for de document with the language actually used in the interface. Then it display the PDF file. 

To send the file, the web service follow this logic :

- If there is no config set or if the file of the config doesn't exist, it returns an error response.
- If the file is not available in the requested language, or if language was not specified, it will send the version in the language of the current user.
- If the user is not connected or if the file is not available in its language, it will send the file in the default language of the OpenSilex instance.
- If this file doesn't exist, it sends the file in the first language of the config.

## Technical specifications

### Detailed explanations

#### New web service

There is a new GDPR web service (GET) in the PersonAPI with following path : /security/persons/GDPR .

This web service try to find the file according to the config. For more logic implementation see [Business Logic](#business-logic).

#### VueJs component

The GDPR Vue component call the new API endpoint and get the blob pdf (using methods of OpenSilexVuePlugin.ts) and display it.

Before all, the component checks if the config was set (via the front config), and display a special message if not, without trying to get the file via the new API endpoint.

## Documentation

For configuration instructions see [GDPR_config.md](../installation/configuration/GDPR_config.md).