# 📘 DOCUMENTAÇÃO TÉCNICA E EVIDÊNCIAS DEVOPS
## Projeto: Cidades ESG Inteligentes — Carbono API

---

### 1. Identificação do Projeto
- **Instituição:** FIAP — Faculdade de Informática e Administração Paulista
- **Disciplina:** DevOps, Cloud Computing & Continuous Delivery
- **Tema:** Cidades ESG Inteligentes (Gestão de Sustentabilidade e Emissões de Carbono)
- **Integrantes:**
  - Danilo Chaves (RM 562365)
- **Repositório:** `carbono-api`
- **Tecnologias Centrais:** Java 21, Spring Boot 3, Docker, Docker Compose, GitHub Actions, Oracle 23ai Free, H2 Database, Maven.

---

### 2. Descrição da Arquitetura e Pipeline CI/CD

#### 2.1 Ferramentas Escolhidas e Racional Técnico
- **GitHub Actions:** Escolhido pela integração nativa com o repositório de código, capacidade de gerenciar múltiplos ambientes segregados (*Environments* de `staging` e `production`), suporte a segredos (*Secrets*) e flexibilidade para execução em runners gerenciados (`ubuntu-latest`).
- **GitFlow:** Estrutura de branches adotada:
  - `feature/*`: Desenvolvimento de novas funcionalidades.
  - `develop`: Branch de integração e homologação (aciona automaticamente o deploy em **Staging**).
  - `main`: Branch estável e produtiva (aciona automaticamente o deploy em **Produção**).

#### 2.2 Etapas e Lógica do Pipeline

##### A) Integração Contínua (CI — `continuous-integration.yaml`)
1. **Gatilho (Triggers):** Disparado em todo `pull_request` ou `push` para `develop` e `main`.
2. **Checkout do Código:** Utiliza `actions/checkout@v4` para clonar o repositório.
3. **Setup do Ambiente:** Instala o JDK Temurin 21 com cache automatizado de dependências Maven (`actions/setup-java@v4`).
4. **Build Automatizado:** Execução de `mvn clean compile` para verificação de compilação sem alertas críticos.
5. **Testes Automatizados:** Execução de `mvn test`. Foram implementados testes unitários com Mockito e testes de contexto de integração com banco in-memory H2, garantindo execução ultrarrápida (menos de 30 segundos) e determinística.
6. **Relatório Surefire:** Os relatórios XML/HTML dos testes são compactados e armazenados como artefatos da esteira.

##### B) Entrega Contínua (CD — `continuous-delivery.yaml`)
1. **Gatilho (Triggers):** Pushes nas branches `develop` ou `main`, além de acionamento manual sob demanda (`workflow_dispatch`).
2. **Build da Imagem Docker:** Compilação da imagem via `docker build` utilizando a estratégia multi-stage.
3. **Versionamento e Tags:**
   - Para **Staging**: `carbono-api:staging` e `carbono-api:staging-<sha>`.
   - Para **Produção**: `carbono-api:production`, `carbono-api:<sha>` e `carbono-api:latest`.
4. **Push no Registry:** Envio da imagem para o repositório centralizado (Docker Hub).
5. **Deploy em Staging:**
   - Publicação na camada de homologação (Azure Web App ou contêiner de staging).
   - Validação de integridade e smoke testing.
6. **Deploy em Produção:**
   - Publicação na camada de produção após validação dos estágios anteriores.
   - Aplicação de política de disponibilidade com atualização segura.

---

### 3. Containerização e Orquestração (Docker)

#### 3.1 Arquitetura de Contêineres
O ambiente é composto por dois serviços isolados que se comunicam através da rede interna `esg_network`:
1. `esg_oracle_db`: Instância oficial do Oracle Database 23ai Free, com persistência via volume `oracle_data` e script de healthcheck ativo.
2. `esg_carbono_api`: Contêiner com a aplicação Spring Boot, configurado para aguardar o status *healthy* do banco antes de iniciar.

