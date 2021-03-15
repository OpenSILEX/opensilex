 
- [Initiate Replica set on a localhost standalone instance (V4.2 - 15/03/2021)](#initiate-replica-set-on-a-localhost-standalone-instance-v42---15032021)

## Initiate Replica set on a localhost standalone instance (V4.2 - 15/03/2021)

Connect to the specific instance with mongo client by typing ``mongo``.

Execute the following lines.
```bash
rs.initiate({'_id':'opensilex','members':[{'_id':0,'host':'127.0.0.1:27017'}]});
exit
```
Then restart mongodb instance with the replica set name parameter :

* Set replica set, in the mongodb config file (default path : ``/etc/mongod.conf``) :
```yaml
replication:
   replSetName: "opensilex"
net:
   bindIp: localhost 
```

* or by command line at the start of the mongodb instance :
```shell
mongod --replSet opensilex --bind_ip localhost 
```
For more information see, [replica set procedure](https://docs.mongodb.com/manual/tutorial/deploy-replica-set/#procedure).
