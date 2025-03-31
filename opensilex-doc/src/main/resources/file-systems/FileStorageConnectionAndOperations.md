---
title: File storage connection and operations
---


<!-- TOC -->
* [File storage](#file-storage)
  * [Features](#features)
  * [Implementations](#implementations)
    * [S3](#s3)
    * [Local](#local)
    * [iRODS](#irods)
    * [GridFS](#gridfs)
* [Operations](#operations)
  * [Checksum](#checksum)
  * [Thumbnail](#thumbnail)
    * [ImageMagick](#imagemagick)
    * [Java img](#java-img)
* [Storage/Operations optimization](#storageoperations-optimization)
  * [Checksum](#checksum-1)
    * [iRODS/ImageMagick (server-side)](#irodsimagemagick-server-side)
  * [Thumbnail](#thumbnail-1)
    * [iRODS/ImageMagick (client-side)](#irodsimagemagick-client-side)
    * [iRODS/Java Img (client-side)](#irodsjava-img-client-side)
<!-- TOC -->

# File storage

## Features

## Implementations

### S3

### Local

### iRODS

### GridFS

# Operations

## Checksum

## Thumbnail

### ImageMagick

### Java img

# Storage/Operations optimization

## Checksum

### iRODS/ImageMagick (server-side)

- Execute `ichecksum`

## Thumbnail

### iRODS/ImageMagick (client-side)

> Steps

- Execute `istream` command for read the original img
- Pipe output to the imagemagick `convert` command 
- Redirect the process output and store thumbnail as byte[]

> Performance impact

- [OK] No additional disk write of input file from iRODS
- [OK] No additional disk write of thumbnail output
- [KO] Still has to fetch source file from remote fs (Network/Disk)

> Design

- Create a optimized operation for iRODS/ImageMagick
- Direct execution of the piped command
  - `istream <irods_options> <remote_path> | convert <convert_options>`
  - require access to irods connexion/irods command options


### iRODS/Java Img (client-side)

> Steps

- Execute `istream` command
- Redirect the process output and store original img as byte[]
- Compute thumbnail with default method

- [OK] No additional disk write of input file from iRODS
- [OK] No additional disk write of thumbnail output
- [KO] Impact on memory for big file
    - Fix: limit file size from OpenSILEX
    - Fix: the output is much smaller than the input
- [KO] The convert algorithm is way faster for jpg and other
- [KO] Still has to fetch source file from remote fs (Network/Disk)