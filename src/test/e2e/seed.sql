-- Exclusivo para o banco descartável do job de navegador.
DO $$ BEGIN IF current_database() <> 'criare3d_e2e' THEN RAISE EXCEPTION 'Este script só pode rodar em criare3d_e2e'; END IF; END $$;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
DO $$
DECLARE largura int; t uuid; p uuid; f uuid; i uuid; b uuid; h uuid;
BEGIN
 FOREACH largura IN ARRAY ARRAY[320,1280] LOOP
 t := ('00000000-0000-4000-8000-' || lpad(largura::text,12,'0'))::uuid;
 p := ('00000000-0000-4000-8000-' || lpad((100000+largura)::text,12,'0'))::uuid;
 f := ('00000000-0000-4000-8000-' || lpad((200000+largura)::text,12,'0'))::uuid;
 i := ('00000000-0000-4000-8000-' || lpad((300000+largura)::text,12,'0'))::uuid;
 b := ('00000000-0000-4000-8000-' || lpad((400000+largura)::text,12,'0'))::uuid;
 h := ('00000000-0000-4000-8000-' || lpad((500000+largura)::text,12,'0'))::uuid;
 INSERT INTO tenant(id,nome,slug,cor_primaria,cor_destaque,cor_fundo) VALUES(t,'Teste navegador '||largura,'e2e-'||largura,'#492e1e','#e9a695','#f5e7da');
 INSERT INTO usuario(id,email,password,perfil,tenant_id) VALUES(t,'e2e'||largura||'@example.test',crypt('E2eSenha123!',gen_salt('bf')),'USER',t);
 INSERT INTO peca(id,nome_peca,qnt_fil_peca,tmp_peca,usuario_id,tenant_id,categoria) VALUES(p,'Peça navegador',10,30,t,t,'Decoração');
 INSERT INTO filamento(id,nome_fil,cor_fil,peso_rolo_fil,preco_rolo_fil,estoque_gramas,data_cadastro,usuario_id,tenant_id) VALUES(f,'PLA navegador','Preto',1000,100,1000,now(),t,t);
 INSERT INTO impressora(id,nome_imp,modelo_imp,potencia_imp,usuario_id,tenant_id) VALUES(i,'Impressora navegador','Modelo teste',100,t,t);
 INSERT INTO embalagem(id,nome_emb,custo_uni_emb,usuario_id,tenant_id) VALUES(b,'Caixa navegador',1,t,t);
 INSERT INTO calculo_historico(id,nome_peca_hist,nome_fil_hist,preco_rolo_hist,peso_rolo_fil,qntd_fil_hist,nome_imp_hist,pot_hist,temp_hist,tarifa_hist,custo_fil_hist,custo_energia_hist,nome_emb_hist,custo_emb_hist,custo_total_hist,markup_hist,preco_sug_hist,data_hist,unidades_hist,fornadas_hist,pecas_por_impressao_hist,minutos_maquina_lote_hist,gramas_totais_lote_hist,lucro_liquido_hist,tenant_id,usuario_id,id_peca_fk,id_impressora_fk,id_filamento_fk,id_embalagem_fk)
 VALUES(h,'Peça navegador','PLA navegador',100,1000,10,'Impressora navegador',100,30,1,1,0.05,'Caixa navegador',1,2.05,100,4.10,now(),3,3,1,90,30,2.05,t,t,p,i,f,b);
 END LOOP;
END $$;
