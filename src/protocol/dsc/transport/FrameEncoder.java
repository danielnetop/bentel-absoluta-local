package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.MessageToByteEncoder;

import protocol.dsc.session.SessionInfo;

@Sharable
public class FrameEncoder extends MessageToByteEncoder<ByteBuf> {
   protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
      // Scrive l'ID multipunto se presente
      String commId = SessionInfo.getOwnInfo(ctx.channel()).getMultiPointCommId();
      if (commId != null && !commId.isEmpty()) {
         out.writeBytes(commId.getBytes(FrameHelper.MULTI_POINT_COMM_ID_CHARSET));
      }

      out.writeByte(FrameHelper.START_OF_FRAME);

      // Escaping dei byte speciali nel payload
      while (in.isReadable()) {
         byte b = in.readByte();
         switch (b) {
               case FrameHelper.ESCAPE:
               case FrameHelper.START_OF_FRAME:
               case FrameHelper.END_OF_FRAME:
                  out.writeByte(FrameHelper.ESCAPE);
                  out.writeByte(FrameHelper.ESCAPE_MAP.get(b));
                  break;
               default:
                  out.writeByte(b);
         }
      }

      out.writeByte(FrameHelper.END_OF_FRAME);
   }
}
