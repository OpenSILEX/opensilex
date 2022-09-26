
# Email

E-mail service for password renewal will only works with a stmp server which accept username and password as identifiers.

From May 30, 2022, ​​Google no longer supports the use of third-party apps or devices which ask you to sign in to your Google Account using only your username and password. See [Less secure apps & your Google Account](https://support.google.com/accounts/answer/6010255#zippy=%2Cif-less-secure-app-access-is-on-for-your-account)

# ---- DEPRECATED ---------

## Configure smtp for gmail in order to use javax.mail

You need to connect to your account.

### For non headless machine

Open you browser and connect to your account and it's done.

### For headless servers (Debian)

You need to connect to your account.

You need to open this following page in your browser : https://accounts.google.com/DisplayUnlockCaptcha

Click on activate.

Then connect to your server with ssh.

Install openssl library.

```
sudo apt install openssl
```

* Start session from terminal:

```bash
openssl s_client -connect smtp.gmail.com:25 -starttls smtp
```

The last line of the response should be "250 SMTPUTF8"

* Initiate login

```bash
auth login
```

This should return "334 VXNlcm5hbWU6".

**Type username**

* Type your username in base64 encoding (eg. echo -n 'your-username' | base64)

This should return "334 UGFzc3dvcmQ6"

**Type password**

* Type your password in base64 encoding (eg. echo -n 'your-password' | base64)

* Success

You're are successfully logged in


*Sources :*

* https://stackoverflow.com/questions/10013736/how-can-i-avoid-google-mail-server-asking-me-to-log-in-via-browser

* https://stackoverflow.com/questions/1516754/connecting-to-smtp-gmail-com-via-command-line