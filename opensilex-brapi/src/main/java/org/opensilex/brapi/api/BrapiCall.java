//******************************************************************************
//                                 BrapiCall.java 
// SILEX-PHIS
// Copyright © INRA 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.api;


import org.opensilex.brapi.model.BrAPIv1CallDTO;
import org.reflections.Reflections;

import javax.ws.rs.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Interface for dependency injection in order to get Calls attributes. * 
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
                List<String> names = new ArrayList<>();
                HashMap<String, HashMap<String, Set<String>>> results = new HashMap<>();

                for (Method call: calls) {
                    String[] path = call.getAnnotation(Path.class).value().split("/");
                    String callName = String.join("/", Arrays.copyOfRange(path, 1, path.length));

                    Set<String> callDatatypes = new HashSet<>(
                            Arrays.asList(call.getAnnotation(Produces.class).value())
                    );

                    Set<String> callVersions = new HashSet<>(
                            Collections.singletonList(call.getAnnotation(BrapiVersion.class).value())
                    );

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

                    if (names.contains(callName)) {
                        callDatatypes.addAll(results.get(callName).get("callDatatypes"));
                        callVersions.addAll(results.get(callName).get("callVersions"));
                        callMethods.addAll(results.get(callName).get("callMethods"));
                    } else {
                        names.add(callName);
                    }
                    HashMap<String, Set<String>> newValue = new HashMap<String, Set<String>>(){{
                        put("callDatatypes", callDatatypes);
                        put("callVersions", callVersions);
                        put("callMethods", callMethods);
                    }};
                    results.put(callName, newValue);
                }

                for (String name : results.keySet()) {
                    brapiCallsInfo.add(new BrAPIv1CallDTO(
                            name,
                            new ArrayList<>(results.get(name).get("callDatatypes")),
                            new ArrayList<>(results.get(name).get("callMethods")),
                            new ArrayList<>(results.get(name).get("callVersions"))
                    ));
                }
            }
        }
        return brapiCallsInfo;
    }
}
