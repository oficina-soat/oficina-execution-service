package br.com.oficina.execution.framework.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RuntimeDependenciesValidatorTest {
    @Test
    void deveManterDevLocalForaDoRuntimeProtegido() {
        var probes = new AtomicInteger();
        var values = Map.of(RuntimeDependenciesValidator.DEPLOYMENT_ENVIRONMENT, "local");

        assertDoesNotThrow(() -> RuntimeDependenciesValidator.validar(
                List.of("dev"), values, probes::incrementAndGet, probes::incrementAndGet));

        assertEquals(0, probes.get());
        assertFalse(RuntimeDependenciesValidator.runtimeProtegido(List.of("test"), values));
    }

    @Test
    void deveProtegerProfilesProdELabEAmbienteLab() {
        var values = Map.of(RuntimeDependenciesValidator.DEPLOYMENT_ENVIRONMENT, "local");
        var labValues = Map.of(RuntimeDependenciesValidator.DEPLOYMENT_ENVIRONMENT, "lab");

        assertTrue(RuntimeDependenciesValidator.runtimeProtegido(List.of("prod"), values));
        assertTrue(RuntimeDependenciesValidator.runtimeProtegido(List.of("dev", "LAB"), values));
        assertTrue(RuntimeDependenciesValidator.runtimeProtegido(List.of("custom"), labValues));
    }

    @Test
    void deveExecutarTodosOsProbesNoRuntimeProtegido() {
        var probes = new AtomicInteger();

        RuntimeDependenciesValidator.validar(
                List.of("prod"),
                validValues(),
                probes::incrementAndGet,
                probes::incrementAndGet);

        assertEquals(2, probes.get());
    }

    @Test
    void deveRejeitarConfiguracaoIncompletaOuLocalNoRuntimeProtegido() {
        var values = new HashMap<>(validValues());
        values.put(RuntimeDependenciesValidator.DYNAMODB_REGION, "");
        values.put(RuntimeDependenciesValidator.DYNAMODB_TABLE_PREFIX, "PREFIX_PLACEHOLDER");
        values.put(RuntimeDependenciesValidator.DYNAMODB_CREDENTIALS_TYPE, "static");
        values.put(RuntimeDependenciesValidator.JWT_ISSUER, "");
        values.put(RuntimeDependenciesValidator.JWT_AUDIENCE, "outro-servico");
        values.put(RuntimeDependenciesValidator.JWT_PUBLIC_KEY_LOCATION, "JWKS_PLACEHOLDER");
        values.put(RuntimeDependenciesValidator.MESSAGING_ENABLED, "false");
        values.put(RuntimeDependenciesValidator.MESSAGING_PUBLISHER_ENABLED, "false");
        values.put(RuntimeDependenciesValidator.MESSAGING_CONSUMER_ENABLED, "false");
        values.put(RuntimeDependenciesValidator.MESSAGING_WORKER_ENABLED, "false");
        values.put(RuntimeDependenciesValidator.DYNAMODB_ENDPOINT_OVERRIDE, "http://localhost:8000");
        values.put(RuntimeDependenciesValidator.DYNAMODB_ENDPOINT_OVERRIDE_ENV, "http://localhost:8000");
        values.put(RuntimeDependenciesValidator.MESSAGING_ENDPOINT_OVERRIDE, "http://localhost:4566");
        values.put(RuntimeDependenciesValidator.AWS_ACCESS_KEY_ID, "access");

        var profiles = List.of("prod");
        Runnable noOp = () -> { };
        var exception = assertThrows(
                IllegalStateException.class,
                () -> RuntimeDependenciesValidator.validar(profiles, values, noOp, noOp));

        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.DYNAMODB_REGION));
        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.DYNAMODB_TABLE_PREFIX));
        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.DYNAMODB_CREDENTIALS_TYPE));
        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.JWT_ISSUER));
        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.JWT_AUDIENCE));
        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.JWT_PUBLIC_KEY_LOCATION));
        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.MESSAGING_ENABLED));
        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.MESSAGING_PUBLISHER_ENABLED));
        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.MESSAGING_CONSUMER_ENABLED));
        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.MESSAGING_WORKER_ENABLED));
        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.DYNAMODB_ENDPOINT_OVERRIDE));
        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.DYNAMODB_ENDPOINT_OVERRIDE_ENV));
        assertTrue(exception.getMessage().contains(RuntimeDependenciesValidator.MESSAGING_ENDPOINT_OVERRIDE));
        assertTrue(exception.getMessage().contains("credenciais AWS estaticas"));
    }

    @Test
    void deveAceitarCadeiaIamCredencialBasicaOuCredencialTemporariaCompleta() {
        var iamValues = validValues();
        var basicValues = new HashMap<>(validValues());
        basicValues.put(RuntimeDependenciesValidator.AWS_ACCESS_KEY_ID, "access");
        basicValues.put(RuntimeDependenciesValidator.AWS_SECRET_ACCESS_KEY, "secret");
        var sessionValues = new HashMap<>(basicValues);
        sessionValues.put(RuntimeDependenciesValidator.AWS_SESSION_TOKEN, "token");

        assertDoesNotThrow(() -> RuntimeDependenciesValidator.validar(
                List.of("prod"), iamValues, () -> { }, () -> { }));
        assertDoesNotThrow(() -> RuntimeDependenciesValidator.validar(
                List.of("prod"), basicValues, () -> { }, () -> { }));
        assertDoesNotThrow(() -> RuntimeDependenciesValidator.validar(
                List.of("prod"), sessionValues, () -> { }, () -> { }));
    }

    @Test
    void deveRejeitarTokenTemporarioSemParDeChaves() {
        var values = new HashMap<>(validValues());
        values.put(RuntimeDependenciesValidator.AWS_SESSION_TOKEN, "token");

        var profiles = List.of("lab");
        Runnable noOp = () -> { };
        var exception = assertThrows(
                IllegalStateException.class,
                () -> RuntimeDependenciesValidator.validar(profiles, values, noOp, noOp));

        assertTrue(exception.getMessage().contains("credenciais AWS estaticas"));
    }

    @Test
    void devePropagarFalhaDoDynamoDbComContexto() {
        var profiles = List.of("prod");
        var values = validValues();
        Runnable dynamoDbFailure = () -> { throw new IllegalArgumentException("dynamodb indisponivel"); };
        Runnable noOp = () -> { };
        var exception = assertThrows(
                IllegalStateException.class,
                () -> RuntimeDependenciesValidator.validar(profiles, values, dynamoDbFailure, noOp));

        assertTrue(exception.getMessage().contains("DynamoDB"));
        assertEquals("dynamodb indisponivel", exception.getCause().getMessage());
    }

    @Test
    void devePropagarFalhaDaMensageriaComContexto() {
        var profiles = List.of("prod");
        var values = validValues();
        Runnable noOp = () -> { };
        Runnable messagingFailure = () -> { throw new IllegalArgumentException("mensageria indisponivel"); };
        var exception = assertThrows(
                IllegalStateException.class,
                () -> RuntimeDependenciesValidator.validar(profiles, values, noOp, messagingFailure));

        assertTrue(exception.getMessage().contains("SNS/SQS/IAM"));
        assertEquals("mensageria indisponivel", exception.getCause().getMessage());
    }

    private static Map<String, String> validValues() {
        var values = new HashMap<String, String>();
        values.put(RuntimeDependenciesValidator.DEPLOYMENT_ENVIRONMENT, "prod");
        values.put(RuntimeDependenciesValidator.DYNAMODB_REGION, "us-east-1");
        values.put(RuntimeDependenciesValidator.DYNAMODB_CREDENTIALS_TYPE, "default");
        values.put(RuntimeDependenciesValidator.DYNAMODB_TABLE_PREFIX, "oficina-execution-prod");
        values.put(RuntimeDependenciesValidator.DYNAMODB_ENDPOINT_OVERRIDE, "");
        values.put(RuntimeDependenciesValidator.DYNAMODB_ENDPOINT_OVERRIDE_ENV, "");
        values.put(RuntimeDependenciesValidator.MESSAGING_ENABLED, "true");
        values.put(RuntimeDependenciesValidator.MESSAGING_PUBLISHER_ENABLED, "true");
        values.put(RuntimeDependenciesValidator.MESSAGING_CONSUMER_ENABLED, "true");
        values.put(RuntimeDependenciesValidator.MESSAGING_WORKER_ENABLED, "true");
        values.put(RuntimeDependenciesValidator.MESSAGING_ENDPOINT_OVERRIDE, "");
        values.put(RuntimeDependenciesValidator.AWS_ACCESS_KEY_ID, "");
        values.put(RuntimeDependenciesValidator.AWS_SECRET_ACCESS_KEY, "");
        values.put(RuntimeDependenciesValidator.AWS_SESSION_TOKEN, "");
        values.put(RuntimeDependenciesValidator.JWT_ISSUER, "https://issuer.example.com");
        values.put(RuntimeDependenciesValidator.JWT_AUDIENCE, "oficina-execution-service");
        values.put(RuntimeDependenciesValidator.JWT_PUBLIC_KEY_LOCATION, "https://issuer.example.com/jwks.json");
        return Map.copyOf(values);
    }
}
