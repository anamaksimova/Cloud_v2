package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WorkWindowController {



    @FXML
    public TextField textField;

    private ClientController controller;
    private Stage workStage;
    public static Path path = null;

    public  void setController(ClientController controller){
        this.controller=controller;
    }


    public void uploadFile(ActionEvent actionEvent) throws IOException {
        FileChooser fileChooser = new FileChooser();

        File selectedFile = fileChooser.showOpenDialog(workStage);
        Path path= Paths.get(selectedFile.getAbsolutePath());
        System.out.println(path);
        //todo controller.sendFile();

    }


    //todo переделать
    public void downloadFile(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();

        File selectedFile = fileChooser.showOpenDialog(workStage);
        fileChooser.setInitialDirectory(new File("C:\\тест"));
        Path path= Paths.get(selectedFile.getAbsolutePath());
        System.out.println(path);


    }

}
