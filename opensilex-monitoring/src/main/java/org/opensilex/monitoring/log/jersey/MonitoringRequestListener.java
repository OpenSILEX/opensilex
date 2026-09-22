//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.log.jersey;

import java.time.Instant;
import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.opensilex.OpenSilex;
import org.opensilex.monitoring.MonitoringModule;
import org.opensilex.monitoring.config.RequestLogConfig;
import org.opensilex.monitoring.log.PathExclusionMatcher;
import org.opensilex.monitoring.log.QueryParameterCapture;
import org.opensilex.monitoring.log.RequestLogService;
import org.opensilex.monitoring.log.dal.RequestLogModel;
import org.opensilex.monitoring.presence.ActiveUserRegistry;
import org.opensilex.security.account.dal.AccountModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures every web service call through Jersey's own monitoring hooks.
 *
 * <p>This is an {@link ApplicationEventListener} rather than a request/response filter pair, for
 * reasons that matter to what is being measured:</p>
 * <ul>
 *   <li>{@link RequestEvent.Type#FINISHED} fires <em>even when request processing ends in an
 *       unhandled exception</em>, and {@code getException()} is still available there. A
 *       {@code ContainerResponseFilter} does not run on every error path — notably not when a
 *       pre-matching filter aborts — so a filter-based logger would systematically under-count
 *       errors, which is fatal for a feature whose headline number is a failure percentage.</li>
 *   <li>{@code onRequest} returns a fresh listener per request, so the start time is just a final
 *       field. A filter pair has to smuggle it through a request property.</li>
 *   <li>{@link ExtendedUriInfo#getMatchedResourceMethod()} names the matched resource method, which
 *       is a far better service identifier than a raw path carrying inlined path parameters.</li>
 *   <li>A listener cannot alter the response. Monitoring should not have that power.</li>
 * </ul>
 *
 * @author Arnaud Charleroy
 */
@Provider
public class MonitoringRequestListener implements ApplicationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringRequestListener.class);

    @Inject
    private OpenSilex opensilex;

    private RequestLogConfig config;
    private RequestLogService writer;
    private ActiveUserRegistry activeUsers;
    private PathExclusionMatcher exclusions;
    private QueryParameterCapture parameterCapture;
    private volatile boolean active;

    @Override
    public void onEvent(ApplicationEvent event) {
        switch (event.getType()) {
            case INITIALIZATION_APP_FINISHED -> initialise();
            case DESTROY_FINISHED -> {
                active = false;
                if (writer != null) {
                    writer.stop();
                }
            }
            default -> {
                // nothing to do
            }
        }
    }

    private void initialise() {
        try {
            MonitoringModule module = opensilex.getModuleByClass(MonitoringModule.class);
            this.config = module.getMonitoringConfig().requestLog();
            this.writer = module.getRequestLogService();
            this.activeUsers = module.getActiveUserRegistry();
            this.exclusions = new PathExclusionMatcher(config.excludedPathPrefixes());
            this.parameterCapture = new QueryParameterCapture(config);
            this.writer.start();
            this.active = true;
        } catch (Exception e) {
            LOGGER.error("monitoring: could not start the access log, it stays disabled", e);
            this.active = false;
        }
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        if (!active) {
            return null;
        }
        // Cheapest possible exclusion: no listener is allocated at all for a path we ignore.
        String path = requestEvent.getUriInfo() == null ? null : requestEvent.getUriInfo().getPath();
        if (exclusions.isExcluded(path)) {
            return null;
        }
        return new SingleRequestListener();
    }

    /**
     * One instance per request, so the start time needs no plumbing.
     */
    private final class SingleRequestListener implements RequestEventListener {

        private final Instant startTime = Instant.now();
        private final long startNanos = System.nanoTime();

        @Override
        public void onEvent(RequestEvent event) {
            if (event.getType() != RequestEvent.Type.FINISHED) {
                return;
            }
            try {
                RequestLogModel entry = capture(event);
                if (entry != null) {
                    writer.submit(entry);
                }
            } catch (Throwable t) {
                // Never let bookkeeping surface in a response.
                LOGGER.debug("monitoring: could not capture a request", t);
            }
        }

        private RequestLogModel capture(RequestEvent event) {
            ContainerRequest request = event.getContainerRequest();
            if (request == null) {
                return null;
            }

            AccountModel account = currentAccount(request);
            boolean anonymous = account == null || account.isAnonymous();
            if (anonymous && !config.logAnonymous()) {
                return null;
            }

            RequestLogModel entry = new RequestLogModel();
            entry.setStartTime(startTime);
            entry.setAnonymous(anonymous);
            if (!anonymous) {
                entry.setAccount(account.getUri());
                entry.setAccountEmail(account.getEmail() == null ? null : account.getEmail().toString());
                activeUsers.touch(account.getUri());
            }

            String method = request.getMethod();
            entry.setHttpMethod(method);
            entry.setPath(request.getUriInfo() == null ? null : request.getUriInfo().getPath());
            entry.setService(matchedService(event.getUriInfo()));

            ContainerResponse response = event.getContainerResponse();
            int status = response != null ? response.getStatus() : 500;
            Throwable exception = event.getException();
            entry.setStatus(status);
            entry.setSuccess(exception == null && status < 400);
            if (exception != null) {
                entry.setErrorType(exception.getClass().getSimpleName());
            }

            if (config.recordDuration()) {
                entry.setDurationMs((System.nanoTime() - startNanos) / 1_000_000L);
            }

            // Query parameters only for a plain GET, and the request body is never read: the
            // authentication filter already re-buffers it and doing that twice costs memory and
            // invites bugs.
            if (config.recordQueryParameters() && "GET".equals(method) && request.getUriInfo() != null) {
                entry.setQueryParameters(
                        parameterCapture.capture(request.getUriInfo().getQueryParameters()));
            }

            if (config.recordClientIp()) {
                entry.setClientIp(clientIp(request));
            }

            return entry;
        }

        private AccountModel currentAccount(ContainerRequest request) {
            if (request.getSecurityContext() == null) {
                return null;
            }
            java.security.Principal principal = request.getSecurityContext().getUserPrincipal();
            return principal instanceof AccountModel accountModel ? accountModel : null;
        }

        private String matchedService(ExtendedUriInfo uriInfo) {
            if (uriInfo == null) {
                return null;
            }
            ResourceMethod matched = uriInfo.getMatchedResourceMethod();
            if (matched == null || matched.getInvocable() == null) {
                return null;
            }
            var definition = matched.getInvocable().getDefinitionMethod();
            return definition.getDeclaringClass().getSimpleName() + "." + definition.getName();
        }

        private String clientIp(ContainerRequest request) {
            String forwarded = request.getHeaderString("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
            }
            return null;
        }
    }
}
