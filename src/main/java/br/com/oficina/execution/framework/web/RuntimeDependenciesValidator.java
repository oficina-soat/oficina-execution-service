package br.com.oficina.execution.framework.web;

import br.com.oficina.execution.framework.dynamodb.DynamoDbRuntimeDependencies;
import br.com.oficina.execution.framework.messaging.MessagingRuntimeDependencies;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@Startup
@ApplicationScoped
public class RuntimeDependenciesValidator {
    static final String DEPLOYMENT_ENVIRONMENT = "oficina.observability.deployment-environment";
    static final String DYNAMODB_REGION = "quarkus.dynamodb.aws.region";
    static final String DYNAMODB_CREDENTIALS_TYPE = "quarkus.dynamodb.aws.credentials.type";
    static final String DYNAMODB_TABLE_PREFIX = "oficina.dynamodb.table-prefix";
    static final String DYNAMODB_ENDPOINT_OVERRIDE = "quarkus.dynamodb.endpoint-override";
    static final String DYNAMODB_ENDPOINT_OVERRIDE_ENV = "DYNAMODB_ENDPOINT_OVERRIDE";
    static final String MESSAGING_ENABLED = "oficina.messaging.enabled";
    static final String MESSAGING_PUBLISHER_ENABLED = "oficina.messaging.publisher.enabled";
    static final String MESSAGING_CONSUMER_ENABLED = "oficina.messaging.consumer.enabled";
    static final String MESSAGING_WORKER_ENABLED = "oficina.messaging.worker.enabled";
    static final String MESSAGING_ENDPOINT_OVERRIDE = "oficina.messaging.endpoint-override";
    static final String AWS_ACCESS_KEY_ID = "oficina.messaging.aws-access-key-id";
    static final String AWS_SECRET_ACCESS_KEY = "oficina.messaging.aws-secret-access-key";
    static final String AWS_SESSION_TOKEN = "oficina.messaging.aws-session-token";
    static final String JWT_ISSUER = "mp.jwt.verify.issuer";
    static final String JWT_AUDIENCE = "mp.jwt.verify.audiences";
    static final String JWT_PUBLIC_KEY_LOCATION = "mp.jwt.verify.publickey.location";

    private static final String SERVICE_NAME = "oficina-execution-service";
    private static final List<String> SNAPSHOT_PROPERTIES = List.of(
            DEPLOYMENT_ENVIRONMENT,
            DYNAMODB_REGION,
            DYNAMODB_CREDENTIALS_TYPE,
            DYNAMODB_TABLE_PREFIX,
            DYNAMODB_ENDPOINT_OVERRIDE,
            DYNAMODB_ENDPOINT_OVERRIDE_ENV,
            MESSAGING_ENABLED,
            MESSAGING_PUBLISHER_ENABLED,
            MESSAGING_CONSUMER_ENABLED,
            MESSAGING_WORKER_ENABLED,
            MESSAGING_ENDPOINT_OVERRIDE,
            AWS_ACCESS_KEY_ID,
            AWS_SECRET_ACCESS_KEY,
            AWS_SESSION_TOKEN,
            JWT_ISSUER,
            JWT_AUDIENCE,
            JWT_PUBLIC_KEY_LOCATION);

    private final DynamoDbRuntimeDependencies dynamoDbDependencies;
    private final MessagingRuntimeDependencies messagingDependencies;

    public RuntimeDependenciesValidator(
            DynamoDbRuntimeDependencies dynamoDbDependencies,
            MessagingRuntimeDependencies messagingDependencies) {
        this.dynamoDbDependencies = dynamoDbDependencies;
        this.messagingDependencies = messagingDependencies;
    }

    @PostConstruct
    void validarNoStartup() {
        validar(
                ConfigUtils.getProfiles(),
                snapshot(ConfigProvider.getConfig()),
                dynamoDbDependencies::validar,
                messagingDependencies::validar);
    }

