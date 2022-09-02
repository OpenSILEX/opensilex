package org.opensilex.nosql.datasource.coordinator;

import org.opensilex.nosql.datasource.operation.AbstractDataSourceOperation;
import org.opensilex.utils.ThrowingConsumer;

/**
 * <pre>
 * Specific kind of {@link org.opensilex.nosql.datasource.operation.DataSourceOperation} which is used
 * when in some pipeline, an operation is also composed of multiple-datasource.
 *
 * Ex: considering [A,B,C] some operation, and considering that B is composed of two sub-operation [B1,B2],
 * then the use of Compound operation allow to represent the following desired execution flow : [A,B1,B2,C]
 * or [A,C,B1,B2].
 *
 * In this case, then the classes which need to perform [B1,B2] work
 * directly with the coordinator by registering operation.
 * </pre>
 *
 * @author rcolin
 */
public class CompoundOperation extends AbstractDataSourceOperation<DistributedDataSourceCoordinator> {

    private final boolean nestedOperationAreEvaluatedNow;

    /**
     *
     * @param coordinator the coordinator which execute this operation
     * @param consumer define which sub-operation performs with the coordinator
     * @param nestedOperationAreEvaluatedNow indicate if the sub-operation registered during this operation must be evaluated just after
     * this one or not
     */
    CompoundOperation(DistributedDataSourceCoordinator coordinator, ThrowingConsumer<DistributedDataSourceCoordinator, Exception> consumer, boolean nestedOperationAreEvaluatedNow) {
        super(coordinator, consumer);
        this.nestedOperationAreEvaluatedNow = nestedOperationAreEvaluatedNow;
    }

    /**
     *
     * @return true if the sub-operation registered during this operation must be evaluated just after
     * this one or not
     *
     */
    public boolean nestedOperationAreEvaluatedNow() {
        return nestedOperationAreEvaluatedNow;
    }
}
