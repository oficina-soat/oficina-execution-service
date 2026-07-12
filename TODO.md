# TODO do oficina-execution-service

## Próximas Tarefas

- [x] Copiar e adaptar domínio de catálogo técnico de peças do `oficina-app`.
- [x] Copiar e adaptar domínio de catálogo técnico de serviços.
- [x] Copiar e adaptar regras de estoque como referência de comportamento.
- [x] Reimplementar persistência de catálogo, estoque, execução, Outbox e idempotência em DynamoDB.
- [x] Alinhar controllers, presenters, DTOs e validações às rotas da [OpenAPI do oficina-execution-service](../oficina-platform/contracts/openapi/oficina-execution-service.yaml).
- [x] Criar seed limpo para tabelas DynamoDB usando apenas dados funcionais aplicáveis do `import.sql` do `oficina-app`.
- [x] Implementar diagnóstico, execução e reparo.
- [x] Implementar movimentos de estoque de entrada, reserva, consumo e estorno.
- [x] Implementar Outbox para eventos operacionais e de estoque.
- [x] Implementar publicação dos eventos de Execution.
- [x] Implementar consumo dos eventos de OS e Billing necessários ao fluxo operacional.
- [x] Implementar fila de execução da OS com prioridade mínima, consulta REST e integração com as transições de diagnóstico e reparo.
- [x] Criar testes unitários e de integração mínimos para APIs, DynamoDB, eventos, idempotência e fluxos operacionais da Saga.
- [x] Configurar o mínimo normativo de 80% e gate local de 90% com JaCoCo e evidência no README/CI, conforme [Padrão BDD, Cobertura e Qualidade](../oficina-platform/docs/delivery/bdd-testing.md).
- [x] Validar contratos OpenAPI, schemas JSON de eventos, erro padronizado, idempotência e Saga.
- [x] Copiar e adaptar workflows de CI/CD, garantindo build, testes, Quality Gate, publicação de imagem e deploy automatizado condicionado por variáveis de ambiente.
- [x] Impedir fallback silencioso e validar DynamoDB, SNS/SQS, IAM, JWT e configurações obrigatórias no startup dos runtimes `prod` e `lab`.

## Eventos Produzidos

- `diagnosticoIniciado`
- `diagnosticoFinalizado`
- `execucaoIniciada`
- `execucaoFinalizada`
- `estoqueAcrescentado`
- `estoqueBaixado`

## Eventos Consumidos

- `ordemDeServicoCriada`
- `pecaIncluidaNaOrdemDeServico`
- `servicoIncluidoNaOrdemDeServico`
- `orcamentoAprovado`
- `ordemDeServicoFinalizada`
- `sagaCompensada`
- `sagaFinalizadaComSucesso`
