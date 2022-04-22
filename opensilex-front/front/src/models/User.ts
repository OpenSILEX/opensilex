import jwt_decode from "jwt-decode";

export class User {
    private firstName: string = "Jean";
    private lastName: string = "Dupont";
    private email: string = "jean.dupont@opensilex.org";
    private admin: boolean = false;
    private locale: string = "en"

    private credentials: Array<string> = [];

    private expire: number = 0;

    private token: string = "";
    private tokenData: any = {};
    public loggedIn: boolean = false;

    public static fromToken(token: string): User {
        try {
            let user = new User();
            user.setToken(token);

            return user;
        } catch (error) {
            console.error("Invalid token", token);
            throw error;
        }
    }

    public static CLAIM_EXPIRE = "exp";
    public static CLAIM_FIRST_NAME = "given_name";
    public static CLAIM_LAST_NAME = "family_name";
    public static CLAIM_EMAIL = "email";
    public static CLAIM_FULL_NAME = "name";
    public static CLAIM_LOCALE = "locale";
    public static CLAIM_IS_ADMIN = "is_admin";
    public static CLAIM_CREDENTIALS_LIST = "credentials_list";

    private static anonymous: User;

    public static ANONYMOUS(): User {
        if (User.anonymous == null) {
            let anonymous = new User();
            anonymous.setFirstName("Anonymous");
            anonymous.setLastName("");
            anonymous.setEmail("anonymous@opensilex.org");

            User.anonymous = anonymous;
        }

        return User.anonymous;
    }

    public getExpiration() {
        return this.expire;
    }

    public getDurationUntilExpirationSeconds() {
        return this.expire - Math.floor(Date.now() / 1000);
    }

    public getDurationUntilExpirationMs() {
        return (this.expire * 1000 - Date.now());
    }

    public getInactivityRenewDelayMs() {
        let expireAfter = this.getDurationUntilExpirationMs();
        return Math.floor((2 / 3) * expireAfter);
    }

    public needRenew() {
        return this.getDurationUntilExpirationMs() > 0 && this.getInactivityRenewDelayMs() <= 0;
    }

    public getToken() {
        return this.token;
    }

    public getAuthorizationHeader() {
        return "Bearer " + this.getToken();
    }


    public setToken(token: string) {
        this.token = token;
        this.tokenData = jwt_decode(token);

        this.firstName = this.getTokenData(User.CLAIM_FIRST_NAME);
        this.lastName = this.getTokenData(User.CLAIM_LAST_NAME);
        this.email = this.getTokenData(User.CLAIM_EMAIL);
        this.admin = this.getTokenData(User.CLAIM_IS_ADMIN);
        this.locale = this.getTokenData(User.CLAIM_LOCALE);
        this.credentials = this.getTokenData(User.CLAIM_CREDENTIALS_LIST);
        console.debug("User credentials:", this.credentials);
        this.loggedIn = true;
        this.expire = parseInt(this.getTokenData(User.CLAIM_EXPIRE));
    }

    public getFirstName() {
        return this.firstName;
    }

    public setFirstName(firstName: string) {
        this.firstName = firstName;
    }

    public getLastName() {
        return this.lastName;
    }

    public setLastName(lastName: string) {
        this.lastName = lastName;
    }

    public getEmail() {
        return this.email;
    }

    public setEmail(email: string) {
        this.email = email;
    }

    public getLocale() {
        return this.locale;
    }

    public setLocale(locale: string) {
        this.locale = locale;
    }

    public isAdmin() {
        return this.admin;
    }

    public setAdmin(admin: boolean) {
        this.admin = admin;
    }

    public isLoggedIn() {
        return this.loggedIn;
    }

    public hasCredential(credential): boolean {
        if (this.isAdmin()) {
            return true;
        }

        return (this.credentials.indexOf(credential) >= 0);
    }

    public hasAllCredentials(credentials): boolean {
        if (this.isAdmin()) {
            return true;
        }

        for (let i in credentials) {
            let credential = credentials[i];
            if (this.credentials.indexOf(credential) < 0) {
                return false;
            }
        }

        return true;
    }

    public hasOneCredential(credentials): boolean {
        if (this.isAdmin()) {
            return true;
        }

        for (let i in credentials) {
            let credential = credentials[i];
            if (this.credentials.indexOf(credential) >= 0) {
                return true;
            }
        }

        return false;
    }

    public getTokenData(key: string) {
        console.debug("Get token data", key, this.tokenData[key], this.tokenData);
        return this.tokenData[key];
    }
}