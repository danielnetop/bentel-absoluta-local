package protocol.dsc.transport;

// Enum per segnali/eventi semplici usati nella pipeline Netty
public enum SimpleMessage {
   COMMAND_RECEIVED,
   CLOSING_CHANNEL_EVENT,
   HANDSHAKE_BEGIN_EVENT,
   HANDSHAKE_END_EVENT,
   HANDSHAKE_STAGE_COMPLETED_EVENT;
}