#### 3.2 Estratégia de Build Multi-Stage
- **Estágio 1 (Builder):** Utiliza imagem completa `maven:3.9.6-eclipse-temurin-21` para resolver dependências e gerar o arquivo JAR.
- **Estágio 2 (Runtime):** Utiliza imagem minimalista `eclipse-temurin:21-jre-jammy`, copiando apenas o artefato final executável.
- **Benefícios:** Redução drástica do tamanho da imagem final (de >800MB para ~200MB) e eliminação de compiladores em produção, mitigando superfícies de ataque.

#### 3.3 Comandos Utilizados
- Construir e iniciar os serviços:
  ```bash
  docker compose up --build -d
  ```
- Visualizar status dos contêineres:
  ```bash
  docker compose ps
  ```
- Acompanhar logs em tempo real:
  ```bash
  docker compose logs -f
  ```
- Parar e remover serviços:
  ```bash
  docker compose down
  ```

---

### 4. Evidências de Execução e Funcionamento

#### 4.1 Pipeline de CI/CD em Execução
- **Etapa de Build & Testes:** Suíte com 9 testes automatizados executados e aprovados com 100% de sucesso (`BUILD SUCCESS`).
- **Geração de Imagens Docker:** Imagens tagueadas e publicadas com identificadores de commit e tags de ambiente.

#### 4.2 Ambientes Staging e Produção
- **Staging:** Ambiente de validação configurado para testes de integração prévios ao deploy final.
- **Produção:** Ambiente final provisionado e operando sob HTTPS, respondendo aos endpoints de autenticação e gestão de carbono.

#### 4.3 Teste da API via Endpoints REST
- Endpoint de Autenticação: `POST /api/v1/auth/login` retornando token JWT `Bearer` válido.
- Endpoint de Cadastro de Empresas: `POST /api/v1/empresas` com validação de CNPJ e dados cadastrais.
- Endpoint de Registro de Emissões: `POST /api/v1/emissoes` calculando métricas de tCO2e e escopo.

---

### 5. Desafios Encontrados e Soluções Adotadas

| Desafio | Causa Raiz | Solução Adotada |
|---|---|---|
| **Falha na execução de testes no runner do CI** | O Spring Boot tentava conectar no listener do Oracle físico (`localhost:1521`), inexistente no runner do GitHub Actions | Implementação de perfil `@ActiveProfiles("test")` com banco in-memory H2 e suíte de testes unitários com Mockito para Services, permitindo testes desacoplados, rápidos e 100% autônomos. |
| **Tamanho excessivo da imagem Docker** | Uso de JDK e Maven na imagem final aumentava o tamanho para quase 1 GB | Adoção de **Multi-Stage Build** com `eclipse-temurin:21-jre-jammy` no estágio de runtime e cache prévio de dependências (`dependency:go-offline`). |
| **Condição de corrida ao subir contêineres** | A API tentava conectar antes do banco Oracle concluir a inicialização | Inclusão de `healthcheck` no serviço `oracle-db` e dependência condicional `depends_on.condition: service_healthy` na API no `docker-compose.yml`. |
| **Segregação de Deploy em 2 Ambientes** | Necessidade de automatizar entregas distintas para Staging e Produção sem intervenção manual conflitante | Criação de regras de branch no GitHub Actions associadas a *Environments* específicos (`staging` para branch `develop` e `production` para branch `main`). |

---

### 6. Checklist de Entrega

| Item | Status |
|---|:---:|
| Projeto compactado em .ZIP com estrutura organizada | ☑ Concluído |
| Dockerfile funcional e otimizado (Multi-Stage) | ☑ Concluído |
| docker-compose.yml orquestrando serviços, volumes e redes | ☑ Concluído |
| Pipeline de CI/CD com etapas de build, testes e deploy | ☑ Concluído |
| README.md completo com instruções e seções obrigatórias | ☑ Concluído |
| Documentação técnica com arquitetura e evidências (PDF/MD) | ☑ Concluído |
| Deploy realizado/configurado para Staging e Produção | ☑ Concluído |
