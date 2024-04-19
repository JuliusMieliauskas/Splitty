package client;


import client.scenes.AdminLoginCtrl;
import client.scenes.EventOverviewCtrl;
import client.scenes.ExpenseViewCtrl;
import client.scenes.LoginCtrl;
import client.scenes.InvitePageCtrl;
import client.scenes.MainCtrl;
import client.scenes.SettingsCtrl;
import client.scenes.StartScreenCtrl;
import client.scenes.StatsCtrl;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;


public class MyModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(MainCtrl.class).in(Scopes.SINGLETON);
        binder.bind(LoginCtrl.class).in(Scopes.SINGLETON);
        binder.bind(AdminLoginCtrl.class).in(Scopes.SINGLETON);
        binder.bind(StartScreenCtrl.class).in(Scopes.SINGLETON);
        binder.bind(ExpenseViewCtrl.class).in(Scopes.SINGLETON);
        binder.bind(StatsCtrl.class).in(Scopes.SINGLETON);
        binder.bind(InvitePageCtrl.class).in(Scopes.SINGLETON);
        binder.bind(SettingsCtrl.class).in(Scopes.SINGLETON);
        binder.bind(EventOverviewCtrl.class).in(Scopes.SINGLETON);
    }
}