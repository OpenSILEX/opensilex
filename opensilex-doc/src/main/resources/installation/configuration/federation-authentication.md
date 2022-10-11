***********************************************************
* OpenSILEX - Authentication through an identity federation
* Copyright © INRAE 2022
* Creation date: 2022-10-03
* Contact: valentin.rigolle@inrae.fr
***********************************************************

<!-- TOC -->
* [Authentication through an identity federation](#authentication-through-an-identity-federation)
  * [SAML](#saml)
  * [OpenID](#openid)
<!-- TOC -->

# Authentication through an identity federation

OpenSILEX supports two federated authentication methods : SAML and OpenID. This document will
present a brief overview on how to set up the configuration for both of them.

Note: this document only explains how OpenSILEX should be configured to support these kinds of
authentication. This document does NOT explain how to register your OpenSILEX instance to an 
identity federation. Please refer to the specific documentation of your federation for further
information on this matter (e.g. RENATER, EGI).

## SAML

To support SAML authentication, OpenSILEX relies on an external software called Shibboleth,
which implements the service provider side of the SAML protocol. In addition to configure
OpenSILEX, you will need to install an Apache2 server with the Shibboleth module to allow for
SAML authentication. In order to simplify the process, it is heavily recommended to use [OSP
(OpenSILEX SAML Proxy)](https://forgemia.inra.fr/OpenSILEX/opensilex-saml-proxy), which provides
a Docker container with Apache2 and Shibboleth, as well as a quick configuration script and a 
complete documentation. The configuration presented in this document will assume that you use
OSP, or a similar architecture, as your service provider. Furthermore, it is strongly advised
to read the associated documentation to understand the architecture and concepts mentioned in
this section.

Concerning OpenSILEX, all the configuration will take place in the main configuration file,
in the `server` and `security` sections.

In the `server` section, you will need to specify a port that Tomcat will listen to using the 
AJP protocol. This protocol is necessary to allow for direct communication between the Apache2
server and Tomcat. By default, the port is 8009. You can also configure a secret to protect
messages sent from Apache2 to Tomcat, however it is not necessary (as the communication will
probably happen on the same machine).

```yaml
server:
    # Configuration for the AJP connector for communication between Apache and Tomcat.
    # If you intend to use SAML/Shibboleth, you MUST configure this.
    ajpConnector:
        enable: true
        # The connector port where Tomcat will listen to AJP messages
        port: 8009
        # The secret is optional. This option is only supported from version 2.4.42 of Apache server
        secret: mysecret
```

In the `security` section, you will have to specify two URIs. The first one, `samlProxyLoginURI`,
should be an endpoint of your SAML service provider serving as proxy to the AJP SAML login endpoint
of OpenSILEX. This endpoint should be protected by Shibboleth, so that the authentication process 
will be automatically initiated when the user tries to access it.

The second URI is the `samlLandingPageURI`, which is the route of the landing page the user will be
redirected to after authenticating though SAML. In most cases, it should be the public URI of
the instance, followed by `/app/saml` or `<pathPrefix>/app/saml` if the path prefix is defined.

The `attributes` optional subsection allows to specify the name of the SAML attributes transmitted by
Apache2. It should correspond to the names defined in the `attribute-map.xml` configuration file of
Shibboleth. The default values match the SAML convention, so they should be fine in most cases.

The `connectionTitle` subsection is used to specify a custom label for the SAML option of the
login page.

```yaml
security:
    # Configuration for connecting to an SAML/Shibboleth identity federation (e.g. RENATER)
    # Please note that the `server.ajpConnector` MUST also be configured in order for the SAML
    # authentication to work
    saml:
        enable: true
        # The SAML proxy URI protected by Shibboleth to initiate SSO login.
        samlProxyLoginURI: http://localhost/opensilex-saml
        # The landing page where the user is redirected to after logging in (it should be the publicUri followed by
        # /app/saml, or <pathPrefix>/app/saml if the pathPrefix is defined)
        samlLandingPageURI: http://localhost:8080/app/saml
        # Attribute names as defined in attribute-map.xml
        attributes:
            email: mail
            firstName: givenName
            lastName: sn
        connectionTitle:
            fr: Connexion avec RENATER
            en: Log in with RENATER
```

## OpenID

To use OpenID as an authentication method, you will need to add an `openID` subsection in the 
`security` section of the main configuration file.

The `providerURI` corresponds to the URI of your identity federation. 

The `redirectURI` is the URI of the landing page the user will be redirected to after logging in
with OpenID. In most cases, it should be the public URI of the instance, followed by `/app/openid` 
or `<pathPrefix>/app/openid` if the path prefix is defined.

The `clientID` and `clientSecret` are the values that identify your service provider inside your
federation. You should get them when after registering your OpenSILEX instance into your federation.

You can also specify a `connectionTitle` subsection, when you can define the labels for the OpenID
connection option in the login page.

```yaml
security:
    openID:
        enable: true
        providerURI: http://aai.egi.eu/auth/realms/egi/
        redirectURI: http://localhost:8080/app/openid
        clientID: my-id
        clientSecret: my-secret
        connectionTitle:
            fr: Connexion avec EGI Check-in
            en: Connect with EGI Check-in
```