OpenSilex security architecture
================================

# Basic security mechanism

User security mechanism is implemented at API method level.

Every request is intercept before execution and checked uppon defined credentials for the current user.

If the requested API method is annotated with `@ApiProtected`, it requires at least an authenticated user.

User are authenticated by a [JWT token](https://jwt.io/) which must be send in header "Authorization" of all request which access protected API method.

"Authorization" header value must be the JWT token prefixed with "Bearer ".

If the requested API method is NOT annotated with `@ApiProtected`, then it's a publicly accessible API method.

User can get a token by calling `/security/authenticate` POST API method with valid identifier (email or uri) and password.

[Hashed password are stored in sparql database and hashed/validated with BCrypt (Hash complexity: 12).](https://github.com/patrickfav/bcrypt)

[JWT token is signed/verified with RSA keys (Algorithm: RSA512, Key size: 2048).](https://github.com/auth0/java-jwt)

If user token is not verified, then incoming request is automatically rejected.

JWT token contains following claims:
- iss: opensilex (issuer)
- sub: User URI (subject)
- iat: Date of token creation (issued at)
- exp: Date of token expiration (expires at)
- given_name: User first name
- family_name: User last name
- email: User email
- name: User displayed name
- is_admin: Boolean to determine if user is admin
- credentials_list: Array of credentials allowed to this user

If user is admin then accept incoming request.

Otherwise if the requested API method is annotated with `@ApiCredential` then accept incoming request ONLY if defined credential ID is in user `credentials_list`

See: `org.opensilex.rest.authentication.AuthenticationFilter`
See: `org.opensilex.rest.authentication.AuthenticationService`

# Credentials organisation




