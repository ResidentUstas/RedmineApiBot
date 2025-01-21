package ru.krista.fm.redmine.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.krista.fm.redmine.calendar.CalendarController;
import ru.krista.fm.redmine.calendar.InlineCalendar;
import ru.krista.fm.redmine.calendar.utils.Locale;
import ru.krista.fm.redmine.interfaces.Report;
import ru.krista.fm.redmine.services.ExportService;
import ru.krista.fm.redmine.services.RedmineService;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class TelegramBotFrame extends TelegramLongPollingBot {
    @Autowired
    private RedmineService redmineService;
    private Long userId;
    CalendarController calendarController = new CalendarController(Locale.RU, this);

    public TelegramBotFrame(DefaultBotOptions defaultBotOptions,
                            @Value("${bot.token}") String botToken) {
        super(defaultBotOptions, botToken);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            var firstname = update.getMessage().getFrom().getFirstName();
            switch (messageText) {
                case "/start", "/getreport":
                    try {
                        helloPhraseExe(chatId, firstname);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
                default:
                    try {
                        getReport(chatId, messageText);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
            }
        }

        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();

            if (callbackQuery.getData().contains(InlineCalendar.CONTROL_ALIAS)) {
                calendarController.control(callbackQuery);
            } else {
                if (callbackQuery.getData().contains("get_report")) {
                    try {
                        LocalDate date = LocalDate.parse(callbackQuery.getData().replaceAll("get_report_", "").replaceAll(InlineCalendar.CONTROL_ALIAS, ""));
                        getReportCommandReceived(callbackQuery.getMessage().getChatId(), userId, date);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void getReport(Long chatId, String msg) throws Exception {
        try {
            userId = Long.parseLong(msg);
            sendMessage(chatId, "Отлично, выберите месяц, за который хотите получить отчёт.");
            calendarController.startCalendar(String.valueOf(chatId), "Выберите месяц отчёта");
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    private void getReportCommandReceived(Long chatId, long userId, LocalDate date) throws Exception {
        sendMessage(chatId, "Формирую отчёт..");
        var issuesList = redmineService.getIssues(userId, date);
        ExportService service = new ExportService(redmineService);
        var report = service.generateReport(new Report.ParameterRec[]{new Report.ParameterRec(issuesList, "tasks"),
                new Report.ParameterRec(date, "дата"),
                new Report.ParameterRec(userId, "id")});
        try (FileOutputStream outputStream = new FileOutputStream("C:\\Users\\g.yakovlev\\Documents\\reportRedmineTXT.xlsx")) {
            outputStream.write(report.fileBody.getByteArray());
        }
        File file = new File("C:\\Users\\g.yakovlev\\Documents\\reportRedmineTXT.xlsx");
        InputFile inputFile = new InputFile(file, report.fileName);

        SendDocument sendDocumentRequest = new SendDocument();
        sendDocumentRequest.setChatId(chatId);
        sendDocumentRequest.setDocument(inputFile);
        execute(sendDocumentRequest);
    }

    private void sendMessage(Long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {

        }
    }

    private void helloPhraseExe(Long chatId, String firstname) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(String.format("Привет, %s, рад тебя слышать! скинь свой redmine_id, чтобы получить отчёт", firstname));
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {

        }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    @Override
    public String getBotUsername() {
        return "RedmineReporter";
    }
}
