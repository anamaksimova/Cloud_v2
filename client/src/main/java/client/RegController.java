package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegController {
    @FXML
    public TextField loginFieldReg;
    @FXML
    public PasswordField passwordFieldReg;

    @FXML
    public PasswordField passwordFieldCheck;
    @FXML
    public Label label;
    private ClientController controller;
    public  void setController(ClientController controller){
        this.controller=controller;
    }


    public void tryToReg(ActionEvent actionEvent) {
        String login = loginFieldReg.getText().trim();
        String password = passwordFieldReg.getText().trim();
        String passwordCheck =passwordFieldCheck.getText().trim();
        if (login.length() * password.length() * passwordCheck.length() == 0) {
            return;
        }

        if (password.equals(passwordCheck)) {

            //todo controller.registration(login, password);
            controller.closeRegWindow();
            loginFieldReg.clear();
            passwordFieldReg.clear();
            passwordFieldCheck.clear();


        } else {
            label.setText("Пароли не совпадают");
        }

    }



}

