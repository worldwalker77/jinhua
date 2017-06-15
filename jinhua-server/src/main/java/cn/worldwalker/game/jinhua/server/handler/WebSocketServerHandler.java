package cn.worldwalker.game.jinhua.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.worldwalker.game.jinhua.common.constant.Constant;
import cn.worldwalker.game.jinhua.common.session.SessionContainer;
import cn.worldwalker.game.jinhua.common.utils.IPUtil;
import cn.worldwalker.game.jinhua.common.utils.redis.JedisTemplate;
import cn.worldwalker.game.jinhua.domain.game.GameRequest;
import cn.worldwalker.game.jinhua.domain.result.ResultCode;
import cn.worldwalker.game.jinhua.server.dispatcher.TextMsgProcessDispatcher;
@Sharable
@Service
public class WebSocketServerHandler  extends SimpleChannelInboundHandler<Object>{
	/**
	       * 日志
	       */
	      private static final Logger logger = Logger.getLogger(WebSocketServerHandler.class);
	      /**
	       * 全局websocket
	       */
	      private WebSocketServerHandshaker handshaker;
	      
	      @Autowired
	      private TextMsgProcessDispatcher textMsgProcessDispatcher;
	      
	      @Autowired
	  	  private JedisTemplate jedisTemplate;
	      
	      @Override
	      public void channelActive(ChannelHandlerContext ctx) throws Exception {
	    	  
	    	  String ip = IPUtil.getLocalIp();
	    	  if (StringUtils.isNotBlank(ip)) {
    		     /**连接加入，将此ip对应的连接数+1，后续做负载均衡会用到*/
				 jedisTemplate.hincrBy(Constant.jinhuaIpConnectCountMap, ip, 1);
	    	  }
	    	  System.out.println("channelActive");
	    	  
	      }
	      
	      @Override
	      public void channelInactive(ChannelHandlerContext ctx) throws Exception {
	    	  System.out.println("channelInactive");
	    	  SessionContainer.removeSession(ctx);
	    	  String ip = IPUtil.getLocalIp();
	    	  if (StringUtils.isNotBlank(ip)) {
    		     /**连接断开，将此ip对应的连接数-1，后续做负载均衡会用到*/
				 jedisTemplate.hincrBy(Constant.jinhuaIpConnectCountMap, ip, -1);
	    	  }
	    	  ctx.close();
	      }
	      
	      @Override
	      protected void messageReceived(ChannelHandlerContext ctx, Object msg)
	              throws Exception {
	          //普通HTTP接入
	          if(msg instanceof FullHttpRequest){
	              handleHttpRequest(ctx, (FullHttpRequest) msg);
	          }else if(msg instanceof WebSocketFrame){ //websocket帧类型 已连接
	              //BinaryWebSocketFrame CloseWebSocketFrame ContinuationWebSocketFrame 
	              //PingWebSocketFrame PongWebSocketFrame TextWebScoketFrame
	              handleWebSocketFrame(ctx, (WebSocketFrame) msg);
	          }
	      }
	      
	      @Override
	      public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
	    	  System.out.println("channelReadComplete");
	          ctx.flush();
	      }
	      
	      private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request){
	          //如果http解码失败 则返回http异常 并且判断消息头有没有包含Upgrade字段(协议升级)
	          if(!request.decoderResult().isSuccess() 
	                  || (!"websocket".equals( request.headers().get("Upgrade")))    ){
	              sendHttpResponse(ctx, request, new DefaultFullHttpResponse(
	                      HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
	              return ;
	          }
	          //构造握手响应返回
	          WebSocketServerHandshakerFactory ws = new WebSocketServerHandshakerFactory("", null, false);
	          handshaker = ws.newHandshaker(request);
	          if(handshaker == null){
	              //版本不支持
	              WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
	          }else{
	              handshaker.handshake(ctx.channel(), request);
	          }
	      }
	      /**
	       * websocket帧
	       * @param ctx
	       * @param frame
	       */
	      private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame){
	          //判断是否关闭链路指令
	          if(frame instanceof CloseWebSocketFrame){
	        	  System.out.println("关闭==============");
	              handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
	              return ;
	          }
	          //判断是否Ping消息 -- ping/pong心跳包
	          if(frame instanceof PingWebSocketFrame){
	        	  System.out.println("心跳==============");
	              ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
	              return ;
	          }
	          //本程序仅支持文本消息， 不支持二进制消息
	          if(frame instanceof TextWebSocketFrame){
	        	  System.out.println("文本==============");
		          textMsgProcessDispatcher.textMsgProcess(ctx, ((TextWebSocketFrame) frame).text());
	          }
	          
	     }
	     
	     /**
	      * response
	      * @param ctx
	      * @param request
	      * @param response
	      */
	     private static void sendHttpResponse(ChannelHandlerContext ctx, 
	             FullHttpRequest request, FullHttpResponse response){
	         //返回给客户端
	         if(response.status().code() != HttpResponseStatus.OK.code()){
	             ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
	             response.content().writeBytes(buf);
	             buf.release();
	             HttpHeaderUtil.setContentLength(response, response.content().readableBytes());
	         }
	         //如果不是keepalive那么就关闭连接
	         ChannelFuture f = ctx.channel().writeAndFlush(response);
	         if(!HttpHeaderUtil.isKeepAlive(response) 
	                 || response.status().code() != HttpResponseStatus.OK.code()){
	             f.addListener(ChannelFutureListener.CLOSE);
	         }
	     }
	     
	     /**
	      * 异常 出错
	      */
	     @Override
	     public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	             throws Exception {
	         cause.printStackTrace();
	         SessionContainer.sendErrorMsg(ctx, ResultCode.SYSTEM_ERROR, 0, new GameRequest());
//	         ctx.close();
	     }
}
