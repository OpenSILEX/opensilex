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

In Opensilex there is no user entity, what we call user is an Account, with or without a Holder (a Person with a name...)

JWT token contains following claims:
- iss: opensilex (issuer)
- sub: Account URI (subject)
- iat: Date of token creation (issued at)
- exp: Date of token expiration (expires at)
- given_name: Holder first name (if account has a holder)
- family_name: Holder last name (if account has a holder)
- email: Account email
- name: User displayed name (email)
- is_admin: Boolean to determine if Account is admin
- credentials_list: Array of credentials allowed to this Account
- experiments_list: List of experiments the user has access to, subject to a maximum limit.
- experiments_exceed_limit: Boolean indicating if the number of experiments exceeds the maximum limit.

The experiments_list field is limited to 100 experiments based on JWT size constraints. If this limit is exceeded, the experiments_exceed_limit field is set to true and replaces experiments_list.

If user is admin then accept incoming request.

Otherwise if the requested API method is annotated with `@ApiCredential` then accept incoming request ONLY if defined credential ID is in Account `credentials_list`

See: `org.opensilex.rest.authentication.AuthenticationFilter`
See: `org.opensilex.rest.authentication.AuthenticationService`

# Credentials organisation

// TODO: Users, Groups and Profiles

