package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import model.Loc;
import model.Rezervare;
import model.Spectator;
import model.StareLoc;
import model.validators.ValidationException;
import service.ServiceException;

import java.util.*;

public class MainController extends AbstractController {
    ObservableList<String> modelSpectacole = FXCollections.observableArrayList();
    Set<Loc> seatsToBook = new HashSet<Loc>();
    String currentShowTitle;

    @FXML
    ListView<String> spectacoleListView;

    @FXML
    GridPane seatsConfigurationGrid;

    @FXML
    TextField numeTextField, prenumeTextField, emailTextField;

    @FXML
    public void initialize() {
        spectacoleListView.setItems(modelSpectacole);
    }

    private void updateModelSpectacole() {
        List<String> allSpectacole = new ArrayList<String>();
        service.getAllSpectacole().forEach(spectacol -> allSpectacole
                .add(spectacol.getTitlu() + " | " + spectacol.getData() + " | " + spectacol.getNrLocuriDisponibile() + " locuri dispinibile"));

        modelSpectacole.setAll(allSpectacole);
    }

    public void generateTheatreSeatsConfigurationForShow() {
        List<Button> seatsList = new ArrayList<Button>();
        this.seatsToBook.clear();
        this.currentShowTitle = "";
        Button seatButton;
        String selectedText = spectacoleListView.getSelectionModel().getSelectedItem().strip();
        String[] splitText = selectedText.split("[|]");
        this.currentShowTitle = splitText[0].strip();
        String nrLocuriDisponibileText = splitText[2].replaceAll("[^0-9]", "");
        int nrLocuriDisponibile = Integer.parseInt(nrLocuriDisponibileText);

        int placedAvailableSeats = 0;
        int i = 0;
        List<Loc> locuri = service.getAllLocuri();
        while (i < locuri.size()) {
            boolean addedBookedSeat = false;
            Loc loc = locuri.get(i);
            for (Rezervare rezervare : service.getAllRezervariFromSpectacol(this.currentShowTitle)) {
                if (rezervare.getIdLocRezervat() == loc.getId()) {
                    seatButton = new Button("Nr. " + loc.getNumar() + "|Rand: " + loc.getRand() + "|Loja: " + loc.getLoja() + "\n" +
                            loc.getPret() + " RON" + " | " + StareLoc.REZERVAT);
                    seatButton.setDisable(true);
                    seatsList.add(seatButton);
                    i++;
                    addedBookedSeat = true;
                }
            }
            if (!addedBookedSeat) {
                loc = locuri.get(i);
                if (placedAvailableSeats < nrLocuriDisponibile) {
                    seatButton = new Button("Nr. " + loc.getNumar() + "|Rand: " + loc.getRand() + "|Loja: " + loc.getLoja() + "\n" +
                            loc.getPret() + " RON" + " | " + loc.getStareLoc().toString());
                    seatButton.setDisable(false);

                    Button finalSeatButton = seatButton;
                    Loc finalLoc = loc;
                    seatButton.setOnAction(event -> {
                        this.seatsToBook.add(finalLoc);
                        for (Button btn : seatsList) {
                            if (btn.getText().equals(finalSeatButton.getText()))
                                btn.setStyle("-fx-background-color: green");
                        }
                    });
                    seatsList.add(seatButton);
                    placedAvailableSeats++;
                } else {
                    seatButton = new Button("Nr. " + loc.getNumar() + "|Rand: " + loc.getRand() + "|Loja: " + loc.getLoja() + "\n" +
                            loc.getPret() + " RON" + " | " + StareLoc.INDISPONIBIL);
                    seatButton.setDisable(true);
                    seatsList.add(seatButton);
                }
                i++;
            }
        }

        seatsConfigurationGrid.getChildren().clear();
        int row = 0;
        int col = 0;
        for (i = 0; i < seatsList.size(); i++) {
            seatsConfigurationGrid.add(seatsList.get(i), col, row);
            col++;
            if ((i + 1) % 10 == 0) {
                row++;
                col = 0;
            }
        }
    }

    public void rezervaLoc() {
        String nume = numeTextField.getText();
        String prenume = prenumeTextField.getText();
        String email = emailTextField.getText();

        if (!this.seatsToBook.isEmpty()) {
            try {
                Spectator saved = service.addNewSpectator(nume, prenume, email);
                for (Loc locRezervat : seatsToBook)
                    service.addNewRezervare(saved.getId(), locRezervat.getId(), this.currentShowTitle);
                service.getAllSpectacole().forEach(spectacol -> {
                    if (spectacol.getTitlu().equals(this.currentShowTitle)) {
                        spectacol.setNrLocuriDisponibile(spectacol.getNrLocuriDisponibile() - this.seatsToBook.size());
                        service.modifyExistingSpectacol(spectacol);
                    }
                });
                reset();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success!");
                alert.setHeaderText("Operation was completed successfully!");
                alert.setContentText("Your booking was saved!");
                alert.showAndWait();
            } catch (ValidationException | ServiceException ex) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning!");
                alert.setHeaderText("Operation failed!");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Warning!");
            alert.setHeaderText("Operation failed!");
            alert.setContentText("No seats selected for booking!");
            alert.showAndWait();
        }
    }

    public void init() {
        updateModelSpectacole();
        seatsConfigurationGrid.getChildren().clear();
    }

    public void openLoginWindow() {
        this.application.changeToLogin();
    }

    @Override
    public void reset() {
        numeTextField.clear();
        prenumeTextField.clear();
        emailTextField.clear();
        updateModelSpectacole();
        seatsConfigurationGrid.getChildren().clear();
        this.seatsToBook.clear();
        this.currentShowTitle = "";
    }
}
