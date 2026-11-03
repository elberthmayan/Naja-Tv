# 🐍 Naja TV

> Sua Android TV / Google TV sem bloatware, com uma tela inicial limpa no estilo Apple TV — sem root, sem cabo HDMI, sem desinstalar nada para sempre, e com restauração em uma linha.

**Naja TV é uma edição personalizada do [CleanYourTV](https://github.com/Gathin23/cleanyourtv)** — o projeto open source premiado com trending #1 — com um launcher mais enxuto (APK de ~380 KB), a gaveta de apps remodelada e melhorias de papel de parede (10 MB → 30 MB de limite para imagens 4K).

---

## O que você ganha

| Antes (home de fábrica) | Depois (Naja TV) |
|---|---|
| Home cheia de anúncio patrocinado | Home com uma fileira de apps, relógio gigante e papel de parede seu |
| Apps da operadora que não dá pra tirar | Debloat reversível (`pm disable-user`) — nada destruído |
| Wallpaper que você não escolheu | **Temas**: galeria de papéis seus, com legenda, rotação, e sem bundling de Imagens de terceiros |

Features principais do launcher:

- 🎯 **Fileira única de apps** (dock) com estilo Apple TV, foco com glow
- 📥 **Gaveta de apps** (*OK abre direto*, *segure OK* para menu; pontinho verde marca os fixados na fileira)
- 🖼️ **Temas** como app: troque o papel de parede por uma galeria que você alimenta
- ⏰ Relógio/cronômetro na home, saudação personalizada (`BOM DIA, ELBERTH`), clima/VPN/inputs configuráveis
- 🔄 Rotação automática de wallpapers (`sequential` ou `shuffle`) com legendas
- ↩️ **Restauração em uma linha**: tudo volta ao estado de fábrica

---

## ⚠️ Papéis de parede NÃO vêm com o repositório

Por licença (imagens/animes/marcas de terceiros), **nenhum wallpaper acompanha o download** — e não deve. Você alimenta a galeria você mesmo:

1. Pegue imagens no seu PC (JPEG/PNG/WEBP, até 30 MB cada).
2. Envie para a TV via ADB:  
   `adb push minha-imagem.jpg /sdcard/Android/data/com.gathin.tvhome/files/wallpapers/`
3. Abra o app **Temas** na TV e escolha. Pronto — a legenda e a rotação sozinhas funcionam.

Também funciona empurrando vários de uma vez ou deixando a rotação cuidar da troca a cada N minutos (`wallpaper_rotation_minutes` no config do tema).

---

## Start rápido

**Requisitos:** Windows / macOS / Linux, TV TCL ou qualquer Android TV 11+ na mesma rede Wi-Fi, **Depuração via rede** habilitada nas Opções do desenvolvedor (7 toques em "N.º da versão" → ative a depuração de rede nos ajustes).

**A forma assistida (recomendada):** abra o projeto num assistente de codificação (Claude Code, Cursor, Codebuff…) e peça: *"configura a minha TV com o Naja TV"* — o `PROMPT.md` e `AGENTS.md` orientam o passo a passo em ~15 minutos.

**Manual (resumo):**

```bash
cd launcher
./build-and-install.sh 192.168.x.x        # Windows: build-and-install.ps1
# depois: Home no controle e aproveite
```

Ferramentas extras em `tools/` (álbum de debloat, tema, restauração, capturas, benchmarks — estão com `-h` para ajuda).

---

## Estrutura do repositório

```
naja-tv/
├── launcher/     # app Android do launcher (Java, ~380 KB de APK)
├── tools/        # scripts Python/PowerShell: debloat, tema, restore, ADB
├── addons/       # apps opcionais (32): clima, Frigate (câmeras), biblioteca da semana, sono, telas de descanso…
├── docs/         # guias por TV, troubleshooting, privacidade, restore
├── themes/       # temas de exemplo no formato config.json
├── examples/     # listas de debloat por marca (TCL, Xiaomi, Fire OS…)
├── tests/        # testes das ferramentas
└── PROMPT.md     # receita para um assistente fazer a limpeza na sua TV
```

---

## Créditos & licença

- **Base**: [CleanYourTV](https://github.com/Gathin23/cleanyourtv) por Gathin — fork respeitoso com crédito. Sem ele, nada disso existiria.
- **Licença**: [MIT](LICENSE) (contratos do CleanYourTV mantidos). Papéis de parede por sua conta e risco de licença. Este projeto **não é afiliado** à TCL, Google ou qualquer operadora; marcas citadas pertencem aos donos.

## Quando algo der errado

- A TV ainda está em modo "Home da fábrica": rode restore (`tools/restore.sh`) ou siga [docs/restore.md](docs/restore.md)
- Firmware atualizou e voltou tudo? Reaproveite o mesmo procedimento — [docs/firmware-updates.md](docs/firmware-updates.md)
- Dúvidas e erros clássicos: [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) e [docs/faq.md](docs/faq.md)
