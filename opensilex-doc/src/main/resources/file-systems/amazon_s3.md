# S3 storage with OpenSILEX
- **Author** : Renaud COLIN (INRAE MISTEA)
- **Date** : 06/10/2022
- **Tags**: `[S3, Amazon, FileSystem]`

This document describe conception, implementation and usage of an Amazon S3 storage as file-system connection
for OpenSILEX

# Conception

## Authentication

## Configuration

- `endpoint` : 
- `region` : 
- `bucket` : 


## Bucket management 

OpenSILEX handle one bucket per S3 connection and can manage folders inside this bucket.

# How to use

## Configuration

### Single file system

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
                        bucket: opensilex
```

In this example we use S3 for documents and datafiles.

In this case, after some documents and datafile insertion, the overall storage structure will look like this :


```yaml
s3:
    opensilex:
        documents:
            document_1.txt
            document_2.txt
        datafile:
            image1.jpg
            image2.jpg
```


### Multiple file-systems

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
                        bucket: opensilex
                irods:
                    implementation: org.opensilex.fs.irods.IrodsFileSystemConnection
                    config:
                        basePath: /opensilex/
```

In this example we use S3 for documents and IRODS for any datafile.

In this case, after some documents and datafile insertion, the overall storage structure will look like this : 

```yaml
s3:
    opensilex:
        documents:
            document_1.txt
            document_2.txt

irods:
    opensilex:
        datafile:
            image1.jpg
            image2.jpg
```


## Authentication


# Links
- **Endpoints, region and quotas** : https://docs.aws.amazon.com/general/latest/gr/s3.html
- **Using credentials** : https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html
- **API Javadoc** : https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3/S3Client.html
- **Bucket and folders** : https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-folders.html