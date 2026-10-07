-- RF13: reconhecimento do documento do certificado pela diretoria.
--
-- O associado anexa o PDF do certificado; ninguem confere de forma
-- rotineira. A conferencia acontece quando ela importa: ao procurar
-- quem pode atender uma demanda, a diretoria ve na ficha quais
-- certificados ainda nao foram reconhecidos e reconhece ali.
--
-- Evita uma fila de conferencia que talvez nunca fosse trabalhada, e
-- amarra o esforco ao momento em que ele tem consequencia.

ALTER TABLE certificado
    -- Nulo = documento ainda nao reconhecido. O proprio timestamp faz
    -- as vezes de marcador, mesmo idioma de profissional.data_exclusao_solicitada:
    -- um booleano ao lado seria estado duplicado, podendo divergir.
    ADD COLUMN validado_em  timestamp,

    -- Login de quem reconheceu. O reconhecimento e ato de pessoa, e
    -- fica no proprio registro, nao apenas no log de acesso: e
    -- propriedade duravel do certificado, nao linha de auditoria.
    ADD COLUMN validado_por varchar(100);

-- Um certificado so pode estar validado se houver quem o validou, e
-- vice-versa. Sem isso o par poderia divergir por engano de codigo.
ALTER TABLE certificado
    ADD CONSTRAINT ck_certificado_validacao_completa
    CHECK ((validado_em IS NULL AND validado_por IS NULL)
        OR (validado_em IS NOT NULL AND validado_por IS NOT NULL));

-- Nao ha validacao sem documento: reconhecer o que nao foi anexado nao
-- significa nada. O banco garante o que o service tambem checa.
ALTER TABLE certificado
    ADD CONSTRAINT ck_certificado_validacao_exige_arquivo
    CHECK (validado_em IS NULL OR arquivo_path IS NOT NULL);

-- A ficha mostra os nao reconhecidos primeiro, e a diretoria pode
-- querer listar pendencias.
CREATE INDEX idx_certificado_validado_em ON certificado (validado_em);
