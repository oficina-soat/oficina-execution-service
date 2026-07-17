package br.com.oficina.execution.interfaces.controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore;
import br.com.oficina.execution.framework.dynamodb.DynamoDbLocalTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(DynamoDbLocalTestResource.class)
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
                .body("acoesPermitidas", equalTo(List.of("INICIAR_DIAGNOSTICO")))
                .body("prioridade", equalTo(100))
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
                .body("status", equalTo("EM_DIAGNOSTICO"))
                .body("acoesPermitidas", equalTo(List.of("CONCLUIR_DIAGNOSTICO")));

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
                .body("acoesPermitidas", equalTo(List.of()))
                .body("diagnostico", equalTo("Bateria sem carga util"));

        store.iniciarReparo(UUID.fromString(execucaoId), "corr-aprovacao-001");

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
                .body("acoesPermitidas", equalTo(List.of()))
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
                        && "EM_DIAGNOSTICO".equals(String.valueOf(event.payload().get("statusExecucao")))));
        assertTrue(store.outboxEvents().stream().anyMatch(event ->
                event.eventType().equals("execucaoFinalizada")
                        && event.topic().equals("oficina.execution.execucao-finalizada")
                        && "REPARO_CONCLUIDO".equals(String.valueOf(event.payload().get("statusExecucao")))));
    }

    @Test
    void naoDeveExporInicioDoReparoComoAcaoPublica() {
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
                .statusCode(404);
    }

    @Test
    void deveConsultarFilaOrdenadaPorPrioridadeERemoverExecucaoEmAndamento() {
        var ordemServicoNormal = UUID.randomUUID();
        var execucaoNormal = criarExecucaoParaFila(ordemServicoNormal, 2, "execucao-fila-normal-001");
        var ordemServicoUrgente = UUID.randomUUID();
        var execucaoUrgente = criarExecucaoParaFila(ordemServicoUrgente, 1, "execucao-fila-urgente-001");

        var fila = consultarFilaCriada();
        var indiceUrgente = indiceDaExecucao(fila, execucaoUrgente);
        var indiceNormal = indiceDaExecucao(fila, execucaoNormal);

        assertTrue(indiceUrgente >= 0);
        assertTrue(indiceNormal >= 0);
        assertTrue(indiceUrgente < indiceNormal);
        assertEquals(1, fila.get(indiceUrgente).get("prioridade"));
        assertEquals(indiceUrgente + 1, fila.get(indiceUrgente).get("posicao"));
        assertEquals(List.of("INICIAR_DIAGNOSTICO"), fila.get(indiceUrgente).get("acoesPermitidas"));

        given()
                .header("X-Idempotency-Key", "execucao-fila-diagnostico-001")
                .header("X-Correlation-Id", "corr-fila-001")
                .when()
                .post("/api/v1/execucoes/{execucaoId}/diagnostico/inicio", execucaoUrgente)
                .then()
                .statusCode(200)
                .body("status", equalTo("EM_DIAGNOSTICO"));

        var filaAposInicio = consultarFilaCriada();
        assertFalse(filaAposInicio.stream().anyMatch(item -> item.get("execucaoId").equals(execucaoUrgente)));
        assertTrue(filaAposInicio.stream().anyMatch(item -> item.get("execucaoId").equals(execucaoNormal)));
    }

    private String criarExecucaoParaFila(UUID ordemServicoId, int prioridade, String idempotencyKey) {
        return given()
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body("""
                        {
                          "ordemServicoId": "%s",
                          "prioridade": %d
                        }
                        """.formatted(ordemServicoId, prioridade))
                .when()
                .post("/api/v1/execucoes")
                .then()
                .statusCode(201)
                .body("prioridade", equalTo(prioridade))
                .extract()
                .path("execucaoId")
                .toString();
    }

    private List<Map<String, Object>> consultarFilaCriada() {
        return given()
                .queryParam("status", "CRIADA")
                .when()
                .get("/api/v1/execucoes/fila")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$");
    }

    private int indiceDaExecucao(List<Map<String, Object>> fila, String execucaoId) {
        for (var i = 0; i < fila.size(); i++) {
            if (fila.get(i).get("execucaoId").equals(execucaoId)) {
                return i;
            }
        }
        return -1;
    }
}
