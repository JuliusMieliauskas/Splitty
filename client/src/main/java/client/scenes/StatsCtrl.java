package client.scenes;


import client.LanguageController;
import client.utils.ConfigUtils;
import client.utils.EventUtils;
import client.utils.TagColorManagerUtils;
import com.google.inject.Inject;
import commons.Event;
import commons.Expense;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;

import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.fxml.Initializable;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.control.Button;

import java.net.URL;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class StatsCtrl implements Initializable {

    @FXML
    private PieChart pieChart;
    @FXML
    private Button returnButton;
    @FXML
    private Label cost;
    @FXML
    private Label totalCostLabel;
    @FXML
    private Label clickOnSliceLabel;
    @FXML
    private VBox chartLegend;
    private final MainCtrl mainCtrl;
    private Event event;
    private final Map<PieChart.Data, Tooltip> sliceTooltips = new HashMap<>();
    private final DecimalFormat numberFormat = new DecimalFormat("#.00");
    private Double totalCost;
    private Map<String, String> tagColors = new HashMap<>();

    @Inject
    public StatsCtrl(MainCtrl mainCtrl) {
        this.mainCtrl = mainCtrl;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mainCtrl.setButtonIcon(returnButton, "back");

        pieChart.setVisible(false);
        addShortcuts();
    }



    private void fillPieChart(Long eventId) {
        pieChart.getData().clear();

        Set<Expense> expenses = new HashSet<>(EventUtils.getExpensesOfEvent(eventId));
        HashMap<String, Double> tagAndExpense = tagExpense(expenses);
        tagAndExpense.forEach((tag, amount) -> {
            PieChart.Data slice = new PieChart.Data(translateTag(tag), amount);
            pieChart.getData().add(slice);
            String color = null;
            try {
                color = tagColors.get(tag);
            } catch (Exception e) {
                mainCtrl.showAlert("Slice doesn't have assigned color. It will be grey.", Alert.AlertType.INFORMATION);
            }
            if (color == null) {
                color = "gray";
            }
            colorSlice(slice, color);
        });
        totalCost = (tagAndExpense.values().stream().mapToDouble(Double::doubleValue).sum());
        cost.setText(numberFormat.format(totalCost * ConfigUtils.getExchangeRate()) + " " + ConfigUtils.getCurrency());


        pieChart.getData().forEach(data ->
                data.nameProperty().bind(
                        Bindings.concat(
                                data.getName(), ": ", numberFormat.format(
                                        data.pieValueProperty().getValue() * ConfigUtils.getExchangeRate()), " ", ConfigUtils.getCurrency(),
                                " (", numberFormat.format(data.pieValueProperty().getValue() * 100 / totalCost), "%)")));
        pieChart.setLabelsVisible(false);
        pieChart.setLegendVisible(false);
        createCustomLegend(tagAndExpense);
        pieChart.setVisible(true);

    }

    private String translateTag(String tag) {
        String displayName = tag;
        if ("food".equals(tag) || "travel".equals(tag) || "entrance_fees".equals(tag)) {
            displayName = LanguageController.getInstance().getString(tag);
        }
        return displayName;
    }


    private HashMap<String, Double> tagExpense(Set<Expense> expenses) {
        HashMap<String, Double> tagAndExpense = new HashMap<>();
        for (Expense expense : expenses) {
            String tag = expense.getTag();
            if (tag != null) {
                tagAndExpense.put(tag, tagAndExpense.getOrDefault(tag, 0.0) + expense.getAmount());
            } else {
                tagAndExpense.put(LanguageController.getInstance().getString("other"),
                        tagAndExpense.getOrDefault(LanguageController.getInstance().getString("other"), 0.0) + expense.getAmount());
            }
        }
        return tagAndExpense;
    }


    private void colorSlice(PieChart.Data data, String color) {
        Node slice = data.getNode();
        slice.setStyle("-fx-pie-color: " + color.toLowerCase() + ";");
    }

    /**
     * Sets the event for which the pie chart will be shown
     *
     * @param event current event
     */
    public void setEvent(Event event) {
        tagColors = TagColorManagerUtils.readTagColorsFromFile(event.getId());
        fillPieChart(event.getId());
        addEventHandlersToSlices();

        Platform.runLater(this::updateLanguage);
    }

    private void handleSliceClick(MouseEvent event) {
        Node node = (Node) event.getSource();
        PieChart.Data data = (PieChart.Data) node.getUserData();
        if (sliceTooltips.containsKey(data)) {
            Tooltip currentTooltip = sliceTooltips.get(data);
            currentTooltip.hide();
            sliceTooltips.remove(data);
        } else {
            Tooltip newTooltip = new Tooltip(data.getName());
            newTooltip.show(node, event.getScreenX(), event.getScreenY() - 10);
            sliceTooltips.put(data, newTooltip);
        }
    }

    /**
     * Adds event handlers (mouse click) to the slices.
     */
    public void addEventHandlersToSlices() {
        pieChart.getData().forEach(slice -> {
            slice.getNode().addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleSliceClick);
            slice.getNode().setUserData(slice);
        });
    }

    public void returnToEventOverview() {
        System.out.println("Changed to Event page");
        mainCtrl.showEventOverview();
    }

    private void createCustomLegend(HashMap<String, Double> tagAndExpense) {
        chartLegend.getChildren().clear();
        tagAndExpense.forEach((tag, amount) -> {
            String color = null;
            try {
                color = tagColors.get(tag);
            } catch (Exception e) {
                System.out.println("Legend color will be grey");
            }
            if (color == null) {
                color = "gray";
            }
            HBox legendItem = new HBox(10);
            legendItem.setAlignment(Pos.CENTER_LEFT);
            Circle colorCircle = new Circle(8);
            colorCircle.setFill(Color.web(color));
            Label label = null;
            label = new Label(translateTag(tag) + ": " +
                    numberFormat.format(amount * ConfigUtils.getExchangeRate()) + " " + ConfigUtils.getCurrency() +
                    " (" + numberFormat.format(amount * 100 / totalCost) + "%)"); //this is fine because here both are USD

            legendItem.getChildren().addAll(colorCircle, label);
            chartLegend.getChildren().add(legendItem);
        });
    }

    /**
     * Updates the language of the scene.
     */
    public void updateLanguage() {
        totalCostLabel.setText(LanguageController.getInstance().getString("Total cost"));
        clickOnSliceLabel.setText(LanguageController.getInstance().getString("Click on slice to see values"));
        returnButton.setText(LanguageController.getInstance().getString("return"));
    }
    private void addShortcuts() {
        Platform.runLater(() -> {
            Scene scene = returnButton.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        returnToEventOverview();
                        event.consume();
                    }
                });
            }
        });
    }
}
