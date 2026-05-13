module com.aclangtonant {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.aclangtonant to javafx.fxml;
    exports com.aclangtonant;
}
