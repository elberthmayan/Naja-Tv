🐍 Naja TV
Sua Android TV / Google TV sem bloatware, com uma tela inicial limpa no estilo Apple TV — sem root, sem cabo HDMI, sem desinstalar nada para sempre e com restauração em uma linha.

Naja TV é uma edição personalizada do CleanYourTV — o projeto open source premiado com trending #1 — com um launcher mais enxuto (APK de ~380 KB), a gaveta de apps remodelada e melhorias de papel de parede (10 MB → 30 MB de limite para imagens 4K).

O que você ganha
Antes (home de fábrica)	Depois (Naja TV)
Home cheia de anúncios patrocinados	Home com uma fileira de apps, relógio gigante e papel de parede personalizado
Apps da operadora que não dá para tirar	Debloat reversível (pm disable-user) — nada destruído
Wallpaper que você não escolheu	Temas: galeria de papéis personalizados, com legenda, rotação e sem bundling de imagens de terceiros
Features principais do launcher
🎯 Fileira única de apps (dock) com estilo Apple TV e foco com glow.
📥 Gaveta de apps (OK abre direto, segure OK para menu; pontinho verde marca os fixados na fileira).
🖼️ Temas como app: troque o papel de parede por uma galeria personalizada.
⏰ Relógio/cronômetro na home, saudação personalizada (BOM DIA, {USUARIO}), clima/VPN/inputs configuráveis.
🔄 Rotação automática de wallpapers (sequential ou shuffle) com legendas.
↩️ Restauração em uma linha: tudo volta ao estado de fábrica.
⚠️ Papéis de parede NÃO vêm com o repositório

Por questões de licença (imagens, animes e marcas de terceiros), nenhum wallpaper acompanha o download — e não deve.

Você alimenta a galeria por conta própria:

Pegue imagens no seu PC (JPEG/PNG/WEBP, até 30 MB cada).
Envie para a TV via ADB:
bash
adb push minha-imagem.jpg /sdcard/Android/data/com.gathin.tvhome/files/wallpapers/

Abra o app Temas na TV e escolha. Pronto! A legenda e a rotação funcionam automaticamente.

Também funciona enviando várias imagens de uma vez ou deixando a rotação cuidar da troca a cada N minutos (wallpaper_rotation_minutes no config do tema).

Start rápido

Requisitos: Windows / macOS / Linux, TV TCL ou qualquer Android TV 11+ na mesma rede Wi-Fi, Depuração via rede habilitada nas Opções do desenvolvedor (7 toques em "N.º da versão" → ative a depuração de rede nos ajustes).

A forma assistida (recomendada): abra o projeto num assistente de codificação (Claude Code, Cursor, Codebuff…) e peça:

"Configura a minha TV com o Naja TV."

O PROMPT.md e o AGENTS.md orientam o passo a passo em aproximadamente 15 minutos.

Manual (resumo):

bash
cd launcher
./build-and-install.sh 192.168.x.x
# Windows: build-and-install.ps1
# Depois: aperte Home no controle e aproveite!


Ferramentas extras em tools/ (álbum de debloat, tema, restauração, capturas e benchmarks — todas com -h para ajuda).

Estrutura do repositório
text
naja-tv/
├── launcher/     # App Android do launcher (Java, ~380 KB de APK)
├── tools/        # Scripts Python/PowerShell: debloat, tema, restore, ADB
├── addons/       # Apps opcionais (32): clima, Frigate (câmeras), biblioteca da semana, sono, telas de descanso…
├── docs/         # Guias por TV, troubleshooting, privacidade, restore
├── themes/       # Temas de exemplo no formato config.json
├── examples/     # Listas de debloat por marca (TCL, Xiaomi, Fire OS…)
├── tests/        # Testes das ferramentas
└── PROMPT.md     # Receita para um assistente fazer a limpeza na sua TV

Créditos & licença
Base: CleanYourTV, por Gathin — fork respeitoso com os devidos créditos. Sem ele, nada disso existiria.
Licença: MIT (contratos do CleanYourTV mantidos).
Papéis de parede são de responsabilidade do usuário quanto aos direitos de uso e licenciamento.
Este projeto não é afiliado à TCL, Google ou qualquer operadora. Todas as marcas citadas pertencem aos seus respectivos proprietários.
Quando algo der errado
A TV ainda está em modo "Home da fábrica": rode o restore (tools/restore.sh) ou siga docs/restore.md.
Firmware atualizou e voltou tudo? Reaproveite o mesmo procedimento — docs/firmware-updates.md.
Dúvidas e erros clássicos: docs/TROUBLESHOOTING.md e docs/faq.md.

Naja TV — sua TV, suas regras. 🐍
