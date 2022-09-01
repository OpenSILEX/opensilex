package org.opensilex.nosql.datasource.operation;

import org.opensilex.nosql.datasource.coordinator.DistributedDataSourceCoordinator;
import org.opensilex.utils.ThrowingConsumer;

public class CompoundOperation extends AbstractDataSourceOperation<DistributedDataSourceCoordinator> {

    /**
     *
     */
    private final boolean nestedOperationAreEvaluatedNow;

    public CompoundOperation(DistributedDataSourceCoordinator dataSource, ThrowingConsumer<DistributedDataSourceCoordinator, Exception> consumer, boolean nestedOperationAreEvaluatedNow) {
        super(dataSource, consumer);
        this.nestedOperationAreEvaluatedNow = nestedOperationAreEvaluatedNow;
    }

    public boolean nestedOperationAreEvaluatedNow() {
        return nestedOperationAreEvaluatedNow;
    }
}
