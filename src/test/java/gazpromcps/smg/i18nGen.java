package gazpromcps.smg;

import java.util.Arrays;
import java.util.stream.Collectors;

public class i18nGen {
    private static final String VARIABLES_ENGLISH = """
            description=This bot is for tracking daily activity. For help use /help
            description-start=starting the bot
            description-register=[registration token] registration with a token (usage example: `/register A0123456789`)
            description-make-report=new report
            description-update-report=update one of 5 last committed reports
            description-export-xlsx=provides `xlsx`-file with all tracked data
            description-export-csv=provides `csv`-file with all tracked data
            description-export-text=shows all tracked data in a message
            description-export-media=provides all the media that has been uploaded for all jobs in a `zip`-archive
            description-import-xlsx=upload new job list
            description-promote=[user id] promote to a specific role (usage example: `/promote 123`)
            description-users=list  all the users
            description-token=generate registration token
            description-en=switch to english
            description-ru=switch to russian
            description-lang=[country code] change the language to any other https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2 (usage example: `/lang ja`)
            description-help=use for help
            
            button-start=Start
            button-register=Register
            button-make-report=Choose organization
            button-update-report=update report
            button-export-xlsx=Export xlsx
            button-export-csv=Export csv
            button-export-text=Export in a text message
            button-export-media=Export media
            button-import-xlsx=Import new job list
            button-promote=Promote
            button-users=Users
            button-token=Token
            button-reload=Reload
            button-en=English
            button-ru=Russian
            button-lang=Different language
            button-help=Help
            
            start=Hello! You are to report your daily activity through this bot. To get started, you need to register. Use /register or the button bellow
            await-register=Wait until the manager will accept your request
            register-accepted=Your request was accepted
            register-declined=Your request was declined
            end-registration=Click the button bellow to proceed
            help=To make new report you should use /makereport or the "Choose organization" button, then peek the right one of sections and print the number you want
            new-lang=Request to `%s` is accepted. Please, wait a minute
            
            select-type=Select the right one
            back=Back
            all-jobs=All jobs
            work-type=Work type is
            print-count=Write the amount
            update-report=It is possible to update only up to 5 last reports. Choose the one you want to change:
            decide-load-media=Do you want to attach media to your report?
            yes=Yes
            no=No
            load-media=Upload one or several pictures
            accepted=Successfully accepted
            choose-period=Choose the period for which you want to receive the report
            day=day
            week=week
            month=month
            year=year
            total=total
            provide-the-file=Provide the file with the job list
            processing=Processing...
            decide-choose-master=Do you want to add full job list? (or do you want to select the only master). Add all?
            done=Done
            decide-accept-user=Accept this user?%nfirst name: `%s`%nlast name: `%s`%nusername: `%s`%n
            accept-user=Accept
            decline-user=Decline
            
            excel-other=Other
            excel-sheet-name=Report
            excel-total=Total by each work type
            
            token=Your registration token. Only having it, new users can contact you for registration
            decide-promote-user=Choose the right role for the user
            promoted=You have been promoted. Use /reload to update your interface
            successful-promotion=`%s` was successfully promoted to `%s`
            
            unknown-error=Unknown error occurred. Make a screenshot of your chat and report to the headmaster
            unknown-command=Unknown command. Check /help. All available commands are listed there
            not-allowed=This action has restricted access. You are not allowed
            unwelcome=Please do not use this command
            invalid-token=Your token is invalid. Try again
            invalid-request-sequence=You have just made an invalid sequence of requests. Please, try again from the start
            invalid-command-usage=Invalid usage of the command. Read /help to find out
            invalid-count=Invalid count. Number of completed jobs should be strictly greater than 0
            invalid-input-format=Invalid input format
            invalid-number=Input number is not valid
            job-not-found=Job is not found. Some description is invalid
            unsupported-file-extension=Unsupported file extension
            """.trim();

