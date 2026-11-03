# Движок плагинов claud code v1
Реализовано: Chaquopy 17.2/Python 3.11 в библиотеке TMessagesProj; AST-метаданные; последовательная очередь Java→Python; импорт .py в приватное хранилище; явное включение; lifecycle; восстановление состояния; JSON-хранилище и формы Header/Switch/Input; выключение при Python-ошибке; события app_started, app_paused, app_resumed и settings_title.
PythonPlugins.java — Android-адаптер, claud_engine.py — движок, base_plugin.py — API, ClaudPluginsActivity.java — экран управления.
Тесты Python: tests/test_plugin_engine.py. Android UI и APK пока не проверены.
Не реализовано: exteraGram SDK, Xposed hooks, SDK отправки сообщений. Плагины exteraGram не объявляются совместимыми.
При импорте код не исполняется; после включения он работает с правами приложения. Изоляции от зависания/падения процесса нет.
