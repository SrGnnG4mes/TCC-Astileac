-- O PostgreSQL cria indice automatico para PRIMARY KEY e UNIQUE, mas
-- NAO para coluna de chave estrangeira. Sem os indices abaixo, toda
-- consulta "dados de um profissional" e "profissionais de uma area"
-- faz varredura sequencial na tabela.

-- Caso de uso mais importante do sistema (RF08): listar profissionais
-- de uma area de atuacao. A PK composta de profissional_area comeca em
-- profissional_id, entao nao atende a um filtro so por area.
CREATE INDEX idx_profissional_area_area ON profissional_area (area_atuacao_id);

-- Tabelas filhas, consultadas sempre a partir do profissional.
-- endereco e usuario ficam de fora: profissional_id ja e UNIQUE neles,
-- e UNIQUE ja cria indice.
CREATE INDEX idx_contato_profissional ON contato (profissional_id);
CREATE INDEX idx_formacao_profissional ON formacao (profissional_id);
CREATE INDEX idx_certificado_profissional ON certificado (profissional_id);
CREATE INDEX idx_vinculo_profissional ON vinculo_empregaticio (profissional_id);

-- Certificado tambem e consultado pela formacao que ele comprova.
CREATE INDEX idx_certificado_formacao ON certificado (formacao_id);
