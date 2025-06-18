package protocol.dsc.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;

import protocol.dsc.session.SessionInfo;

import java.nio.ByteOrder;
import java.util.List;

public class FrameDecoder extends ByteToMessageDecoder {
   private static final int MAX_FRAME_LENGTH = 65535;
   private ByteBuf outBuf = null;
   private boolean inFrame = false;
   private boolean discarding = false;

   @SuppressWarnings("deprecation")
   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws DecoderException {
      try {
         byte b;
         // Scarta dati fino a nuovo inizio frame se in stato discarding
         while (this.discarding && in.isReadable()) {
               b = in.readByte();
               switch (b) {
                  case FrameHelper.START_OF_FRAME:
                     this.inFrame = true;
                     this.discarding = false;
                     break;
                  case FrameHelper.END_OF_FRAME:
                     this.inFrame = false;
                     this.discarding = false;
                     break;
               }
         }

         if (in.isReadable()) {
               assert !this.discarding;

               // Alloca buffer di output se necessario
               if (this.outBuf == null) {
                  this.outBuf = ctx.alloc().buffer().order(ByteOrder.BIG_ENDIAN);
                  assert this.outBuf.maxCapacity() > MAX_FRAME_LENGTH;
               }

               while (in.isReadable()) {
                  b = in.readByte();
                  switch (b) {
                     case FrameHelper.ESCAPE:
                           if (this.inFrame) {
                              if (!in.isReadable()) {
                                 in.readerIndex(in.readerIndex() - 1);
                                 return;
                              }
                              b = in.readByte();
                              Byte unescaped = FrameHelper.UNESCAPE_MAP.get(b);
                              if (unescaped == null) {
                                 in.readerIndex(in.readerIndex() - 1);
                                 this.discarding = true;
                                 throw new CorruptedFrameException(String.format("unexpected escape sequence: %02X %02X", FrameHelper.ESCAPE, b));
                              }
                              b = unescaped;
                           }
                           // no break, continua con default per scrivere il byte
                     default:
                           // Controlla lunghezza massima frame
                           if (this.outBuf.readableBytes() >= MAX_FRAME_LENGTH) {
                              this.discarding = true;
                              throw new TooLongFrameException(String.format("frame length exceeds %d", MAX_FRAME_LENGTH));
                           }
                           this.outBuf.writeByte(b);
                           break;
                     case FrameHelper.START_OF_FRAME:
                           try {
                              if (this.inFrame) {
                                 throw new CorruptedFrameException("unexpected start of frame");
                              }
                              this.setID(ctx);
                              assert this.outBuf == null;
                           } finally {
                              this.inFrame = true;
                           }
                           return;
                     case FrameHelper.END_OF_FRAME:
                           try {
                              if (!this.inFrame) {
                                 throw new CorruptedFrameException("unexpected end of frame");
                              }
                              out.add(this.outBuf);
                              this.outBuf = null;
                           } finally {
                              this.inFrame = false;
                           }
                           return;
                  }
               }
         }
      } catch (DecoderException ex) {
         this.releaseOutBuf();
         throw ex;
      }
   }

   // Gestisce l'ID multipunto all'inizio frame
   private void setID(ChannelHandlerContext ctx) throws CorruptedFrameException {
      assert this.outBuf != null;
      try {
         String commId = this.outBuf.toString(FrameHelper.MULTI_POINT_COMM_ID_CHARSET);
         SessionInfo sessionInfo = SessionInfo.getPeerInfo(ctx.channel());
         String currentId = sessionInfo.getMultiPointCommId();
         if (currentId == null) {
               sessionInfo.setMultiPointCommId(commId);
         } else if (!currentId.equals(commId)) {
               throw new CorruptedFrameException(String.format("unexpected multi-point communication id '%s' (it was '%s')", commId, currentId));
         }
      } finally {
         this.releaseOutBuf();
      }
   }

   protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws DecoderException {
      try {
         this.decode(ctx, in, out);
         if (this.inFrame) {
               throw new CorruptedFrameException("no end of frame");
         }
      } finally {
         this.reset();
      }
   }

   protected void handlerRemoved0(ChannelHandlerContext ctx) {
      this.reset();
   }

   private void releaseOutBuf() {
      if (this.outBuf != null) {
         this.outBuf.release();
         this.outBuf = null;
      }
   }

   private void reset() {
      this.inFrame = false;
      this.discarding = false;
      this.releaseOutBuf();
   }
}
