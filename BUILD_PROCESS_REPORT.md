# Подробный отчет о процессе сборки приложения в AhMyth Server

## Обзор

Процесс сборки APK в серверной части AhMyth представляет собой комплексную систему, которая может работать в двух режимах:
1. **Режим без привязки (Standalone)** - создание чистого payload APK
2. **Режим с привязкой (Bind)** - внедрение payload в существующее приложение

---

## Точка входа: Функция `Build()`

**Расположение:** `AhMyth-Server/app/app/assets/js/controllers/AppCtrl.js:916`

**Параметры:**
- `ip` (string) - IP адрес сервера
- `port` (number) - Порт сервера (по умолчанию: 42474)

**Начальные проверки:**
1. Проверка версии Java (должна быть версия 11)
2. Валидация IP адреса (не должен быть пустым)
3. Установка порта по умолчанию, если не указан

---

## Сценарий 1: Сборка без привязки (bindApk.enable = false)

### Шаг 1: Обновление IP:PORT в исходном коде
**Функция:** `Build()` → строки 937-964

**Процесс:**
1. Чтение файла с IP:PORT из:
   - Путь: `Factory/Ahmyth/smali/ahmyth/mine/king/ahmyth/e.smali`
   - Константа: `CONSTANTS.IOSocketPath`

2. Поиск и замена URL:
   - Ищет строку между `"http://"` и `"?model="`
   - Заменяет на `"http://" + ip + ":" + port`

3. Запись обновленного файла обратно

**Пример замены:**
```javascript
// Было: http://192.168.1.100:42474?model=
// Стало: http://YOUR_IP:YOUR_PORT?model=
```

### Шаг 2: Генерация APK
**Функция:** `GenerateApk(apkFolder)` → строки 192-374

**Параметр:** `apkFolder = CONSTANTS.ahmythApkFolderPath` (путь к `Factory/Ahmyth`)

#### 2.1. Обработка разрешений (если bindApk.enable = false)
**Строки:** 193-323

**Процесс:**
1. Чтение состояния чекбоксов разрешений из UI:
   - Permissions1: Камера
   - Permissions2: Хранилище
   - Permissions3: Микрофон
   - Permissions4: Геолокация
   - Permissions5: Контакты
   - Permissions6: SMS
   - Permissions7: Звонки и логи

2. Формирование массива `selectedPermissions`:
   - Если все чекбоксы отмечены → используются все разрешения из `CONSTANTS.permissions`
   - Если все чекбоксы сняты → также используются все разрешения (по умолчанию)
   - Иначе → только выбранные разрешения

3. Обновление AndroidManifest.xml:
   - Парсинг XML через `xml2js`
   - Очистка существующих `uses-permission` и `uses-feature`
   - Добавление новых разрешений и фич на основе `selectedPermissions`
   - Запись обновленного манифеста

**Особенности:**
- `android.hardware.camera` и `android.hardware.camera.autofocus` добавляются как `uses-feature`
- Остальные разрешения добавляются как `uses-permission`

#### 2.2. Очистка framework директории Apktool
**Строки:** 325-333
```javascript
exec('java -jar "' + CONSTANTS.apktoolJar + '" empty-framework-dir --force')
```
- Очищает кэш framework файлов Apktool
- Ошибки игнорируются (try-catch)

#### 2.3. Сборка APK через Apktool
**Строки:** 335-347
```javascript
java -jar apktool.jar b "apkFolder" -o "outputPath/Ahmyth.apk" --use-aapt2
```

**Параметры:**
- `b` - build (сборка)
- `apkFolder` - путь к декомпилированной структуре APK
- `-o` - выходной файл
- `--use-aapt2` - использование AAPT2 для обработки ресурсов

**Результат:** Неподписанный APK в `~/AhMyth/Output/Ahmyth.apk`

#### 2.4. Подписание APK
**Строки:** 349-358
```javascript
java -jar sign.jar -a "outputPath/Ahmyth.apk"
```

**Процесс:**
- Использует `Factory/sign.jar` для подписания
- Создает подписанный APK: `Ahmyth-aligned-debugSigned.apk`
- Удаляет неподписанный APK

#### 2.5. Восстановление оригинального манифеста
**Строки:** 368-370
```javascript
fs.copyFile(Vault/AndroidManifest.xml, Factory/Ahmyth/AndroidManifest.xml)
```
- Восстанавливает оригинальный манифест из Vault для следующей сборки

**Итоговый файл:** `~/AhMyth/Output/Ahmyth-aligned-debugSigned.apk`

---

## Сценарий 2: Сборка с привязкой (bindApk.enable = true)

### Шаг 1: Выбор APK для привязки
**Функция:** `BrowseApk()` → строки 164-189

**Процесс:**
1. Открытие диалога выбора файла (только .apk)
2. Сохранение пути в `$appCtrl.filePath`

