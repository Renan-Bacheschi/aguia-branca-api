# language: pt
Funcionalidade: Gestão de ideias para redução de desperdício
  Como equipe de sustentabilidade
  Quero registrar e avaliar ideias ligadas a estratégias ativas
  Para reduzir desperdícios e medir resultados ESG

  Cenário: Operador cadastra ideia de redução de desperdício em estratégia ativa
    Dado contas reais de operador, gestor e líder
    E uma estratégia ativa de redução de desperdício criada pelo líder
    Quando o operador cadastra a ideia de reaproveitamento de insumos
    Então a ideia é criada com HTTP 201, estratégia vinculada e contrato válido

  Cenário: Gestor aprova uma ideia submetida com os dados obrigatórios
    Dado contas reais de operador, gestor e líder
    E uma ideia de desperdício enviada pelo operador
    Quando o gestor aprova a ideia com prioridade alta e justificativa
    Então a ideia fica aprovada com prioridade "HIGH" e contrato válido

  Cenário: Operador não pode aprovar a própria ideia
    Dado contas reais de operador, gestor e líder
    E uma ideia de desperdício enviada pelo operador
    Quando o operador tenta aprovar a própria ideia
    Então recebe acesso negado HTTP 403 com contrato de erro válido
    E a ideia continua enviada ao consultá-la
