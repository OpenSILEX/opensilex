#!/bin/bash

# /!\                               WARNING                                 /!\
# /!\                                                                       /!\
# /!\ This script will destroy every docker container and images presents   /!\
# /!\ on your system, NOT ONLY those used by opensilex !                    /!\
# /!\                                                                       /!\
# /!\                               WARNING                                 /!\

# Stop all containers
containers=`docker ps -a -q`
if [ -n "$containers" ] ; then
        docker stop $containers
fi

# Delete all containers
containers=`docker ps -a -q`
if [ -n "$containers" ]; then
        docker rm -f -v $containers
fi

# Delete all images
images=`docker images -q -a`
if [ -n "$images" ]; then
        docker rmi -f $images
fi

# Delete all volumes
volumes=`docker volume ls -q`
if [ -n "$volumes" ]; then
        docker volume rm -f $volumes
fi
