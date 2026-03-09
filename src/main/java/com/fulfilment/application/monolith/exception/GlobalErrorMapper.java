package com.fulfilment.application.monolith.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.PropertyValueException;
import org.jboss.logging.Logger;

@Provider
public class GlobalErrorMapper implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = Logger.getLogger(GlobalErrorMapper.class.getName());

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Response toResponse(Exception exception) {
        LOGGER.error("Failed to handle request", exception);
        ObjectNode exceptionJson = objectMapper.createObjectNode();
        int code = 500;
        if (exception instanceof WebApplicationException) {
            code = ((WebApplicationException) exception).getResponse().getStatus();
        }else if (exception instanceof PropertyValueException propertyEx) {
            code = 400;
            exceptionJson.put("exceptionType", exception.getClass().getName());
            exceptionJson.put("code", code);
            exceptionJson.put("error", "Required field is missing: " + propertyEx.getPropertyName());
            return Response.status(code).entity(exceptionJson).build();
        }
        exceptionJson.put("exceptionType", exception.getClass().getName());
        exceptionJson.put("code", code);

        if (exception.getMessage() != null) {
            exceptionJson.put("error", exception.getMessage());
        }

        return Response.status(code).entity(exceptionJson).build();
    }
}
