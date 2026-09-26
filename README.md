# ToughSurvival

Плагин выживания для Paper/Spigot 1.20 на сервере **VortexiaPolit** — система температуры тела и жажды (аналог [Tough As Nails](https://www.curseforge.com/minecraft/mc-mods/tough-as-nails)), с кастомными предметами (грелки, очищенная/дождевая вода, Книга Равновесия) и собственным ресурспаком для иконок/анимаций.

## Команда

`/tan [status|settemp|setthirst|reload|help]` (алиасы: `/toughsurvival`, `/survival`). Административные подкоманды требуют право `tan.admin` (по умолчанию только ОП).

## Механика

- Температура зависит от биома, глубины (Y), времени суток и погоды.
- Жажда снижается со временем, восполняется питьём воды.
- Гипо-/гипертермия дают дебаффы и урон при выходе за пределы шкалы.

Подробности по предметам, крафтам и шкале — см. `plugins/ShopLite-commands.txt` (раздел TOUGHSURVIVAL) на боевом сервере.

## Сборка

- `build.bat` — компиляция плагина.
- `build-rp.bat` / `build-resourcepack.ps1` — сборка ресурспака с иконками.

## Зависимости

- Опционально: **ItemsAdder** (кастомные предметы/текстуры).

## Авторство

Код и текстуры ресурспака (`heater_1–3.png`, `thermos.png`, `survival_icons.png`) — **PowerUSSR**. Текстуры созданы с помощью генерации изображений.

## Совместимость

- Minecraft **1.20.1**
- Ядро сервера: **Mohist 1.20.1** (плагин используется на сервере VortexiaPolit)
- Написан на Bukkit/Spigot API, поэтому может работать и на Paper/Spigot 1.20.x

## Сообщество

Discord сервера VortexiaPolit: https://discord.gg/3svAGgVtz

## Лицензия

MIT — см. [LICENSE](LICENSE).
