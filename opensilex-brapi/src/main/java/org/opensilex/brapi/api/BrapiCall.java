//******************************************************************************
//                                 BrapiCall.java 
// SILEX-PHIS
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;


import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.SetUtils;
import org.opensilex.brapi.model.BrAPIv1CallDTO;
import org.reflections.Reflections;

import javax.ws.rs.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.SetUtils.union;

/**
 * Abstract class containing most of the logic for calls description
 * 
 * @see BrapiVersion
 * @see CallsAPI
 * @author Gabriel Besombes
 */
abstract class BrapiCall {

    public static final Reflections REFLECTIONS = new Reflections("org.opensilex.brapi");

    public static List<BrAPIv1CallDTO> brapiCallsInfo = new ArrayList<>();

    public static List<BrAPIv1CallDTO> getBrapiCallsInfo(){
        if(brapiCallsInfo.isEmpty()) {
            Set<Class<? extends BrapiCall>> brapiClasses = REFLECTIONS.getSubTypesOf(BrapiCall.class);

            for (Class<?> brapiClass: brapiClasses) {
                List<Method> calls = Arrays.stream(brapiClass.getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(BrapiVersion.class))
                        .collect(Collectors.toList());
                HashMap<String, BrAPIv1CallDTO> results = new HashMap<>();

                for (Method call: calls) {
                    String[] path = call.getAnnotation(Path.class).value().split("/");
                    String callName = String.join("/", Arrays.copyOfRange(path, 1, path.length));

                    Set<String> callMethods = new HashSet<>();
                    if (call.isAnnotationPresent(GET.class)) {
                        callMethods.add("GET");
                    } else if (call.isAnnotationPresent(POST.class)) {
                        callMethods.add("POST");
                    } else if (call.isAnnotationPresent(PUT.class)) {
                        callMethods.add("PUT");
                    } else if (call.isAnnotationPresent(DELETE.class)) {
                        callMethods.add("DELETE");
                    }

                    BrAPIv1CallDTO currentCall = new BrAPIv1CallDTO(
                            callName,
                            new HashSet<>(
                                    Arrays.asList(call.getAnnotation(Produces.class).value())
                            ),
                            callMethods,
                            Collections.singleton(call.getAnnotation(BrapiVersion.class).value())
                    );

                    results.merge(
                            callName,
                            currentCall,
                            (BrAPIv1CallDTO existingCall, BrAPIv1CallDTO newCall) -> new BrAPIv1CallDTO(
                                    callName,
                                    union(existingCall.getDataTypes(), newCall.getDataTypes()),
                                    union(existingCall.getMethods(), newCall.getMethods()),
                                    union(existingCall.getVersions(), newCall.getVersions())
                            )
                    );
                }

                brapiCallsInfo.addAll(results.values());
            }
        }
        return brapiCallsInfo;
    }
}
