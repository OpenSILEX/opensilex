# Configuration : Tracking with Matomo

**Document history (please add a line when you edit the document)**

| Date       | Editor(s)        | OpenSILEX version | Comment           |
|------------|------------------|-------------------|-------------------|
| 2023-11-14 | Valentin Rigolle | 1.1.0             | Document creation |

## Table of contents

<!-- TOC -->
* [Configuration : Tracking with Matomo](#configuration--tracking-with-matomo)
  * [Table of contents](#table-of-contents)
  * [Set up a Matomo server](#set-up-a-matomo-server)
    * [Register the OpenSilex instance](#register-the-opensilex-instance)
  * [OpenSilex configuration file](#opensilex-configuration-file)
<!-- TOC -->

## Set up a Matomo server

Please refer to the Matomo documentation to set up a Matomo server.

### Register the OpenSilex instance

Once your Matomo server is up and running, you have to register your OpenSilex instance in it. You juste have to 
specify the base URL of the instance. Matomo will assign a `site ID` to this instance (for example `1`, if this is 
the first website you added in your Matomo server). You will need this ID to correctly configure your OpenSilex 
instance.

## OpenSilex configuration file

You must specify the Matomo server in your OpenSilex instance configuration. You also need to know the `site ID` of 
your OpenSilex instance in Matomo.

```yaml
front:
  matomo:
    serverUrl: http://localhost:7777/
    siteId: 1
```