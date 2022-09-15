package org.opensilex.fs.s3;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * <pre>
 * Utility class used for minimizing the number of creation of {@link S3Client}.
 *
 * Since this object is thread-safe and maintains a internal pool connection to S3 server, applications
 * which use it, should reuse the same client (when connecting to the same S3 server/bucket) in order
 * to minimize extra cost due to client creation and initialization.
 *
 * So this class ensure that two call to {@link #getOrCreateClient(S3FsConfig, Function)} with two config with the same {@link S3FsConfig#region()} and {@link S3FsConfig#endpoint()}
 * will return the same instance of {@link S3Client}.
 *
 * In the same way {@link #closeClient(S3FsConfig)} can close the {@link S3Client} associated to the given {@link S3FsConfig}.
 * Note: coherence of client open/close is not the responsibility of this class. This class just offers
 * the possibility to close a {@link S3Client} which has not been already closed.
 *
 * So, callers of this class must handle the good state of the shared {@link S3Client} or handle manually the close if
 * conflicts between client usage can happened.
 * </pre>
 *
 * @author rcolin
 */
public class S3ClientStore {
    private static S3ClientStore INSTANCE;

    /**
     * Maintain an unique {@link S3Client} per (endpoint/region)
     */
    private final Map<Pair<String, Region>, S3Client> s3ClientByLocation;

    /**
     * Maintain state of closing for each (endpoint/region)
     */
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

    /**
     * @param config S3 config
     * @param clientGenerator : function which return a new {@link S3Client} according the given config
     * @return a new or already created {@link S3Client}
     */
    public S3Client getOrCreateClient(S3FsConfig config, Function<S3FsConfig, S3Client> clientGenerator){

        Pair<String,Region> key = new ImmutablePair<>(
                config.endpoint(),
                Region.of(config.region())
        );

        S3Client client = s3ClientByLocation.computeIfAbsent(key, newKey -> clientGenerator.apply(config));
        clientHasBeenClosed.putIfAbsent(key,false);

        return client;
    }

    /**
     * Close the {@link S3Client} associated to the given config if it has not been closed
     * @param config S3 config
     */
    public void closeClient(S3FsConfig config){

        Pair<String,Region> key = new ImmutablePair<>(
                config.endpoint(),
                Region.of(config.region())
        );

        // client has not been created yet
        Boolean hasBeenClosed = clientHasBeenClosed.get(key);
        if(hasBeenClosed == null){
            return;
        }

        if(! hasBeenClosed){
            s3ClientByLocation.get(key).close();
        }
    }
}
