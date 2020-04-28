Databases
=============

## MongoDB

### MongoDB docker

1. Resolve error NotMasterOrSecondary: 'not master or secondary (on mongo 4.2.X)

- Connect to mongo console of the docker container, you will be logged as *opensilex:OTHER*.

- Set rsconf members 0 host value with the container ID value :
```bash
$ opensilex:OTHER> rsconf.members = [{_id: 0, host: "d64c8d20c7f4:27017"}]
```

- Apply this new config to the replicaSet :
```bash
$ rs.reconfig(rsconf, {force: true})
```
```bash
[ { "_id" : 0, "host" : "d64c8d20c7f4:27017" } ]
{
	"ok" : 1,
	"$clusterTime" : {
		"clusterTime" : Timestamp(1584107028, 1),
		"signature" : {
			"hash" : BinData(0,"AAAAAAAAAAAAAAAAAAAAAAAAAAA="),
			"keyId" : NumberLong(0)
		}
	},
	"operationTime" : Timestamp(1584107028, 1)
}
opensilex:OTHER>
opensilex:PRIMARY>
```

You are no logged as *opensilex:PRIMARY*.