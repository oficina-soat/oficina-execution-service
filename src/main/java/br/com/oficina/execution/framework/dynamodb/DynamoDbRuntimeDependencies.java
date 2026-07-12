package br.com.oficina.execution.framework.dynamodb;

import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;

@ApplicationScoped
public class DynamoDbRuntimeDependencies {
    private final DynamoDbTableNames tableNames;
    private final DynamoDbClient dynamoDbClient;

    public DynamoDbRuntimeDependencies(DynamoDbTableNames tableNames, DynamoDbClient dynamoDbClient) {
        this.tableNames = tableNames;
        this.dynamoDbClient = dynamoDbClient;
    }

    public void validar() {
        for (var tableName : tableNames.todas()) {
            validarTabela(tableName);
        }
    }

    private void validarTabela(String tableName) {
        TableDescription table;
        try {
            table = dynamoDbClient.describeTable(request -> request.tableName(tableName)).table();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Falha ao validar tabela DynamoDB obrigatoria: " + tableName, exception);
        }
        if (table.tableStatus() != TableStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Tabela DynamoDB obrigatoria nao esta ACTIVE: %s (%s)".formatted(tableName, table.tableStatus()));
        }
    }
}
