//******************************************************************************
//                                 FaidareCall.java
// SILEX-PHIS
// Copyright © INRA 2019
// Contact: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.faidare.api;


import org.opensilex.faidare.model.Faidarev1CallDTO;
import org.reflections.Reflections;

import javax.ws.rs.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.SetUtils.union;

/**
 * Abstract class containing most of the logic for calls description
 * 
 * @see FaidareVersion
 * @see CallsAPI
 * @author Gabriel Besombes
 */
abstract class FaidareCall {

    public static final Reflections REFLECTIONS = new Reflections("org.opensilex.faidare");

    public static final List<Faidarev1CallDTO> faidareCallsInfo = new ArrayList<>();

    public static List<Faidarev1CallDTO> getfaidareCallsInfo(){
        if(faidareCallsInfo.isEmpty()) {
            Set<Class<? extends FaidareCall>> faidareClasses = REFLECTIONS.getSubTypesOf(FaidareCall.class);

            for (Class<?> faidareClass: faidareClasses) {
                List<Method> calls = Arrays.stream(faidareClass.getDeclaredMethods())
                        .filter(m -> m.isAnnotationPresent(FaidareVersion.class))
                        .collect(Collectors.toList());
                HashMap<String, Faidarev1CallDTO> results = new HashMap<>();

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

                    Faidarev1CallDTO currentCall = new Faidarev1CallDTO(
                            callName,
                            new HashSet<>(
                                    Arrays.asList(call.getAnnotation(Produces.class).value())
                            ),
                            callMethods,
                            Collections.singleton(call.getAnnotation(FaidareVersion.class).value())
                    );

                    results.merge(
                            callName,
                            currentCall,
                            (Faidarev1CallDTO existingCall, Faidarev1CallDTO newCall) -> new Faidarev1CallDTO(
                                    callName,
                                    union(existingCall.getDataTypes(), newCall.getDataTypes()),
                                    union(existingCall.getMethods(), newCall.getMethods()),
                                    union(existingCall.getVersions(), newCall.getVersions())
                            )
                    );
                }

                faidareCallsInfo.addAll(results.values());
            }
        }
        return faidareCallsInfo;
    }
}
