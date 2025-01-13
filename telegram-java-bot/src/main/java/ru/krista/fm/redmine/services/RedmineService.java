package ru.krista.fm.redmine.services;

import com.taskadapter.redmineapi.Params;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.Issue;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RedmineService {
    public List<Issue> getIssues() throws RedmineException {
        String uri = "http://fmredmine.krista.ru/";
        String apiAccessKey = "a84e0c96ec3e7b9142f1c06dc8e176db72dd31ea";

        RedmineManager mgr = RedmineManagerFactory.createWithApiKey(uri, apiAccessKey);
        mgr.setObjectsPerPage(100);
        Params params = new Params()
                .add("set_filter", "1")
                .add("f[]", "assigned_to_id")
                .add("op[assigned_to_id]", "=")
                .add("v[assigned_to_id][]", "468")
                .add("f[]", "updated_on")
                .add("op[updated_on]", ">=")
                .add("v[updated_on][]", "2024-12-01");

        List<Issue> issues = mgr.getIssueManager().getIssues(params).getResults();
        var t = mgr.getProjectManager().getProjects();
        return issues;
    }
}
