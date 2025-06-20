public class HomeAssistantManager {

    private static final String DEVICE_JSON =
        "\"device\": {" +
            "\"identifiers\": [\"absoluta_panel\"]," +
            "\"name\": \"Centrale Absoluta\"," +
            "\"manufacturer\": \"Bentel\"," +
            "\"model\": \"Absoluta\"" +
        "}";

    public static String buildPartition(int partitionID, String partitionName) {
        return "{" +
            "\"name\": \"" + partitionName + "\"," +
            "\"state_topic\": \"ABS/partition/" + partitionID + "\"," +
            "\"unique_id\": \"absoluta_partition_" + partitionID + "\"," +
            "\"command_topic\": \"ABS/partition/" + partitionID + "/set\"," +
            "\"code_arm_required\": false," +
            "\"code_disarm_required\": false," +
            "\"supported_features\": [\"arm_away\"]," +
            "\"payload_arm_away\": \"ARM_AWAY\"," +
            "\"payload_disarm\": \"DISARM\"," +
            DEVICE_JSON +
        "}";
    }

    public static String buildSensor(int sensorID, String sensorName) {
        return "{" +
            "\"name\": \"" + sensorName + "\"," +
            "\"state_topic\": \"ABS/sensor/" + sensorID + "\"," +
            "\"unique_id\": \"absoluta_sensor_" + sensorID + "\"," +
            "\"device_class\": \"motion\"," +
            DEVICE_JSON +
        "}";
    }

    public static String buildSensorBypass(int sensorID, String sensorName) {
        return "{" +
            "\"name\": \"" + sensorName + " Bypass\"," +
            "\"state_topic\": \"ABS/sensor/" + sensorID + "_bypass\"," +
            "\"unique_id\": \"absoluta_sensor_" + sensorID + "_bypass\"," +
            "\"command_topic\": \"ABS/sensor/" + sensorID + "/set\"," +
            "\"payload_on\": \"ON\"," +
            "\"payload_off\": \"OFF\"," +
            "\"device_class\": \"switch\"," +
            DEVICE_JSON +
        "}";
    }

    public static String buildMode(char modeChar, String modeLabel) {
        return "{" +
            "\"name\": \"" + modeLabel + "\"," +
            "\"state_topic\": \"ABS/mode/" + modeChar + "\"," +
            "\"unique_id\": \"absoluta_mode_" + modeChar + "\"," +
            "\"command_topic\": \"ABS/mode/" + modeChar + "/set\"," +
            "\"payload_press\": \"MODE_" + modeChar + "\"," +
            DEVICE_JSON +
        "}";
    }

    public static String buildResetErrors() {
        return "{" +
            "\"name\": \"Resetta Guasti e Sabotaggi\"," +
            "\"state_topic\": \"ABS/reset_errors\"," +
            "\"unique_id\": \"absoluta_reset_errors\"," +
            "\"command_topic\": \"ABS/reset_errors/set\"," +
            "\"payload_press\": \"RESET_ERRORS\"," +
            DEVICE_JSON +
        "}";
    }
}