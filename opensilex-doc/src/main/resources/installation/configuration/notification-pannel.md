---
- notification-pannel.md
- OpenSILEX
- Copyright © INRAE 2023
- Creation date: 02 February, 2024
- Contact: sebastien.prado@inrae.fr
---


# Notification information for users

This configuration option allows you to define an information message that you want to send to users. This could for example be about maintenance information, success of the implementation of a patch, or the upcoming new version of OpenSILEX.
A key is provided for each of the languages available on the information system, and is updated according to the user's choice.

 Configuration for notification key :

```yaml
# ------------------------------------------------------------------------------
# Configuration for module: CoreModule (CoreConfig)
front:
    notificationMessage:
        fr: 'Passage à la version 1.2 le 08 janvier 2024'
        en: 'Upgrade to version 1.2 on January 8, 2024'
```

Messages can include html tags in order to highlight elements, or to refer to the link of a resource :

```yaml
# ------------------------------------------------------------------------------
# Configuration for module: CoreModule (CoreConfig)
front:
    notificationMessage:
        fr: 'Passage à la version 1.2 le 08 janvier 2024 <a href="https://github.com/OpenSILEX/opensilex/releases/tag/1.2.0"> Lien vers le journal des modification de la version</a>'
        en: 'Upgrade to version 1.2 on January 8, 2024 <a href="https://github.com/OpenSILEX/opensilex/releases/tag/1.2.0"> Link to the release changelog</a>'
```


# Choice of color theme for the notification space

The notification banner has a default green style. However, it is possible to replace it by choosing one of the two other styles available. You can choose `warning` style with a red background, or `information` style with a blue background.
The value is not case sensitive and accepts uppercase letters as well :


```yaml
# ------------------------------------------------------------------------------
# Configuration for module: CoreModule (CoreConfig)
front:
    notificationColorTheme: "Warning"

```


# Configuration of the display end date

It is possible to define an end date from which the notification will no longer be visible on the information system interface.
The format should be: `"YYYY-MM-DD"` :

```yaml
# ------------------------------------------------------------------------------
# Configuration for module: CoreModule (CoreConfig)
front:
    notificationEndDate: "2024-02-13"
```