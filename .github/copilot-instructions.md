# Minecraft Server - Инструкции для Copilot

Этот проект представляет собой кастомный Minecraft сервер, разработанный на базе Paper API с использованием Java и Maven.

## Информация о проекте

- **Язык:** Java 17
- **Build System:** Maven
- **API:** Paper/Bukkit
- **Основной пакет:** com.example.server

## Структура

- `src/main/java/` - Исходный код Java
- `src/main/resources/` - Конфигурационные файлы (plugin.yml и т.д.)
- `pom.xml` - Maven конфигурация зависимостей

## Общие задачи

### Компиляция проекта
```bash
mvn clean compile
```

### Сборка JAR файла
```bash
mvn clean package
```

### Добавление новых команд

1. Создайте класс, наследующий `CommandExecutor`
2. Зарегистрируйте команду в `plugin.yml`
3. Зарегистрируйте в `MinecraftPlugin.onEnable()`

### Добавление событий

1. Создайте класс, наследующий `Listener`
2. Аннотируйте методы с `@EventHandler`
3. Зарегистрируйте в `MinecraftPlugin.onEnable()` через `Bukkit.getPluginManager().registerEvents()`

## Зависимости

Основные зависимости в `pom.xml`:
- **Paper API** - основной API для разработки плагинов
