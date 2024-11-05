
<!-- TOC -->
* [Building errors:](#building-errors)
  * [Can't find node module](#cant-find-node-module)
  * [Error: ENOSPC: System limit for number of file watchers reached](#error-enospc-system-limit-for-number-of-file-watchers-reached-)
* [Run issues](#run-issues)
  * [Authentication](#authentication)
    * [Unexpected internal error - java.lang.IllegalStateException, message": "size = 2"](#unexpected-internal-error---javalangillegalstateexception-message-size--2)
  * [IntelliJ: java.lang.ClassNotFoundException](#intellij-javalangclassnotfoundexception)
  * [External services certificate issues](#external-services-certificate-issues)
* [OpenSilex installation errors](#opensilex-installation-errors)
  * [Failure on Tests](#failure-on-tests)
<!-- TOC -->

# Building errors:
## Can't find node module
If you get an error while building the front part, it could be due to finding library issues. For example, you get this error "Error: Cannot find module 'ajv'" whereas you can find it in the "node-modules" folder.
  
To resolve this issue, try this :
- In *opensilex-dev/.node/node*, run this command: 

```
node yarn/dist/bin/yarn.js cache clean 
```

- In opensilex-dev-tools, run the file ResetNodeModules.java  
This will delete every node module and yarn.lock files from all the application modules
  
- Build the whole project

##  Error: ENOSPC: System limit for number of file watchers reached 

The number of files monitored by the system has reached the limit
You can fix it, that increasing the amount of inotify watchers.

If you are not interested in the technical details and only want to get Listen to work:

If you are running Debian, RedHat, or another similar Linux distribution, run the following in a terminal:

```sh
$ echo fs.inotify.max_user_watches=524288 | sudo tee -a /etc/sysctl.conf && sudo sysctl -p
```

If you are running ArchLinux, run the following command instead

```sh
$ echo fs.inotify.max_user_watches=524288 | sudo tee /etc/sysctl.d/40-max-user-watches.conf && sudo sysctl --system
```

Then paste it in your terminal and press on enter to run it.

**The Technical Details**

Listen uses inotify by default on Linux to monitor directories for changes. It's not uncommon to encounter a system limit on the number of files you can monitor. For example, Ubuntu Lucid's (64bit) inotify limit is set to 8192.

You can get your current inotify file watch limit by executing:

```sh
$ cat /proc/sys/fs/inotify/max_user_watches
When this limit is not enough to monitor all files inside a directory, the limit must be increased for Listen to work properly.
```

You can set a new limit temporary with:
```sh
$ sudo sysctl fs.inotify.max_user_watches=524288
$ sudo sysctl -p
```

# Run issues

## Authentication

### Unexpected internal error - java.lang.IllegalStateException, message": "size = 2"

- **How to reproduce** : This errors is returned when you try to login (via UI or via Swagger UI) and only occurs if you have a JAVA version >= 17.
- **Cause** : It's due to the dependency [ByteBuddy](https://bytebuddy.net/)
- **How to solve** : use a JAVA version <= 14

## IntelliJ: java.lang.ClassNotFoundException

When you comme across this issue or something similar :
```
java.lang.ClassNotFoundException: org.opensilex.dev.StartServerWithFront
```
```
java.lang.ClassNotFoundException: org.opensilex.dev.StartServer
```

You can fix it by changing this parameter: Structure> project> compiler output
to the target directory at the root of this directory (opensilex-dev/target)

## External services certificate issues

Errors with external services certificates will usually manifest as this exception :
```json
{
  "title": "Unexpected internal error - javax.net.ssl.SSLHandshakeException",
  "message": "PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target"
}
```

Known affected APIs are the dataverse and the agroportal APIs.

To fix this issue you need to add the certificate of the certification authority (CA) to the trusted sources on your machine.
Known issues usually relate to `Let's Encrypt` and this CA : `ISRG Root X1`.
See [Let's Encrypt documentation](https://letsencrypt.org/certificates/).
To add a trusted source run these commands (example here for `ISRG Root X1` but can easily be adapted to other CAs) :
__NOTE__ : `cacerts` is an encrypted file. Its default password is `changeit`.

```shell
cd /usr/lib/jvm/default-java/lib/security; # This path can be different depending on your OS. Locate where your cacerts file is
keytool -list -v -keystore cacerts  > java_cacerts.txt; # This is just to make a backup in case something goes wrong
wget https://letsencrypt.org/certs/isrgrootx1.der; # Get the DER file to add to trusted sources
keytool -import -alias isrgrootx1 -keystore cacerts -file ./isrgrootx1.der
```

If a certificate with the same alias already exists you may need to remove it first :

```shell
keytool -delete -keystore cacerts -alias isrgrootx1
```

You have to restart OpenSILEX for this to be taken into account.

# OpenSilex installation errors

## Failure on Tests

Mongo 6.0.2, used for tests, is not natively compatible with Ubuntu 22.04.2 LTS.

To fix that you just have to install the ssl1.1 library with the following commands.
```shell
echo "deb http://security.ubuntu.com/ubuntu focal-security main" | sudo tee /etc/apt/sources.list.d/focal-security.list

sudo apt-get update
sudo apt-get install libssl1.1
```

```shell
sudo rm /etc/apt/sources.list.d/focal-security.list
```

solution from [askubuntu website](https://askubuntu.com/questions/1403619/mongodb-install-fails-on-ubuntu-22-04-depends-on-libssl1-1-but-it-is-not-insta/1403683#1403683)
