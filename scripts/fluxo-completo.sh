#!/usr/bin/env bash
# Percorre a API de ponta a ponta:
# destino → pacote → item do roteiro → saída → cliente → cotação → reserva → pagamento → aprovação.
# Uso: ./scripts/fluxo-completo.sh   (API em http://localhost:8080; mude com API=...)
set -euo pipefail

API="${API:-http://localhost:8080/api}"
# A saída precisa ser no futuro: por padrão, daqui a 60 dias
PARTIDA="${PARTIDA:-$(date -d '+60 days' +%F 2>/dev/null || date -v+60d +%F)}"
# CPF válido de exemplo; rodar o script de novo no mesmo banco dá 409 (CPF duplicado). Troque com CPF=...
CPF="${CPF:-529.982.247-25}"

# Corpo enviado pela entrada padrão: no Windows, o curl converte argumentos de linha de comando para o
# codepage local e quebraria os acentos (o JSON chegaria como UTF-8 inválido).
post() { printf '%s' "${2:-}" | curl -sS --fail-with-body -X POST "$API$1" -H 'Content-Type: application/json' --data-binary @-; }
id() { grep -o '"id":[0-9]*' | head -1 | cut -d: -f2; }
passo() { printf '\n== %s\n' "$1"; }

passo "1. Destino"
DESTINO=$(post /destinos '{"nome":"Lençóis Maranhenses","cidade":"Barreirinhas","uf":"MA","pais":"Brasil"}')
echo "$DESTINO"
DESTINO_ID=$(echo "$DESTINO" | id)

passo "2. Pacote (R$ 1.890,00 por passageiro, 5 dias)"
PACOTE=$(post /pacotes "{\"destinoId\":$DESTINO_ID,\"nome\":\"Lençóis 5 dias\",\"duracaoDias\":5,\"precoBase\":1890.00,\"ativo\":true}")
echo "$PACOTE"
PACOTE_ID=$(echo "$PACOTE" | id)

passo "3. Item do roteiro"
post "/pacotes/$PACOTE_ID/itens" '{"tipo":"PASSEIO","titulo":"Lagoa Azul de jardineira","dia":2,"ordem":0}'
echo

passo "4. Saída em $PARTIDA com 20 vagas"
SAIDA=$(post "/pacotes/$PACOTE_ID/saidas" "{\"dataPartida\":\"$PARTIDA\",\"capacidadeTotal\":20}")
echo "$SAIDA"
SAIDA_ID=$(echo "$SAIDA" | id)

passo "5. Cliente"
CLIENTE=$(post /clientes "{\"nome\":\"Maria Souza\",\"documento\":\"$CPF\",\"email\":\"maria@email.com\",\"telefone\":\"(98) 99999-0000\"}")
echo "$CLIENTE"
CLIENTE_ID=$(echo "$CLIENTE" | id)

passo "6. Cotação: 6 passageiros no cartão em 6x (desconto de grupo + taxa de parcelamento)"
post /cotacoes "{\"saidaId\":$SAIDA_ID,\"qtdPassageiros\":6,\"forma\":\"CARTAO\",\"parcelas\":6}"
echo

passo "7. Reserva de 2 passageiros"
RESERVA=$(post /reservas "{\"clienteId\":$CLIENTE_ID,\"saidaId\":$SAIDA_ID,\"qtdPassageiros\":2}")
echo "$RESERVA"
CODIGO=$(echo "$RESERVA" | grep -o '"codigo":"[^"]*"' | cut -d'"' -f4)

passo "8. Pagamento integral no PIX (R$ 3.780,00)"
PAGAMENTO=$(post "/reservas/$CODIGO/pagamentos" '{"valor":3780.00,"forma":"PIX","parcelas":1}')
echo "$PAGAMENTO"
PAGAMENTO_ID=$(echo "$PAGAMENTO" | id)

passo "9. Aprovação do pagamento (simula o webhook do gateway)"
post "/pagamentos/$PAGAMENTO_ID/aprovacao"
echo

passo "10. Reserva $CODIGO e vagas da saída"
curl -sS --fail-with-body "$API/reservas/$CODIGO"
echo
curl -sS --fail-with-body "$API/saidas/$SAIDA_ID"
echo
