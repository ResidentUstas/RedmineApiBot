package ru.krista.fm.redmine.services;

import com.taskadapter.redmineapi.*;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.Journal;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class RedmineService {
    private final RedmineManager manager;

    public RedmineService(RedmineManager manager) {
        this.manager = manager;
    }

    public List<Issue> getIssues(long userId, LocalDate date) throws RedmineException {
        Params params = new Params()
                .add("set_filter", "1")
                .add("f[]", "assigned_to_id")
                .add("op[assigned_to_id]", "=")
                .add("v[assigned_to_id][]", String.valueOf(userId))
                .add("f[]", "updated_on")
                .add("op[updated_on]", ">=")
                .add("v[updated_on][]", getFormatDate(date, false))
                .add("f[]", "created_on")
                .add("op[created_on]", "<=")
                .add("v[created_on][]", getFormatDate(date, true));

        List<Issue> issues = manager.getIssueManager().getIssues(params).getResults();
        return issues;
    }

    public List<Journal> getIssueJournal(int issueId) throws RedmineException {
        var myissue = manager.getIssueManager().getIssueById(issueId, Include.journals);
        return myissue.getJournals().stream().filter(x -> !x.getDetails().isEmpty()).toList();
    }

    private String getFormatDate(LocalDate reportDate, boolean isEnd) {
        LocalDate startOfMonth = YearMonth.from(reportDate).atDay(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", new Locale("ru"));
        var strStartOfMonth = startOfMonth.format(formatter);
        var strEndOfMonth = endOfMonth.format(formatter);

        return isEnd ? strEndOfMonth : strStartOfMonth;
    }
}
