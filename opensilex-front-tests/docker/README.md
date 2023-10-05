# opensilex-front-tests-docker

## Description

This repository contains the docker image for testing the frontend of OpenSILEX with playwright.
This image is meant to be used on gitlab ci.

## Making changes/updating the image

To make changes you can update the `Dockerfile`.
Once the changes are done you can build the image like this :

```sh
docker build -t registry.forgemia.inra.fr/opensilex/opensilex-dev/opensilex-front-tests ../.. -f Dockerfile
```

## Updating the image used by gitlab ci

To update the image used by gitlab ci you can follow gitlab's documentation here : <https://docs.gitlab.com/ee/user/packages/container_registry/>

Or follow this quick summary :

* Generate an acces token with read and write registry rights in your profile
* Use these token name and token to log in with docker :
  
  ```sh
  docker login registry.forgemia.inra.fr/opensilex/opensilex-dev -u <my-token-name> -p <my-token>
  ```

* To push the image to the registry :
  
  ```sh
  docker push registry.forgemia.inra.fr/opensilex/opensilex-dev/opensilex-front-tests
  ```
