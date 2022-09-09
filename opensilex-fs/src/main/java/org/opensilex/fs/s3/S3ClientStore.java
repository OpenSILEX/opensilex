package org.opensilex.fs.s3;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 *
 */
public class S3ClientStore {
    private static S3ClientStore INSTANCE;

    private final Map<Pair<String, Region>, S3Client> s3ClientByLocation;
    private final Map<Pair<String, Region>, Boolean> clientHasBeenClosed;

    private S3ClientStore(){
        s3ClientByLocation = new HashMap<>();
        clientHasBeenClosed = new HashMap<>();
    }

    public static S3ClientStore getInstance() {
        if(INSTANCE == null){
            INSTANCE = new S3ClientStore();
        }
        return INSTANCE;
    }

    public S3Client getOrCreateClient(S3FsConfig config, Function<S3FsConfig, S3Client> clientGenerator){

        Pair<String,Region> key = new ImmutablePair<>(
                config.endpoint(),
                Region.of(config.region())
        );

        S3Client client =  s3ClientByLocation.computeIfAbsent(key, newKey -> clientGenerator.apply(config));
        clientHasBeenClosed.putIfAbsent(key,false);

        return client;
    }

    public void closeClient(S3FsConfig config){

        Pair<String,Region> key = new ImmutablePair<>(
                config.endpoint(),
                Region.of(config.region())
        );

        Boolean hasBeenClosed = clientHasBeenClosed.get(key);
        if(hasBeenClosed == null){
            return;
        }

        if(! hasBeenClosed){
            s3ClientByLocation.get(key).close();
        }
    }
}
