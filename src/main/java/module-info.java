module com.pbl4.monitor {
    requires javafx.controls;

    exports com.pbl4.monitor;

    requires javafx.graphicsEmpty;
    requires java.logging;
    requires java.management;
    requires jdk.management;

    opens com.pbl4.models to javafx.base;
}
