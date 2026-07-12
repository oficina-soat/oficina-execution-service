package br.com.oficina.execution.framework.dynamodb;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DynamoDbTableNames {
    private final String prefix;

    public DynamoDbTableNames(@ConfigProperty(name = "oficina.dynamodb.table-prefix", defaultValue = "oficina-execution-lab") String prefix) {
        this.prefix = prefix;
    }

    public String catalogo() {
        return prefix + "-catalogo";
    }

    public String estoque() {
        return prefix + "-estoque";
    }

    public String execucoes() {
        return prefix + "-execucoes";
    }

    public String outbox() {
        return prefix + "-outbox";
    }

    public String idempotencia() {
        return prefix + "-idempotencia";
    }

    public List<String> todas() {
        return List.of(catalogo(), estoque(), execucoes(), outbox(), idempotencia());
    }
}
