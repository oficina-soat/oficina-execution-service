package br.com.oficina.execution.interfaces.controllers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore;
import br.com.oficina.execution.framework.dynamodb.DynamoDbLocalTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(DynamoDbLocalTestResource.class)
class ExecutionCatalogoEstoqueResourceTest {
    @Test
    void deveConsultarSeedsDeCatalogoESaldo() {
        given()
                .when()
                .get("/api/v1/servicos/{servicoId}", DynamoDbExecutionStore.SEED_SERVICO_ID)
                .then()
                .statusCode(200)
                .body("servicoId", equalTo(DynamoDbExecutionStore.SEED_SERVICO_ID.toString()))
                .body("nome", equalTo("Troca de oleo"))
                .body("descricao", equalTo("Servico reaproveitado do import.sql do oficina-app"))
                .body("valorBase", equalTo(150.00f))
                .body("ativo", equalTo(true))
                .body("criadoEm", notNullValue())
                .body("atualizadoEm", notNullValue());

        given()
                .when()
                .get("/api/v1/pecas/{pecaId}", DynamoDbExecutionStore.SEED_PECA_ID)
                .then()
                .statusCode(200)
                .body("pecaId", equalTo(DynamoDbExecutionStore.SEED_PECA_ID.toString()))
                .body("nome", equalTo("Volante"))
                .body("codigo", equalTo("VOL-001"))
                .body("valorUnitario", equalTo(50.00f))
                .body("ativo", equalTo(true));

        given()
                .when()
                .get("/api/v1/estoques/pecas/{pecaId}/saldo", DynamoDbExecutionStore.SEED_PECA_ID)
                .then()
                .statusCode(200)
                .body("pecaId", equalTo(DynamoDbExecutionStore.SEED_PECA_ID.toString()))
                .body("quantidadeDisponivel", equalTo(50))
                .body("quantidadeReservada", equalTo(0))
                .body("atualizadoEm", notNullValue())
                .body("acoesPermitidas[0]", equalTo("REGISTRAR_ENTRADA"));

        given()
                .queryParam("nome", "vol")
                .queryParam("codigo", "001")
                .queryParam("page", 0)
                .queryParam("size", 1)
                .when()
                .get("/api/v1/pecas")
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(1))
                .body("items[0].pecaId", equalTo(DynamoDbExecutionStore.SEED_PECA_ID.toString()))
                .body("size", equalTo(1))
                .body("totalElements", equalTo(1));
    }

    @Test
    void deveCriarAtualizarEListarServico() {
        var servicoId = given()
                .header("X-Idempotency-Key", "servico-create-001")
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Troca de bateria",
                          "descricao": "Substituicao da bateria",
                          "valorBase": 80.0
                        }
                        """)
                .when()
                .post("/api/v1/servicos")
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/v1/servicos/"))
                .body("servicoId", notNullValue())
                .body("nome", equalTo("Troca de bateria"))
                .body("descricao", equalTo("Substituicao da bateria"))
                .body("valorBase", equalTo(80.0f))
                .body("ativo", equalTo(true))
                .extract()
                .path("servicoId")
                .toString();

        given()
                .header("X-Idempotency-Key", "servico-update-001")
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Troca de bateria revisada",
                          "descricao": "Substituicao completa",
                          "valorBase": 90.0
                        }
                        """)
                .when()
                .put("/api/v1/servicos/{servicoId}", servicoId)
                .then()
                .statusCode(200)
                .body("servicoId", equalTo(servicoId))
                .body("nome", equalTo("Troca de bateria revisada"))
                .body("valorBase", equalTo(90.0f));

        given()
                .when()
                .get("/api/v1/servicos")
                .then()
                .statusCode(200)
                .body("items.size()", greaterThanOrEqualTo(2))
                .body("page", equalTo(0));
    }

    @Test
    void deveRepetirRespostaIdempotenteERejeitarPayloadDivergente() {
        var idempotencyKey = "servico-replay-" + UUID.randomUUID();
        var requestBody = """
                {
                  "nome": "Higienizacao idempotente",
                  "descricao": "Higienizacao interna",
                  "valorBase": 180.0
                }
                """;

        var servicoId = given()
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/v1/servicos")
                .then()
                .statusCode(201)
                .body("servicoId", notNullValue())
                .extract()
                .path("servicoId")
                .toString();

        given()
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body(requestBody)
                .when()
                .post("/api/v1/servicos")
                .then()
                .statusCode(201)
                .body("servicoId", equalTo(servicoId))
                .body("nome", equalTo("Higienizacao idempotente"));

        given()
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Higienizacao idempotente divergente",
                          "descricao": "Higienizacao interna",
                          "valorBase": 180.0
                        }
                        """)
                .when()
                .post("/api/v1/servicos")
                .then()
                .statusCode(409)
                .body("code", equalTo("IDEMPOTENCY_CONFLICT"))
                .body("message", equalTo("Chave de idempotencia reutilizada com payload divergente."));
    }

    @Test
    void deveCriarAtualizarPecaERegistrarMovimentosDeEstoque() {
        var pecaId = given()
                .header("X-Idempotency-Key", "peca-create-001")
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Bateria 60Ah",
                          "codigo": "bat-60ah",
                          "valorUnitario": 320.0
                        }
                        """)
                .when()
                .post("/api/v1/pecas")
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/v1/pecas/"))
                .body("pecaId", notNullValue())
                .body("nome", equalTo("Bateria 60Ah"))
                .body("codigo", equalTo("BAT-60AH"))
                .body("valorUnitario", equalTo(320.0f))
                .extract()
                .path("pecaId")
                .toString();

        given()
                .header("X-Idempotency-Key", "peca-update-001")
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Bateria 60Ah Moura",
                          "codigo": "bat-60ah-moura",
                          "valorUnitario": 350.0
                        }
                        """)
                .when()
                .put("/api/v1/pecas/{pecaId}", pecaId)
                .then()
                .statusCode(200)
                .body("pecaId", equalTo(pecaId))
                .body("nome", equalTo("Bateria 60Ah Moura"))
                .body("codigo", equalTo("BAT-60AH-MOURA"));

        given()
                .header("X-Idempotency-Key", "estoque-entrada-001")
                .contentType("application/json")
                .body("""
                        {
                          "pecaId": "%s",
                          "quantidade": 5,
                          "motivo": "Compra inicial"
                        }
                        """.formatted(pecaId))
                .when()
                .post("/api/v1/estoques/movimentos/entrada")
                .then()
                .statusCode(201)
                .body("movimentoId", notNullValue())
                .body("pecaId", equalTo(pecaId))
                .body("tipo", equalTo("ENTRADA"))
                .body("quantidade", equalTo(5))
                .body("motivo", equalTo("Compra inicial"));

        given()
                .header("X-Idempotency-Key", "estoque-reserva-001")
                .contentType("application/json")
                .body("""
                        {
                          "pecaId": "%s",
                          "ordemServicoId": "%s",
                          "quantidade": 2,
                          "motivo": "Reserva para OS"
                        }
                        """.formatted(pecaId, DynamoDbExecutionStore.SEED_ORDEM_SERVICO_ID))
                .when()
                .post("/api/v1/estoques/movimentos/reserva")
                .then()
                .statusCode(201)
                .body("pecaId", equalTo(pecaId))
                .body("ordemServicoId", equalTo(DynamoDbExecutionStore.SEED_ORDEM_SERVICO_ID.toString()))
                .body("tipo", equalTo("RESERVA"))
                .body("quantidade", equalTo(2));

        given()
                .header("X-Idempotency-Key", "estoque-consumo-001")
                .contentType("application/json")
                .body("""
                        {
                          "pecaId": "%s",
                          "ordemServicoId": "%s",
                          "quantidade": 1,
                          "motivo": "Consumo no reparo"
                        }
                        """.formatted(pecaId, DynamoDbExecutionStore.SEED_ORDEM_SERVICO_ID))
                .when()
                .post("/api/v1/estoques/movimentos/consumo")
                .then()
                .statusCode(201)
                .body("tipo", equalTo("CONSUMO"))
                .body("quantidade", equalTo(1));

        given()
                .header("X-Idempotency-Key", "estoque-estorno-001")
                .contentType("application/json")
                .body("""
                        {
                          "pecaId": "%s",
                          "ordemServicoId": "%s",
                          "quantidade": 1,
                          "motivo": "Estorno de reserva"
                        }
                        """.formatted(pecaId, DynamoDbExecutionStore.SEED_ORDEM_SERVICO_ID))
                .when()
                .post("/api/v1/estoques/movimentos/estorno")
                .then()
                .statusCode(201)
                .body("tipo", equalTo("ESTORNO"))
                .body("quantidade", equalTo(1));

        given()
                .when()
                .get("/api/v1/estoques/pecas/{pecaId}/saldo", pecaId)
                .then()
                .statusCode(200)
                .body("quantidadeDisponivel", equalTo(4))
                .body("quantidadeReservada", equalTo(0));

        given()
                .queryParam("pecaId", pecaId)
                .when()
                .get("/api/v1/estoques/movimentos")
                .then()
                .statusCode(200)
                .body("items.size()", equalTo(4))
                .body("items[0].tipo", equalTo("ESTORNO"))
                .body("items[1].tipo", equalTo("CONSUMO"))
                .body("items[2].tipo", equalTo("RESERVA"))
                .body("items[3].tipo", equalTo("ENTRADA"));

        given()
                .queryParam("ordemServicoId", DynamoDbExecutionStore.SEED_ORDEM_SERVICO_ID)
                .when()
                .get("/api/v1/estoques/movimentos")
                .then()
                .statusCode(200)
                .body("items.size()", greaterThanOrEqualTo(3));
    }

    @Test
    void deveRejeitarCatalogoInvalidoComErroContratado() {
        given()
                .header("X-Idempotency-Key", "servico-invalid-001")
                .contentType("application/json")
                .body("""
                        {
                          "nome": " ",
                          "valorBase": -1
                        }
                        """)
                .when()
                .post("/api/v1/servicos")
                .then()
                .statusCode(400)
                .body("timestamp", notNullValue())
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("message", equalTo("Nome do servico e obrigatorio."))
                .body("path", equalTo("/api/v1/servicos"))
                .body("correlationId", notNullValue())
                .body("service", equalTo("oficina-execution-service"))
                .body("details.size()", equalTo(0));
    }

    @Test
    void deveRetornarNotFoundEConflictContratados() {
        given()
                .when()
                .get("/api/v1/pecas/{pecaId}", UUID.fromString("aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa"))
                .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("code", equalTo("RESOURCE_NOT_FOUND"))
                .body("message", containsString("Peca nao encontrada"))
                .body("path", equalTo("/api/v1/pecas/aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa"));

        given()
                .header("X-Idempotency-Key", "peca-duplicada-001")
                .contentType("application/json")
                .body("""
                        {
                          "nome": "Volante duplicado",
                          "codigo": "VOL-001",
                          "valorUnitario": 55.0
                        }
                        """)
                .when()
                .post("/api/v1/pecas")
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("code", equalTo("HTTP_409"))
                .body("message", containsString("Codigo de peca ja cadastrado"));

        given()
                .header("X-Idempotency-Key", "estoque-conflict-001")
                .contentType("application/json")
                .body("""
                        {
                          "pecaId": "%s",
                          "ordemServicoId": "%s",
                          "quantidade": 999,
                          "motivo": "Reserva maior que saldo"
                        }
                        """.formatted(DynamoDbExecutionStore.SEED_PECA_ID, DynamoDbExecutionStore.SEED_ORDEM_SERVICO_ID))
                .when()
                .post("/api/v1/estoques/movimentos/reserva")
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("code", equalTo("HTTP_409"))
                .body("message", containsString("Saldo de estoque insuficiente"))
                .body("path", equalTo("/api/v1/estoques/movimentos/reserva"));
    }
}
