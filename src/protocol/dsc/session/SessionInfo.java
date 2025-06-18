package protocol.dsc.session;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class SessionInfo {
   // Costanti per identificare i tipi di device e cifratura
   public static final int INTERFACE_COMMUNICATOR_MODULE = 2;
   public static final int I_CONTROL = 128;
   public static final int KEYPAD = 129;
   public static final int GENERIC_3RD_PARTY_DEVICE = 143;
   public static final int NO_ENCRYPTION = 0;
   public static final int AES_128_ENCRYPTION = 1;

   // Chiavi per associare info al canale
   private static final AttributeKey<SessionInfo> OWN_INFO = AttributeKey.valueOf("SessionInfo.ownInfo");
   private static final AttributeKey<SessionInfo> PEER_INFO = AttributeKey.valueOf("SessionInfo.peerInfo");

   private Boolean client;
   private Integer deviceTypeOrVendorID;
   private Integer deviceId;
   private String softwareVersion;
   private String protocolVersion;
   private Integer txSize;
   private Integer rxSize;
   private Integer encryptionType;
   private String identifierOrInitKey;
   private String softwareVersionFields;
   private String multiPointCommId;

   public Boolean isClient() {
      return this.client;
   }

   public Boolean getClient() {
      return this.client;
   }

   public void setClient(Boolean client) {
      this.client = client;
   }

   public Integer getDeviceTypeOrVendorID() {
      return this.deviceTypeOrVendorID;
   }

   public void setDeviceTypeOrVendorID(Integer deviceTypeOrVendorID) {
      this.deviceTypeOrVendorID = deviceTypeOrVendorID;
   }

   public Integer getDeviceId() {
      return this.deviceId;
   }

   public void setDeviceId(Integer deviceId) {
      this.deviceId = deviceId;
   }

   public String getSoftwareVersion() {
      return this.softwareVersion;
   }

   public void setSoftwareVersion(String softwareVersion) {
      this.softwareVersion = softwareVersion;
   }

   public String getProtocolVersion() {
      return this.protocolVersion;
   }

   public void setProtocolVersion(String protocolVersion) {
      this.protocolVersion = protocolVersion;
   }

   public Integer getTxSize() {
      return this.txSize;
   }

   public void setTxSize(Integer txSize) {
      this.txSize = txSize;
   }

   public Integer getRxSize() {
      return this.rxSize;
   }

   public void setRxSize(Integer rxSize) {
      this.rxSize = rxSize;
   }

   public Integer getEncryptionType() {
      return this.encryptionType;
   }

   public void setEncryptionType(Integer encryptionType) {
      this.encryptionType = encryptionType;
   }

   public String getIdentifierOrInitKey() {
      return this.identifierOrInitKey;
   }

   public void setIdentifierOrInitKey(String identifierOrInitKey) {
      this.identifierOrInitKey = identifierOrInitKey;
   }

   public String getSoftwareVersionFields() {
      return this.softwareVersionFields;
   }

   public void setSoftwareVersionFields(String softwareVersionFields) {
      this.softwareVersionFields = softwareVersionFields;
   }

   public String getMultiPointCommId() {
      return this.multiPointCommId;
   }

   public void setMultiPointCommId(String multiPointCommId) {
      this.multiPointCommId = multiPointCommId;
   }

   // Inizializza le info di sessione associate al canale
   public static void initInfo(Channel channel, SessionInfo ownInfo) {
      if (ownInfo.getClient() == null) {
         throw new IllegalArgumentException("invalid own info (null client)");
      } else if (channel.attr(OWN_INFO).setIfAbsent(ownInfo) != null) {
         throw new IllegalStateException("info already initialized");
      } else {
         // Crea info peer con ruolo opposto (client/server)
         SessionInfo peerInfo = new SessionInfo();
         peerInfo.setClient(!ownInfo.getClient());
         channel.attr(PEER_INFO).set(peerInfo);
      }
   }

   public static SessionInfo getOwnInfo(Channel channel) {
      return getInfo(OWN_INFO, channel);
   }

   public static SessionInfo getPeerInfo(Channel channel) {
      return getInfo(PEER_INFO, channel);
   }

   // Recupera info dal canale, lancia eccezione se non inizializzate
   private static SessionInfo getInfo(AttributeKey<SessionInfo> key, Channel channel) {
      SessionInfo info = channel.attr(key).get();
      if (info != null) {
         return info;
      } else {
         throw new IllegalStateException("info not initialized");
      }
   }
}