    private static final String VARIABLES_RUSSIAN = """
            description=Этот бот предназначен для ведения ежедневных отчетов. Для справки используйте /help
            description-start=стартует бота
            description-register=[регистрационный токен] регистрирует на основе токена (пример исплользования: `/register A0123456789`)
            description-make-report=новый отчет
            description-update-report=изменить один из 5 последних отправленных отчетов
            description-export-xlsx=предоставляет `xlsx`-файл со всеми сохраненными отчетами за период
            description-export-csv=предоставляет `csv`-файл со всеми сохраненными отчетами за период
            description-export-text=выводит все сохраненные отчеты за период в виде текстового сообщения
            description-export-media=предоставляет все загруженные медиа в виде `zip`-архива
            description-import=загрузка нового списка работ
            description-promote=[id пользователя] изменить роль пользователя (пример использования: `/promote 123`)
            description-users=выводит список всех пользователей
            description-token=генерирует регистрационный токен
            description-en=переключение на английский
            description-ru=переключение на русский
            description-lang=[код страны] изменяет язык на любой из представленных здесь https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2 (пример использования: `/lang ja`)
            description-help=помощь
            
            button-start=Старт
            button-register=Регистрация
            button-make-report=Выбор подрядной организации
            button-update-report=Изменить отчет
            button-export-xlsx=Экспортировать в xlsx
            button-export-csv=Экспортировать в csv
            button-export-text=Выгрузить текстовым сообщением
            button-export-media=Посмотреть загруженные фотографии
            button-import-xlsx=Загрузить новый список работ
            button-promote=Изменить роль
            button-users=Пользователи
            button-token=Сгенерировать токен
            button-reload=Перезагрузить
            button-ru=Русский
            button-en=Английский
            button-lang=Другой язык
            button-help=Помощь
            
            start=Здравствуйте! С моей помощью, вы сможете составлять ежедневный отчеты. Чтобы начать, вам потребуется зарегистрироваться. Для этого используйте регистрационный токен и команду /register
            await-register=Подождите, пока менеджер примет ваш запрос
            register-accepted=Ваш запрос был принят
            register-declined=Ваш запрос был отклонен
            end-registration=Для завершения регистрации, нажмите кнопку ниже
            help=Для занесения нового отчета напишите /makereport или нажмите на кнопку "Выбор подрядной организации", затем необходимо выбрать нужные варианты и после ввести количество
            new-lang=Запрос на `%s` принят. Пожалуйста, подождите немного
            
            select-type=Выберете нужный вариант
            back=Назад
            all-jobs=Все работы
            work-type=Выбранная работа
            print-count=Введите объем
            update-report=Возможно изменить только последние 5 отчетов. Выберете нужный:
            decide-load-media=Хотите прикрепить фотографию к отчету?
            yes=Да
            no=Нет
            load-media=Загрузите одну или несколько фотографий
            accepted=Успешно принято
            choose-period=Выберите период, за который хотите получить отчет
            day=день
            week=неделя
            month=месяц
            year=год
            total=весь период
            provide-the-file=Предоставьте файл со списком всех работ
            processing=Обрабатывается...
            decide-choose-master=Вы хотите добавить все работы из списка? (или только кокнкретного подрядчика). Добавить все?
            done=Готово
            decide-accept-user=Зарегистрировать этого пользователя?%nИмя:`%s`%nФамилия: `%s`%nЛогин телеграма: `%s`%n
            accept-user=Принять
            decline-user=Отклонить
            
            excel-other=Другие
            excel-sheet-name=Отчет
            excel-total=Сумма по всем видам работ
            
            token=Ваш регистрационный токен. Только с его помощью вас смогут найти новые пользователи
            decide-promote-user=Выберете новую роль для пользователя
            promoted=Ваша роль изменилась. Используйте команду /reload, чтобы обновить ваш интерфейс
            successful-promotion=Роль пользователя `%s` была успешно изменена на `%s`
            
            unknown-error=Возникла неизвестная ошибка. Сделайте скриншот вашего чата и передайте его руководителю
            unknown-command=Неизвестная команда. Проверьте /help. Все доступные команды перечислены там
            not-allowed=Это действие имеет защищенный доступ. Вы не можете его выполнить
            unwelcome=Пожалуйста, не используйте эту команду
            invalid-token=Ваш токен неправильный. Проверьте, пожалуйста, и попробуйте снова
            invalid-request-sequence=Последовательность Ваших действий является запрещенной. Пожалуста, повоторите попытку с самого начала
            invalid-command-usage=Некорректное использование команды. Проверьте в /help
            invalid-count=Некорректное количество: дожно быть строго положительное
            invalid-input-format=Некорректный формат вывода
            invalid-number=Введенное число неверно
            job-not-found=Работа не найдена. Какое-то описание некорректно. Проверьте, пожалуйста
            unsupported-file-extension=Неподдерживаемый формат файла
            """.trim();

    private static final String NEW = """
            decide-choose-master=Do you want to add full job list? (or do you want to select the only master). Add all?
            
            decide-choose-master=Вы хотите добавить все работы из списка? (или только кокнкретного подрядчика). Добавить все?
            """.trim();


    public static void main(String[] args) {
        System.out.println(Arrays.stream(VARIABLES_RUSSIAN.split("\\n")).map(line -> {
            final String[] env = line.split("\\s*=\\s*");
            return env.length < 2 ? line :
                    env[0] + "=" + env[1].chars().mapToObj(c -> String.format("\\u%04x", c)).collect(Collectors.joining(""));
        }).collect(Collectors.joining("\n")));
    }
}
