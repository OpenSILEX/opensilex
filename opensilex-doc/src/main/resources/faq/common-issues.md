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

# OpenSilex installation errors

## Failure on Tests

Mongo 5.0.0, used for tests, is not compatible with Ubuntu 22.04.2 LTS.
You need to change the Mongo version to 4_0_12 in the file : `AbstractMongoIntegrationTest.java`(line 64).

```sh
$ mongoExec = runtime.prepare(MongodConfig.builder().version(Version.V4_0_12)
```
