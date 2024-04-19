package client;

import client.scenes.AddEditExpenseCtrl;
import client.scenes.AddEditParticipantsCtrl;
import client.scenes.AdminLoginCtrl;
import client.scenes.EventOverviewCtrl;
import client.scenes.ExpenseViewCtrl;
import client.scenes.InvitePageCtrl;
import client.scenes.LoginCtrl;
import client.scenes.MainCtrl;
import client.scenes.ServerUrlCtrl;
import client.scenes.SettingsCtrl;
import client.scenes.SettleDebtsCtrl;
import client.scenes.StartScreenCtrl;
import client.scenes.StatsCtrl;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.google.inject.Guice.createInjector;

public class Main extends Application {

    private static final Injector INJECTOR = createInjector(new MyModule());
    private static final MyFXML FXML = new MyFXML(INJECTOR);

    /**
     * Starts the client
     */
    public static void main(String[] args) throws URISyntaxException, IOException {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        Pair<StartScreenCtrl, Parent> startScreen = FXML.load(
                StartScreenCtrl.class,
                "client", "scenes", "StartScreen.fxml");
        Pair<StatsCtrl, Parent> stats = FXML.load(
                StatsCtrl.class,
                "client", "scenes", "Stats.fxml");
        Pair<EventOverviewCtrl, Parent> eventOverview = FXML.load(
                EventOverviewCtrl.class,
                "client", "scenes", "EventOverview.fxml");
        Pair<ExpenseViewCtrl, Parent> expenseView = FXML.load(
                ExpenseViewCtrl.class,
                "client", "scenes", "ExpenseView.fxml");
        Pair<AddEditExpenseCtrl, Parent> addEditExpense = FXML.load(
                AddEditExpenseCtrl.class,
                "client", "scenes", "AddEditExpense.fxml");
        Pair<SettingsCtrl, Parent> settings = FXML.load(
                SettingsCtrl.class,
                "client", "scenes", "Settings.fxml");
        Pair<InvitePageCtrl, Parent> invitePage = FXML.load(
                InvitePageCtrl.class,
                "client", "scenes", "InvitePage.fxml");
        Pair<AddEditParticipantsCtrl, Parent> addEditParticipants = FXML.load(
                AddEditParticipantsCtrl.class,
                "client", "scenes", "AddEditParticipants.fxml");
        Pair<SettleDebtsCtrl, Parent> settleDebts = FXML.load(
                SettleDebtsCtrl.class,
                "client", "scenes", "settledebts.fxml");

        Pair<LoginCtrl, Parent> loginScreen = FXML.load(
                LoginCtrl.class,
                "client", "scenes", "loginScreen.fxml");

        Pair<AdminLoginCtrl, Parent> adminLoginScreen = FXML.load(
                AdminLoginCtrl.class,
                "client", "scenes", "adminLogin.fxml");

        Pair<ServerUrlCtrl, Parent> serverUrlInp = FXML.load(
                ServerUrlCtrl.class,
                "client", "scenes", "ServerUrlInp.fxml");


        var pc = INJECTOR.getInstance(MainCtrl.class);
        pc.initialize(primaryStage, addEditExpense, addEditParticipants, eventOverview,
                expenseView, invitePage, settings, settleDebts, startScreen, stats, loginScreen, adminLoginScreen, serverUrlInp);
    }

}