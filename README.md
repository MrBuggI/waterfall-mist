# Waterfall Mist

[![Build](https://github.com/MrBuggI/waterfall-mist/actions/workflows/build.yml/badge.svg)](https://github.com/MrBuggI/waterfall-mist/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/MrBuggI/waterfall-mist)](https://github.com/MrBuggI/waterfall-mist/releases/latest)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-62B47A?style=flat-square)
![Loader](https://img.shields.io/badge/Loader-Fabric-DBD0B4?style=flat-square)
![Side](https://img.shields.io/badge/Side-Client--only-blue?style=flat-square)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red?style=flat-square)
![Modpacks](https://img.shields.io/badge/Modpacks-Welcome-brightgreen?style=flat-square)

**Waterfall Mist** — ультралёгкий клиентский визуальный мод для Minecraft, добавляющий мягкий атмосферный туман и брызги в местах падения водопадов.

> **English:** A lightweight, pure client-side visual mod that adds atmospheric mist, gentle foam, and splash particle effects where waterfalls meet water below. Zero server requirements, zero performance impact.

---

## 🌊 Особенности мода (Features)

* **Естественный туман у водопадов:** В местах, где вода падает с высоты (от 3+ блоков) и разбивается о поверхность внизу, образуются клубы мягкого водяного пара и брызги.
* **100% Client-Side:** Мод работает исключительно на клиенте. Вы можете играть с ним на любых ванильных серверах (Hypixel, приватные SMP) — серверная часть не требуется.
* **Ванильная эстетика:** Визуальные эффекты идеально вписываются в оригинальный стиль Minecraft, дополняя пейзажи без вычурности и лишних шейдерных багов.
* **Высокая производительность (Zero-lag):** Эффекты оптимизированы для слабых ПК. Кэширование высоты падения и лимиты спавна частиц не просаживают FPS даже рядом с гигантскими горными каскадами.
* **Эмбиент-звуки:** Мягкий естественный гул падающей воды у подножия водопада.

---

## 🚀 Установка

1. Установите [Fabric Loader](https://fabricmc.net/use/) для Minecraft 1.20.1.
2. Положите файл мода `.jar` вместе с [Fabric API](https://modrinth.com/mod/fabric-api) в папку `.minecraft/mods/`.
3. Запустите игру.

---

## 🛠️ Сборка из исходников

Нужен JDK 17 или новее.

```bash
./gradlew build       # готовый .jar появится в build/libs/
./gradlew runClient   # запустить клиент Minecraft с модом
```

---

## 📜 Лицензия и права (Permissions)

Этот проект распространяется под лицензией **All Rights Reserved** с открытым разрешением для модпаков.

* ✅ **Модпаки (Modpacks):** Вы можете свободно включать этот мод в любые публичные, приватные или серверные модпаки на CurseForge, Modrinth и других платформах.
* ✅ **Баг-репорты и PR:** Сообщения об ошибках и помощь в оптимизации через Pull Requests горячо приветствуются.
* ❌ **Перезалив (Re-hosting):** Запрещено повторно загружать бинарные `.jar` файлы на сторонние сайты (9minecraft и т.д.). Ссылки должны вести на официальные страницы проекта.
* ❌ **Форки и ассеты:** Запрещено создание неофициальных публичных форков и использование кастомных ассетов в коммерческих целях без согласования с автором.
