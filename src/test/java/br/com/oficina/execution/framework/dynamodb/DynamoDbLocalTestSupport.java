package br.com.oficina.execution.framework.dynamodb;

import java.net.URI;
import java.util.List;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

public final class DynamoDbLocalTestSupport {
    private static final DockerImageName DYNAMODB_LOCAL_IMAGE = DockerImageName.parse("amazon/dynamodb-local:latest");
    private static final int DYNAMODB_PORT = 8000;

    private DynamoDbLocalTestSupport() {
    }

    public static GenericContainer<?> startContainer() {
        var container = new GenericContainer<>(DYNAMODB_LOCAL_IMAGE)
                .withExposedPorts(DYNAMODB_PORT);
        container.start();
        return container;
    }

    public static String endpoint(GenericContainer<?> container) {
        return "http://%s:%d".formatted(container.getHost(), container.getMappedPort(DYNAMODB_PORT));
    }

    public static DynamoDbClient client(String endpoint) {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();
    }

    public static void createTables(DynamoDbClient client, DynamoDbTableNames tableNames) {
        createTable(
                client,
                tableNames.catalogo(),
                attributes("PK", "SK", "entityType", "nomeNormalizado", "codigo"),
                List.of(gsi("GSI1", "entityType", "nomeNormalizado"), gsi("GSI2", "codigo", "entityType")));
        createTable(
                client,
                tableNames.estoque(),
                attributes("PK", "SK", "ordemServicoId", "createdAt", "movimentoId", "entityType"),
                List.of(gsi("GSI1", "ordemServicoId", "createdAt"), gsi("GSI2", "movimentoId", "entityType")));
        createTable(
                client,
                tableNames.execucoes(),
                attributes("PK", "SK", "ordemServicoId", "entityType", "status", "updatedAt", "filaStatus", "prioridadeCriadoEm"),
                List.of(
                        gsi("GSI1", "ordemServicoId", "entityType"),
                        gsi("GSI2", "status", "updatedAt"),
                        gsi("GSI3", "filaStatus", "prioridadeCriadoEm")));
        createTable(
                client,
                tableNames.outbox(),
                attributes("PK", "SK", "status", "nextAttemptAt", "aggregateId", "createdAt"),
                List.of(gsi("GSI1", "status", "nextAttemptAt"), gsi("GSI2", "aggregateId", "createdAt")));
        createTable(client, tableNames.idempotencia(), attributes("PK", "SK"), List.of());
    }

    private static void createTable(
            DynamoDbClient client,
            String tableName,
            List<AttributeDefinition> attributes,
            List<GlobalSecondaryIndex> globalSecondaryIndexes) {
        var requestBuilder = CreateTableRequest.builder()
                .tableName(tableName)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .keySchema(key("PK", KeyType.HASH), key("SK", KeyType.RANGE))
                .attributeDefinitions(attributes);
        if (!globalSecondaryIndexes.isEmpty()) {
            requestBuilder.globalSecondaryIndexes(globalSecondaryIndexes);
        }
        client.createTable(requestBuilder.build());
        client.waiter().waitUntilTableExists(DescribeTableRequest.builder().tableName(tableName).build());
    }

    private static List<AttributeDefinition> attributes(String... names) {
        return java.util.Arrays.stream(names)
                .distinct()
                .map(name -> AttributeDefinition.builder()
                        .attributeName(name)
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .toList();
    }

    private static GlobalSecondaryIndex gsi(String name, String pk, String sk) {
        return GlobalSecondaryIndex.builder()
                .indexName(name)
                .keySchema(key(pk, KeyType.HASH), key(sk, KeyType.RANGE))
                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                .build();
    }

    private static KeySchemaElement key(String name, KeyType keyType) {
        return KeySchemaElement.builder()
                .attributeName(name)
                .keyType(keyType)
                .build();
    }
}
