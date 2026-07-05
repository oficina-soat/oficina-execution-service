package br.com.oficina.execution.framework.web;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.MDC;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
    @Context
    UriInfo uriInfo;

    @ConfigProperty(name = "quarkus.application.name")
    String serviceName;

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        String correlationId = correlationId();
        var timestamp = OffsetDateTime.now(ZoneOffset.UTC);
        var body = new ErrorResponse(
                timestamp,
                Response.Status.BAD_REQUEST.getStatusCode(),
                Response.Status.BAD_REQUEST.getReasonPhrase(),
                "VALIDATION_ERROR",
                exception.getMessage(),
                path(),
                correlationId,
                null,
                null,
                null,
                serviceName,
                logReference(timestamp, correlationId),
                List.of());

        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .header(CorrelationIdFilter.HEADER_NAME, correlationId)
                .entity(body)
                .build();
    }

    private String correlationId() {
        Object correlationId = MDC.get(CorrelationIdFilter.PROPERTY_NAME);
        return correlationId == null ? UUID.randomUUID().toString() : correlationId.toString();
    }

    private String path() {
        if (uriInfo == null) {
            return null;
        }
        return "/" + uriInfo.getPath().replaceFirst("^/+", "");
    }

    private String logReference(OffsetDateTime timestamp, String correlationId) {
        return serviceName + "/" + timestamp.toLocalDate() + "/" + correlationId;
    }
}
