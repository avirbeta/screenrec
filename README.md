# Screen Recorder — Запись экрана Minecraft 1.16.5 (Fabric)

Записывает весь экран Minecraft в MP4 видео через FFmpeg. Нажми R — запись пошла, снова R — стоп.

## Установка

1. Установи [Fabric Loader](https://fabricmc.net/) для Minecraft 1.16.5
2. Установи FFmpeg:
   - **Linux**: `sudo apt install ffmpeg`
   - **Windows**: скачай с [ffmpeg.org](https://ffmpeg.org/download.html) и добавь в PATH
   - **Mac**: `brew install ffmpeg`
3. Положи `screenrec-fabric-1.16.5-1.0.0.jar` и [Fabric API](https://modrinth.com/mod/fabric-api) в `mods/`
4. Запусти Minecraft

## Использование

| Клавиша | Действие |
|---------|----------|
| **R** | Начать / остановить запись |

При старте записи в чате появится зелёное сообщение `Запись начата (R для стопа)`.
При остановке — красное `Остановлено` с длительностью.

## Что записывается

- Полный экран Minecraft — интерфейс, хотбар, чат,ולם, HUD — всё
- 60 FPS
- H.264 видео без звука

## Где файлы

```
.minecraft/recordings/
├── recording_2026-08-26_15-30-00.mp4
├── recording_2026-08-26_16-05-42.mp4
└── ...
```

Имя файла: `recording_ГГГГ-ММ-ДД_ЧЧ-ММ-СС.mp4`

## Настройка качества

По умолчанию: CRF 23, ultrafast preset. Чтобы изменить — собери из исходников и отредактируй `Recorder.java`:

| Параметр | Значение | Описание |
|----------|----------|----------|
| `-crf` | 23 | Качество (0-51, меньше = лучше) |
| `-preset` | ultrafast | Скорость кодирования |
| `-r` | 60 | FPS |

Рекомендуемые пресеты:
- **ultrafast** — минимум нагрузки, файлы больше
- **fast** — баланс
- **medium** — лучше качество, больше нагрузка

## Требования

- Minecraft 1.16.5
- Fabric Loader 0.11+
- Fabric API 0.42.0+1.16
- Java 8+
- **FFmpeg** (обязательно!)

## Сборка из исходников

```bash
git clone https://github.com/YOUR_NAME/screenrec.git
cd screenrec
./gradlew build
# jar будет в build/libs/
```

## Лицензия

VizorVerze