    static void validar(
            List<String> profiles,
            Map<String, String> values,
            Runnable dynamoDbProbe,
            Runnable messagingProbe) {
        if (!runtimeProtegido(profiles, values)) {
            return;
        }

        var violations = new ArrayList<String>();
        exigirPreenchido(values, DYNAMODB_REGION, violations);
        exigirPreenchido(values, DYNAMODB_TABLE_PREFIX, violations);
        exigirPreenchido(values, JWT_ISSUER, violations);
        exigirAudienceDoServico(values, violations);
        exigirPreenchido(values, JWT_PUBLIC_KEY_LOCATION, violations);
        exigirCredenciaisDefaultDoDynamoDb(values, violations);
        exigirMensageriaAtiva(values, violations);
        rejeitarEndpointsAlternativos(values, violations);
        validarCredenciaisEstaticas(values, violations);

        if (!violations.isEmpty()) {
            throw new IllegalStateException(
                    "Configuracao invalida para runtime prod/lab: " + String.join("; ", violations));
        }

        executarProbe("DynamoDB", dynamoDbProbe);
        executarProbe("SNS/SQS/IAM", messagingProbe);
    }

    static boolean runtimeProtegido(List<String> profiles, Map<String, String> values) {
        var protectedProfile = profiles.stream()
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> profile.equals("prod") || profile.equals("lab"));
        return protectedProfile || "lab".equalsIgnoreCase(values.getOrDefault(DEPLOYMENT_ENVIRONMENT, ""));
    }

    private static Map<String, String> snapshot(Config config) {
        var values = new LinkedHashMap<String, String>();
        for (var property : SNAPSHOT_PROPERTIES) {
            values.put(property, config.getOptionalValue(property, String.class).orElse(""));
        }
        return Map.copyOf(values);
    }

    private static void exigirPreenchido(Map<String, String> values, String property, List<String> violations) {
        var value = values.getOrDefault(property, "");
        if (value.isBlank() || value.toUpperCase(Locale.ROOT).contains("PLACEHOLDER")) {
            violations.add(property + " deve estar configurado sem placeholder");
        }
    }

    private static void exigirAudienceDoServico(Map<String, String> values, List<String> violations) {
        var audiences = values.getOrDefault(JWT_AUDIENCE, "");
        if (!SERVICE_NAME.equals(audiences.trim())) {
            violations.add(JWT_AUDIENCE + " deve ser exatamente " + SERVICE_NAME);
        }
    }

    private static void exigirCredenciaisDefaultDoDynamoDb(
            Map<String, String> values,
            List<String> violations) {
        if (!"default".equalsIgnoreCase(values.getOrDefault(DYNAMODB_CREDENTIALS_TYPE, ""))) {
            violations.add(DYNAMODB_CREDENTIALS_TYPE + " deve usar a cadeia default/IAM");
        }
    }

    private static void exigirMensageriaAtiva(Map<String, String> values, List<String> violations) {
        for (var property : List.of(
                MESSAGING_ENABLED,
                MESSAGING_PUBLISHER_ENABLED,
                MESSAGING_CONSUMER_ENABLED,
                MESSAGING_WORKER_ENABLED)) {
            if (!Boolean.parseBoolean(values.getOrDefault(property, "false"))) {
                violations.add(property + " deve ser true");
            }
        }
    }

    private static void rejeitarEndpointsAlternativos(Map<String, String> values, List<String> violations) {
        for (var property : List.of(
                DYNAMODB_ENDPOINT_OVERRIDE,
                DYNAMODB_ENDPOINT_OVERRIDE_ENV,
                MESSAGING_ENDPOINT_OVERRIDE)) {
            if (!values.getOrDefault(property, "").isBlank()) {
                violations.add(property + " nao pode ser usado em prod/lab");
            }
        }
    }

    private static void validarCredenciaisEstaticas(Map<String, String> values, List<String> violations) {
        var accessKeyConfigured = !values.getOrDefault(AWS_ACCESS_KEY_ID, "").isBlank();
        var secretKeyConfigured = !values.getOrDefault(AWS_SECRET_ACCESS_KEY, "").isBlank();
        var sessionTokenConfigured = !values.getOrDefault(AWS_SESSION_TOKEN, "").isBlank();
        if (accessKeyConfigured != secretKeyConfigured || (sessionTokenConfigured && !accessKeyConfigured)) {
            violations.add("credenciais AWS estaticas devem informar access key e secret key; session token e opcional");
        }
    }

    private static void executarProbe(String dependency, Runnable probe) {
        try {
            probe.run();
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    "Dependencia obrigatoria indisponivel no startup: " + dependency, exception);
        }
    }
}
