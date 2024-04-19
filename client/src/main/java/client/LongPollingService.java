package client;

import client.scenes.ExpenseViewCtrl;
import commons.UserExpense;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

public class LongPollingService extends ScheduledService<Void> {
    private final ExpenseViewCtrl expenseViewCtrl;
    private final String urlBase;
    private String url;
    private boolean isOn;
    public LongPollingService(String url, ExpenseViewCtrl expenseViewCtrl) {
        super();
        this.expenseViewCtrl = expenseViewCtrl;
        this.urlBase = (url.endsWith("/") ? url.substring(0,url.length() - 1) : url);
    }

    public void link(Long expenseId) {
        url = urlBase + "/api/expenses/" + expenseId + "/debtors/long-poll";
        start();
        isOn = true;
    }

    public void unlink() {
        isOn = false;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                RestTemplate restTemplate = new RestTemplate();
                var res = restTemplate.exchange(url, HttpMethod.GET, null,
                        new ParameterizedTypeReference<Set<UserExpense>>() {
                        }).getBody();
                if (isOn) {
                    expenseViewCtrl.updateUserExpenses(res);
                }
                return null;
            }
        };
    }
}