-- Empresa de exemplo
INSERT INTO TB_EMPRESA (NOME, CNPJ, SETOR, PAIS)
VALUES ('EcoTech Brasil', '12.345.678/0001-99', 'Tecnologia', 'Brasil');

-- Usuário admin inicial (senha: admin123 em BCrypt)
INSERT INTO TB_USUARIO (NOME, EMAIL, SENHA, ROLE)
VALUES (
           'Administrador',
           'admin@esg.com',
           '$2a$10$wT0l47L5846K6O4U0gD35eGgVz36c84m99qFqUeP1fHn3GjG6o.1S',
           'ADMIN'
       );

COMMIT;