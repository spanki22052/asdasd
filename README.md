# Minecraft Server

Кастомный Minecraft сервер, разработанный на базе Paper API.

## Требования

- Java 17 или выше
- Maven 3.6+

## Структура проекта

```
├── src/
│   └── main/
│       ├── java/com/example/server/  # Исходный код
│       └── resources/                 # Конфигурационные файлы
├── pom.xml                           # Maven конфигурация
└── README.md
```

## Установка

1. Убедитесь, что установлены Java 17+ и Maven
2. Клонируйте репозиторий и перейдите в директорию проекта
3. Выполните команду:

```bash
mvn clean compile
```

## Сборка

Для сборки JAR файла:

```bash
mvn clean package
```

## Запуск

1. Скачайте Paper Server JAR с [papermc.io](https://papermc.io)
2. Создайте директорию сервера и поместите туда JAR файл
3. Запустите сервер:

```bash
java -Xmx1024M -Xms1024M -jar paper.jar nogui
```

4. Поместите собранный плагин (из `target/`) в папку `plugins/`
5. Перезагрузите сервер

## Разработка

### Добавление команд

Создавайте новые классы-обработчики команд, наследуя `CommandExecutor`.

### Добавление событий

Используйте `Listener` интерфейс для обработки событий сервера.

## Документация

- [Paper API Docs](https://papermc.io/docs/paper/)
- [Bukkit Plugin Development](https://bukkit.org/threads/guide-how-to-make-a-bukkit-plugin.33373/)

## Лицензия

MIT
