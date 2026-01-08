import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

final class Entities {
   final Map<Integer, Partition> partitionsById = new HashMap<>();
   final Map<String, Integer> partitionIdByTopic = new HashMap<>();

   final Map<Integer, Sensor> sensorsById = new HashMap<>();
   final Map<String, Integer> sensorIdByTopic = new HashMap<>();

   final String[] modeNames = new String[4];

   final HashSet<Integer> sensorDiscoverySent = new HashSet<>();
   final HashSet<Integer> partitionDiscoverySent = new HashSet<>();
   final HashSet<Integer> modeDiscoverySent = new HashSet<>();

   static final class Partition {
      String name;
      String topic;
      String arming;
      String status;
   }

   static final class Sensor {
      String name;
      String topic;
      String status;
      String bypass;
   }
}
