package org.opensilex.service.reflection;

import org.opensilex.OpenSilex;
import org.opensilex.server.rest.RestApplication;

import java.lang.annotation.*;

/**
 * <p>
 *     Annotates a service to be automatically bound to itself, i.e. by calling the binder in the following way :
 * </p>
 * <pre>
 *     binder.bindAsContract(serviceClass);
 * </pre>
 * <p>
 *     The class annotated with @SelfBoundService should also be annotated with @Service
 *     (from {@link org.jvnet.hk2.annotations.Service}. Without both annotations, the service will not be automatically
 *     self-bound.
 * </p>
 * <p>
 *     Currently, the binding logic is implemented by {@link RestApplication}. It could maybe be moved into
 *     {@link org.opensilex.OpenSilexModuleManager}, but that class lacks the needed access to OpenSilex reflections
 *     (accessible from {@link OpenSilex#getReflections()}).
 * </p>
 *
 * @author Valentin Rigolle
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface SelfBound {

}