### Шаг 2: Обновление IP:PORT
**Аналогично Сценарию 1, Шаг 1** → строки 976-999

### Шаг 3: Декомпиляция выбранного APK
**Строки:** 1001-1014
```javascript
java -jar apktool.jar d "filePath" -f -o "apkFolder"
```

**Параметры:**
- `d` - decompile (декомпиляция)
- `-f` - force (перезаписать существующую директорию)
- `apkFolder` - путь без расширения .apk (создается временная директория)

**Результат:** Декомпилированная структура APK в `apkFolder`

### Шаг 4: Выбор метода привязки

#### Метод A: Boot Method (bindApk.method = 'BOOT')
**Функция:** `bindOnBoot(apkFolder)` → строки 705-745

**Процесс:**

1. **Чтение AndroidManifest.xml** (строка 709)

2. **Модификация манифеста** через `modifyManifest()` (строка 719):
   - Добавление разрешений на основе чекбоксов
   - Добавление Service: `ahmyth.mine.king.ahmyth.MainService`
   - Добавление Receiver: `ahmyth.mine.king.ahmyth.MyReceiver` с `BOOT_COMPLETED`

3. **Запись обновленного манифеста** (строка 732)

4. **Копирование файлов payload** → `copyAhmythFilesAndGenerateApk()` (строка 741)

#### Метод B: Activity Method (bindApk.method = 'ACTIVITY')
**Функция:** `bindOnActivity(apkFolder)` → строки 748-912

**Процесс:**

1. **Чтение и модификация манифеста** (аналогично Boot Method)

2. **Поиск главной Activity** (строка 803):
   - Функция `getLauncherActivity()` → строки 1106-1173
   - Ищет Activity с `MAIN` action и `LAUNCHER` category
   - Приоритет поиска:
     1. Main Application Class (если не android.app.*)
     2. Main Activity
     3. Activity Alias

3. **Поиск Smali файла** (строка 811):
   - Функция `getLauncherPath()` → строки 1175-1202
   - Использует `readdirp` для рекурсивного поиска файла
   - Ищет файл вида `ClassName.smali`

4. **Внедрение хука в Smali** (строки 820-835):
   ```smali
   # Ищет: return-void
   # Заменяет на:
   invoke-static {}, Lahmyth/mine/king/ahmyth/MainService;->start()V
   return-void
   ```

5. **Модификация SDK версии** (строки 845-901):
   - Обновление `AndroidManifest.xml`:
     - `compileSdkVersion="23"`
     - `platformBuildVersionCode="23"`
   - Обновление `apktool.yml`:
     - `targetSdkVersion: '23'`
     - `minSdkVersion: '19'` (остается)

6. **Копирование файлов payload** → `copyAhmythFilesAndGenerateApk()` (строка 899)

### Шаг 5: Копирование файлов payload
**Функция:** `copyAhmythFilesAndGenerateApk(apkFolder)` → строки 408-485

**Процесс:**

1. **Определение директории для payload** (строка 422):
   - Функция `createPayloadDirectory()` → строки 377-404
   - Анализирует существующие `smali` директории
   - Создает следующую доступную: `smali_classes2`, `smali_classes3`, и т.д.

2. **Копирование Smali файлов** (строка 435):
   ```javascript
   fs.copy(Factory/Ahmyth/smali, apkFolder/payloadSmaliFolder)
   ```

3. **Перемещение android и androidx** (строки 447-476):
   - Копирование `android/` из payload в `apkFolder/smali/android`
   - Копирование `androidx/` из payload в `apkFolder/smali/androidx`
   - Удаление оригинальных директорий из payload папки

4. **Генерация APK** → `GenerateApk(apkFolder)` (строка 479)

### Шаг 6: Генерация финального APK
**Аналогично Сценарию 1, Шаг 2** (но с модифицированным `apkFolder`)

---

## Вспомогательные функции

### `modifyManifest(data, callback)`
**Строки:** 487-703

**Назначение:** Модификация AndroidManifest.xml для добавления payload компонентов

**Процесс:**
1. Парсинг XML через `xml2js`
2. Сбор выбранных разрешений из чекбоксов
3. Фильтрация дубликатов разрешений
4. Добавление `uses-permission` и `uses-feature`
5. Добавление Service и Receiver в `<application>`
6. Сериализация обратно в XML
7. Вызов callback с результатом

**Добавляемые компоненты:**
- **Service:** `ahmyth.mine.king.ahmyth.MainService`
- **Receiver:** `ahmyth.mine.king.ahmyth.MyReceiver` с `BOOT_COMPLETED` intent-filter

### `getLauncherActivity(manifest, apkFolder)`
**Строки:** 1106-1173

**Назначение:** Поиск главной Activity для хука

**Алгоритм поиска:**
1. Проверка Main Application Class (если указан и не android.app.*)
2. Поиск Activity с `MAIN` + `LAUNCHER` intent-filter
3. Поиск Activity Alias с `MAIN` + `LAUNCHER`
4. Возвращает имя класса или -1 при неудаче

