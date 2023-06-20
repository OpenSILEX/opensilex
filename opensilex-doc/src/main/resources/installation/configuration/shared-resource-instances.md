***************************************************
* OpenSILEX - Configuring shared resource instances
* Copyright © INRAE 2023
* Creation date: 2023-02-14
* Contact: valentin.rigolle@inrae.fr
***************************************************

# Configuring shared resource instances

OpenSILEX supports the connexion to other OpenSILEX instances for the purpose of browsing or retrieving resources like
variables. Those instances are called **shared resource instances** or **SRI** for short.

The configuration takes place in the main configuration file, under the `core` section. You need to add a `sharedResourceInstances`
subsection, as shown in the example below :

```yaml
core:
    sharedResourceInstances:
      - uri: http://phenome.inrae.fr/resources
        apiUrl: http://phenome.inrae.fr/resources/rest
        label:
            fr: "PHENOME"
            en: "PHENOME"
        accountName: guest@opensilex.org
        accountPassword: guest
      - uri: http://138.102.159.36:8082/
        apiUrl: http://138.102.159.36:8082/rest
        label:
            fr: "EMPHASIS"
            en: "EMPHASIS"
        accountName: guest@opensilex.org
        accountPassword: guest
```

## Options

All the fields are required, except `label` (but it is still recommended to provide at least one label).

| field             | type                  | description                                                                                                                                                         |
|-------------------|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `uri`             | `String`              | Unique identifier of the SRI, must be a URI. For example, `http://phenome.inrae.fr/resources` is a valid value.                                                     |
| `apiUrl`          | `String`              | Access point for the REST API (ending with `/rest`). For example, `http://phenome.inrae.fr/resources/rest` is a valid value.                                        |
| `label`           | `Map<String, String>` | List of labels of the IRP. Supported keys are only `fr` and `en` at the moment. If the requested language is not found in the labels, the URI will be used instead. |
| `accountName`     | `String`              | The identifier of the account that will be used to connect to the SRI.                                                                                              |
| `accountPassword` | `String`              | The password of the account that will be used to connect to the SRI.                                                                                                |
