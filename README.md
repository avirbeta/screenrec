# Screen Recorder — запись экрана Minecraft 1.16.5 (Fabric)

Записывает **весь экран Minecraft** — мир, HUD, чат, инвентарь и меню — в MP4 через FFmpeg. Нажми **R** — запись пошла, снова R — стоп.

## Что нового в 1.1.0

- Исправлена гонка при старте: `recording` выставлялся после запуска pipe-потока, из-за чего запись могла не начаться.
- Захват перенесён из `WorldRenderer.render` в `GameRenderer.render`: теперь в видео попадают GUI, чат и HUD, а не только 3D-мир.
- FFmpeg ищется без команды `which` (её нет на Windows), с поддержкой `FFMPEG_PATH`.
- Буферы кадров переиспользуются — меньше нагрузки на GC при 60 FPS.
- Аккуратная остановка FFmpeg: ожидание завершения и закрытия файла.
- В чат выводится длительность записи.
- Добавлены CI-сборка и MIT-лицензия.

## Установка

1. Установи [Fabric Loader](https://fabricmc.net/) для Minecraft 1.16.5 и Fabric API `0.42.0+1.16`.
2. Установи FFmpeg:
   - **Linux:** `sudo apt install ffmpeg`
   - **Windows:** скачай с [ffmpeg.org](https://ffmpeg.org/download.html) и добавь в PATH
   - **macOS:** `brew install ffmpeg`
   - либо задай переменную `FFMPEG_PATH`
3. Положи `screenrec-fabric-1.16.5-1.1.0.jar` в `mods/`.
4. Запусти игру.

## Использование

| Клавиша | Действие |
|---------|----------|
| **R** | Начать / остановить запись |

Файлы сохраняются в `.minecraft/recordings/recording_ГГГГ-ММ-ДД_ЧЧ-ММ-СС.mp4`.

Запись идёт в 60 FPS, H.264, без звука: `-preset ultrafast`, `-crf 23`.

## Сборка

Требуется JDK 17, Gradle 8.10 (wrapper в комплекте), Fabric Loom 1.8.13, yarn `1.16.5+build.10:v2`.

```bash
./gradlew build
```

Готовый мод: `build/libs/screenrec-fabric-1.16.5-1.1.0.jar`.

CI на GitHub собирает jar на каждый push.

## Лицензия

MIT — см. [LICENSE](LICENSE).