### `getLauncherPath(launcherActivity, apkFolder, callback)`
**Строки:** 1175-1202

**Назначение:** Поиск физического пути к Smali файлу

**Процесс:**
- Использует `readdirp` для рекурсивного поиска
- Ищет файл с именем `launcherActivity` (например, `MainActivity.smali`)
- Возвращает относительный путь от `apkFolder`

### `createPayloadDirectory(files)`
**Строки:** 377-404

**Назначение:** Определение директории для payload Smali файлов

**Логика:**
- Игнорирует: `original`, `res`, `build`, `kotlin`, `lib`, `assets`, `META-INF`, `unknown`, `smali_assets`
- Находит все `smali*` директории
- Сортирует численно
- Возвращает следующую доступную: `smali_classes{N+1}`

---

## Константы и пути

**Файл:** `AhMyth-Server/app/app/assets/js/Constants.js`

### Ключевые пути:
- `ahmythApkFolderPath`: `Factory/Ahmyth/` (декомпилированная структура payload)
- `vaultFolderPath`: `Factory/Vault/` (хранилище оригинальных файлов)
- `apktoolJar`: `Factory/apktool.jar`
- `signApkJar`: `Factory/sign.jar`
- `IOSocketPath`: `smali/ahmyth/mine/king/ahmyth/e.smali` (файл с IP:PORT)

### Выходные пути:
- `outputPath`: `~/AhMyth/Output/`
- `logPath`: `~/AhMyth/Logs/`

### Имена файлов:
- `apkName`: `Ahmyth.apk` (неподписанный)
- `signedApkName`: `Ahmyth-aligned-debugSigned.apk` (финальный)

---

## Инструменты

1. **Apktool** (`apktool.jar`)
   - Декомпиляция APK в Smali
   - Сборка APK из декомпилированной структуры
   - Версия: 2.7.0 (судя по выводу)

2. **Sign Tool** (`sign.jar`)
   - Подписание APK debug ключом
   - Выравнивание APK (zipalign)

3. **Java 11**
   - Обязательное требование для работы Apktool и Sign Tool

---

## Поток данных

### Сценарий без привязки:
```
Build() 
  → Обновление IP:PORT в e.smali
  → GenerateApk(Factory/Ahmyth)
    → Обработка разрешений (если нужно)
    → Apktool build
    → Sign tool
    → Восстановление манифеста
  → Готовый APK в Output/
```

### Сценарий с привязкой:
```
Build()
  → Обновление IP:PORT в e.smali
  → Декомпиляция выбранного APK
  → bindOnBoot() или bindOnActivity()
    → modifyManifest()
    → (Activity Method) Внедрение хука в Smali
    → (Activity Method) Обновление SDK версии
    → copyAhmythFilesAndGenerateApk()
      → Создание smali_classesN
      → Копирование payload файлов
      → GenerateApk(apkFolder)
        → Apktool build
        → Sign tool
  → Готовый APK в Output/
```

---

## Обработка ошибок

Все ошибки логируются через `writeErrorLog()` в соответствующие файлы:
- `Parsing.log` - ошибки парсинга XML
- `Reading.log` - ошибки чтения файлов
- `Writing.log` - ошибки записи файлов
- `Building.log` - ошибки сборки APK
- `Signing.log` - ошибки подписания
- `Decompiling.log` - ошибки декомпиляции
- `IP-PORT.log` - ошибки обновления IP:PORT
- `Copying.log` - ошибки копирования файлов
- `Error.log` - прочие ошибки

Все логи сохраняются в `~/AhMyth/Logs/`

---

## Особенности реализации

1. **Асинхронность:** Используются callback'и и промисы для асинхронных операций
2. **Логирование:** Все этапы логируются с задержкой через `delayedLog()`
3. **Восстановление состояния:** После сборки оригинальные файлы восстанавливаются из Vault
4. **SDK версия:** При Activity Method SDK версия принудительно устанавливается в 23
5. **Хук в Smali:** Ищет `return-void` и заменяет на вызов MainService.start()

---

## Зависимости

- **Node.js модули:**
  - `fs-extra` - расширенные файловые операции
  - `xml2js` - парсинг и генерация XML
  - `readdirp` - рекурсивный поиск файлов
  - `child_process` - выполнение системных команд

- **Внешние инструменты:**
  - Java 11
  - Apktool
  - Sign Tool

---

## Заключение

Процесс сборки представляет собой сложную систему модификации Android приложений, которая:
1. Декомпилирует APK в Smali код
2. Внедряет payload компоненты (Service, Receiver)
3. Модифицирует манифест для добавления разрешений
4. Внедряет хуки в код приложения (для Activity Method)
5. Собирает и подписывает финальный APK

Система поддерживает два режима работы и два метода привязки, что обеспечивает гибкость при создании payload.
