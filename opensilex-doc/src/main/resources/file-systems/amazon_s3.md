# S3 storage with OpenSILEX
- **Description**: Conception, implementation and usage of an Amazon S3 storage as file-system connection
  for OpenSILEX
- **Author** : Renaud COLIN (INRAE MISTEA)
- **Date** : 06/10/2022
- **Tags**: `[S3, Amazon, FileSystem]`

# Conception

## Bucket and file management 

OpenSILEX handle one bucket per S3 connection and can manage folders inside this bucket.

### Upload

Uploading a datafile with : 
- `http://www.phenome-fppn.fr/id/file/1661036401.fcf0933a40d6bc61bbdb89d625c44ec1` : URI of the datafile
- `datafile/aHR0cDovL3d3dy5waGVub21l`: file path generated from datafile URI

**Java file service usage**

```java
fs.writeFile("datafile","datafile/aHR0cDovL3d3dy5waGVub21l",file);
```

**S3 storage overview** 

```yaml
s3:
    opensilex-bucket:
        datafile/aHR0cDovL3d3dy5waGVub21l
```

### Declaration

Declaring an existing datafile with : 
- `datafile/sub_directory/LWZwcG4uZnIvaWQvZmlsZS8x` : relative path of the existing datafile
- `/datafile/sub_directory/LWZwcG4uZnIvaWQvZmlsZS8x` : absolute path of the existing datafile

The S3 connection has no notion of base/root path, so calling `S3FileStorageConnection.getAbsolutePath(path)`
will just return `path`

**Java file service usage**

```java
fs.exists("datafile","datafile/sub_directory/LWZwcG4uZnIvaWQvZmlsZS8x");
fs.exists("datafile","/datafile/sub_directory/LWZwcG4uZnIvaWQvZmlsZS8x");
```

**S3 storage overview**

```yaml
s3:
    opensilex-bucket:
        datafile/sub_directory/LWZwcG4uZnIvaWQvZmlsZS8x       
```

## Authentication

## Configuration

- `endpoint` :
- `region` :
- `bucket` :
- `crendentialProvider : `


## Reading and writing on a bucket

## Performances

## Logging and monitoring


# How to use

## Single file system

```yaml
file-system:
    fs:
        config:
            defaultFS: S3      
            connections:
                S3:
                    implementation: org.opensilex.fs.s3.S3FileStorageConnection
                    config:
                        endpoint: s3-website.eu-west-3.amazonaws.com
                        region: eu-west-3
                        bucket: opensilex-bucket
```


## Multiple file-systems

```yaml
file-system:
    fs:
        config:
            customPath:
                datafile/: irods
                documents/: S3    
                
            connections:
                S3:
                    implementation: org.opensilex.fs.s3.S3FileStorageConnection
                    config:
                        endpoint: s3-website.eu-west-3.amazonaws.com
                        region: eu-west-3
                        bucket: opensilex-bucket
                irods:
                    implementation: org.opensilex.fs.irods.IrodsFileSystemConnection
                    config:
                        basePath: /opensilex/
```

## Authentication


# Links

## Developers

- **API Javadoc** : https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/S3Client.html
- **Using credentials** : https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html
- **Transfer Manager** : https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/transfer-manager.html
- **Transfer Manager** : https://github.com/aws/aws-sdk-java-v2/tree/master/services-custom/s3-transfer-manager

## Users/Administrators
- **Endpoints, region and quotas** : https://docs.aws.amazon.com/general/latest/gr/s3.html
- **Bucket and folders** : https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-folders.html
- **Credentials files** : https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html