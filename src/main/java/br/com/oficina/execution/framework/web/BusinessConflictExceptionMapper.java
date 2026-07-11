package br.com.oficina.execution.framework.web;

import br.com.oficina.execution.core.exceptions.BusinessConflictException;
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
public class BusinessConflictExceptionMapper implements ExceptionMapper<BusinessConflictException> {
    @Context
    UriInfo uriInfo;

    @ConfigProperty(name = "quarkus.application.name")
    String serviceName;

    @Override
    public Response toResponse(BusinessConflictException exception) {
        String correlationId = correlationId();
        var timestamp = OffsetDateTime.now(ZoneOffset.UTC);
        var body = new ErrorResponse(
                timestamp,
                Response.Status.CONFLICT.getStatusCode(),
                Response.Status.CONFLICT.getReasonPhrase(),
                "HTTP_409",
                exception.getMessage(),
                path(),
                correlationId,
                null,
                null,
                null,
                serviceName,
                logReference(timestamp, correlationId),
                List.of());

        return Response.status(Response.Status.CONFLICT)
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
