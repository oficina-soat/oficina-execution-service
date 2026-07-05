package br.com.oficina.execution.framework.dynamodb;

import java.util.LinkedHashMap;
import java.util.Map;

public record DynamoDbItem(
        String tableName,
        String pk,
        String sk,
        String entityType,
        Map<String, Object> attributes) {

    public DynamoDbItem {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("Nome da tabela DynamoDB e obrigatorio.");
        }
        if (pk == null || pk.isBlank()) {
            throw new IllegalArgumentException("PK do item DynamoDB e obrigatoria.");
        }
        if (sk == null || sk.isBlank()) {
            throw new IllegalArgumentException("SK do item DynamoDB e obrigatoria.");
        }
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType do item DynamoDB e obrigatorio.");
        }
        attributes = Map.copyOf(attributes == null ? Map.of() : new LinkedHashMap<>(attributes));
    }

    public String key() {
        return pk + "|" + sk;
    }
}
