package br.com.oficina.execution.framework.dynamodb;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import java.util.UUID;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class DynamoDbLocalTestResource implements QuarkusTestResourceLifecycleManager {
    private GenericContainer<?> container;
    private DynamoDbClient client;

    @Override
    public Map<String, String> start() {
        container = DynamoDbLocalTestSupport.startContainer();
        var endpoint = DynamoDbLocalTestSupport.endpoint(container);
        client = DynamoDbLocalTestSupport.client(endpoint);
        var tablePrefix = "oficina-execution-test-" + UUID.randomUUID().toString().substring(0, 8);
        DynamoDbLocalTestSupport.createTables(client, new DynamoDbTableNames(tablePrefix));
        return Map.of(
                "quarkus.dynamodb.endpoint-override", endpoint,
                "quarkus.dynamodb.aws.credentials.type", "static",
                "quarkus.dynamodb.aws.credentials.static-provider.access-key-id", "test",
                "quarkus.dynamodb.aws.credentials.static-provider.secret-access-key", "test",
                "oficina.dynamodb.table-prefix", tablePrefix);
    }

    @Override
    public void stop() {
        if (client != null) {
            client.close();
        }
        if (container != null) {
            container.stop();
        }
    }
}
