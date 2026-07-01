# TODO do oficina-execution-service

## Próximas Tarefas

- [ ] Copiar e adaptar domínio de catálogo técnico de peças do `oficina-app`.
- [ ] Copiar e adaptar domínio de catálogo técnico de serviços.
- [ ] Copiar e adaptar regras de estoque como referência de comportamento.
- [ ] Reimplementar persistência de catálogo, estoque, execução, Outbox e idempotência em DynamoDB.
- [ ] Alinhar controllers, presenters, DTOs e validações às rotas da [OpenAPI do oficina-execution-service](../oficina-platform/contracts/openapi/oficina-execution-service.yaml).
- [ ] Criar seed limpo para tabelas DynamoDB usando apenas dados funcionais aplicáveis do `import.sql` do `oficina-app`.
- [ ] Implementar diagnóstico, execução e reparo.
- [ ] Implementar movimentos de estoque de entrada, reserva, consumo e estorno.
- [ ] Implementar Outbox para eventos operacionais e de estoque.
- [ ] Implementar publicação dos eventos de Execution.
- [ ] Implementar consumo dos eventos de OS e Billing necessários ao fluxo operacional.
- [x] Implementar fila de execução da OS com prioridade mínima, consulta REST e integração com as transições de diagnóstico e reparo.
- [ ] Criar testes unitários, de integração e de contrato para APIs, DynamoDB, eventos, idempotência e fluxos operacionais da Saga.

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
