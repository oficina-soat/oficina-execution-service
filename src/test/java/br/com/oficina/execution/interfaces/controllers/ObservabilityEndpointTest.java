package br.com.oficina.execution.interfaces.controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import br.com.oficina.execution.framework.dynamodb.DynamoDbLocalTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(DynamoDbLocalTestResource.class)
class ObservabilityEndpointTest {
    @Test
    void shouldExposeLiveHealthEndpoint() {
        given()
                .when()
                .get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void shouldExposeReadyHealthEndpoint() {
        given()
                .when()
                .get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void shouldExposePrometheusMetricsEndpoint() {
        given()
                .when()
                .get("/q/metrics")
                .then()
                .statusCode(200)
                .body(not(emptyOrNullString()))
                .body(containsString("# HELP"));
    }
}
