package br.com.oficina.execution.interfaces.controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ExecutionExecucoesResourceTest {
    @Inject
    DynamoDbExecutionStore store;

    @Test
    void deveExecutarFluxoFelizDeDiagnosticoEReparoConformeContrato() {
        var ordemServicoId = UUID.randomUUID();
        var execucaoId = given()
                .header("X-Idempotency-Key", "execucao-create-001")
                .contentType("application/json")
                .body("""
                        {
                          "ordemServicoId": "%s"
                        }
                        """.formatted(ordemServicoId))
                .when()
                .post("/api/v1/execucoes")
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/v1/execucoes/"))
                .body("execucaoId", notNullValue())
                .body("ordemServicoId", equalTo(ordemServicoId.toString()))
                .body("status", equalTo("CRIADA"))
                .body("criadoEm", notNullValue())
                .body("atualizadoEm", notNullValue())
                .extract()
                .path("execucaoId")
                .toString();

        given()
                .header("X-Idempotency-Key", "execucao-diagnostico-inicio-001")
                .header("X-Correlation-Id", "corr-execucao-001")
                .when()
                .post("/api/v1/execucoes/{execucaoId}/diagnostico/inicio", execucaoId)
                .then()
                .statusCode(200)
                .body("execucaoId", equalTo(execucaoId))
                .body("status", equalTo("EM_DIAGNOSTICO"));

        given()
                .header("X-Idempotency-Key", "execucao-diagnostico-conclusao-001")
                .header("X-Correlation-Id", "corr-execucao-001")
                .contentType("application/json")
                .body("""
                        {
                          "diagnostico": "Bateria sem carga util"
                        }
                        """)
                .when()
                .post("/api/v1/execucoes/{execucaoId}/diagnostico/conclusao", execucaoId)
                .then()
                .statusCode(200)
                .body("status", equalTo("DIAGNOSTICO_CONCLUIDO"))
                .body("diagnostico", equalTo("Bateria sem carga util"));

        given()
                .header("X-Idempotency-Key", "execucao-reparo-inicio-001")
                .header("X-Correlation-Id", "corr-execucao-001")
                .when()
                .post("/api/v1/execucoes/{execucaoId}/reparo/inicio", execucaoId)
                .then()
                .statusCode(200)
                .body("status", equalTo("EM_REPARO"));

        given()
                .header("X-Idempotency-Key", "execucao-reparo-conclusao-001")
                .header("X-Correlation-Id", "corr-execucao-001")
                .contentType("application/json")
                .body("""
                        {
                          "observacoes": "Bateria substituida e teste eletrico realizado"
                        }
                        """)
                .when()
                .post("/api/v1/execucoes/{execucaoId}/reparo/conclusao", execucaoId)
                .then()
                .statusCode(200)
                .body("status", equalTo("REPARO_CONCLUIDO"))
                .body("observacoesReparo", equalTo("Bateria substituida e teste eletrico realizado"));

        given()
                .when()
                .get("/api/v1/ordens-servico/{ordemServicoId}/execucao", ordemServicoId)
                .then()
                .statusCode(200)
                .body("execucaoId", equalTo(execucaoId))
                .body("status", equalTo("REPARO_CONCLUIDO"));

        assertTrue(store.outboxEvents().stream().anyMatch(event ->
                event.eventType().equals("diagnosticoIniciado")
                        && event.topic().equals("oficina.execution.diagnostico-iniciado")
                        && event.payload().get("statusExecucao").toString().equals("EM_DIAGNOSTICO")));
        assertTrue(store.outboxEvents().stream().anyMatch(event ->
                event.eventType().equals("execucaoFinalizada")
                        && event.topic().equals("oficina.execution.execucao-finalizada")
                        && event.payload().get("statusExecucao").toString().equals("REPARO_CONCLUIDO")));
    }

    @Test
    void deveRejeitarTransicaoInvalidaComErroContratado() {
        var ordemServicoId = UUID.randomUUID();
        var execucaoId = given()
                .header("X-Idempotency-Key", "execucao-invalid-create-001")
                .contentType("application/json")
                .body("""
                        {
                          "ordemServicoId": "%s"
                        }
                        """.formatted(ordemServicoId))
                .when()
                .post("/api/v1/execucoes")
                .then()
                .statusCode(201)
                .extract()
                .path("execucaoId")
                .toString();

        given()
                .header("X-Idempotency-Key", "execucao-invalid-reparo-001")
                .when()
                .post("/api/v1/execucoes/{execucaoId}/reparo/inicio", execucaoId)
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("code", equalTo("HTTP_409"))
                .body("message", containsString("Reparo so pode iniciar"))
                .body("path", equalTo("/api/v1/execucoes/" + execucaoId + "/reparo/inicio"))
                .body("correlationId", notNullValue())
                .body("service", equalTo("oficina-execution-service"));
    }
}
