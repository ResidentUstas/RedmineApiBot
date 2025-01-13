package ru.krista.fm.redmine.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.krista.fm.redmine.interfaces.Report;
import ru.krista.fm.redmine.services.ExportService;
import ru.krista.fm.redmine.services.RedmineService;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

@Component
public class TelegramBotFrame extends TelegramLongPollingBot {
    @Autowired
    private RedmineService redmineService;
    private final ExportService exportService;

    public TelegramBotFrame(DefaultBotOptions defaultBotOptions,
                            @Value("${bot.token}") String botToken,
                            ExportService exportService) {
        super(defaultBotOptions, botToken);
        this.exportService = exportService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start", "/getreport":
                    try {
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;
            }
        }
    }

    private void startCommandReceived(Long chatId, String name) throws Exception {
        var issuesList = redmineService.getIssues();
        ExportService service = new ExportService();
        var report = service.generateReport(new Report.ParameterRec[]{new Report.ParameterRec(issuesList, "tasks")});
       // var report = exportService.generateReport(new Report.ParameterRec[]{new Report.ParameterRec(issuesList, "tasks")});
        try (FileOutputStream outputStream = new FileOutputStream("C:\\Users\\g.yakovlev\\Documents\\reportRedmineTXT.xlsx")) {
            outputStream.write(report.fileBody.getByteArray());
        }
        File file = new File("C:\\Users\\g.yakovlev\\Documents\\reportRedmineTXT.xlsx");
        InputFile inputFile = new InputFile(file, name + "_" + report.fileName);

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

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    @Override
    public String getBotUsername() {
        return "RedmineReporter";
    }
}
