package cn.worldwalker.game.jinhua.common.session;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.worldwalker.game.jinhua.common.constant.Constant;
import cn.worldwalker.game.jinhua.common.utils.JsonUtil;
import cn.worldwalker.game.jinhua.common.utils.redis.JedisTemplate;
import cn.worldwalker.game.jinhua.domain.enums.GameTypeEnum;
import cn.worldwalker.game.jinhua.domain.game.GameRequest;
import cn.worldwalker.game.jinhua.domain.game.UserInfo;
import cn.worldwalker.game.jinhua.domain.result.Result;

@Component
public class SessionContainer {
	
	private static final Log log = LogFactory.getLog(SessionContainer.class);
	
	private static JedisTemplate jedisTemplate;
	
	private static Map<Long, Channel> sessionMap = new ConcurrentHashMap<Long, Channel>();
	
	public static void addChannel(ChannelHandlerContext ctx, Long playerId){
		sessionMap.put(playerId, ctx.channel());
	}
	
	public static Channel getChannel(Long playerId){
		return sessionMap.get(playerId);
	}
	
	public static boolean sendTextMsgByPlayerId(Long playerId, Result result){
		Channel channel = getChannel(playerId);
		if (null != channel) {
			try {
				channel.writeAndFlush(new TextWebSocketFrame(JsonUtil.toJson(result)));
			} catch (Exception e) {
				log.error("sendTextMsgByPlayerId error, playerId: " + playerId + ", result : " + JsonUtil.toJson(result), e);
				return false;
			}
			return true;
		}
		return false;
	}
//	
//	public static void sendTextMsgByPlayerIdSet(Set<Long> playerIdSet, Result result){
//		for(Long playerId : playerIdSet){
//			Channel channel = getChannel(playerId);
//			if (null != channel) {
//				try {
//					channel.writeAndFlush(new TextWebSocketFrame(JsonUtil.toJson(result)));
//				} catch (Exception e) {
//					log.error("sendTextMsgByPlayerIdList error, playerId: " + playerId + ", result : " + JsonUtil.toJson(result), e);
//				}
//			}
//		}
//	}
	
	public static boolean sendTextMsgByPlayerId(Long roomId, Long playerId, Result result){
		long msgId = 0;
		Channel channel = getChannel(playerId);
		if (null != channel) {
			/**从redis通过自增获取当前房间的msgId，这里不做异常捕获，交给MsgProcessDispatcher层进行统一捕获，*/
			msgId = jedisTemplate.hincrBy(Constant.jinhuaRoomMsgIdMap, String.valueOf(roomId), 1);
			result.setMsgId(msgId);
			try {
				channel.writeAndFlush(new TextWebSocketFrame(JsonUtil.toJson(result)));
			} catch (Exception e) {
				log.error("sendTextMsgByPlayerId error, playerId: " + playerId + ", result : " + JsonUtil.toJson(result), e);
				return false;
			}
			return true;
		}
		return false;
	}
	
	public static long sendTextMsgByPlayerIdSet(Long roomId, Set<Long> playerIdSet, Result result){
		/**从redis通过自增获取当前房间的msgId，这里不做异常捕获，交给MsgProcessDispatcher层进行统一捕获，*/
		long msgId = jedisTemplate.hincrBy(Constant.jinhuaRoomMsgIdMap, String.valueOf(roomId), 1);
		result.setMsgId(msgId);
		for(Long playerId : playerIdSet){
			Channel channel = getChannel(playerId);
			if (null != channel) {
				try {
					channel.writeAndFlush(new TextWebSocketFrame(JsonUtil.toJson(result)));
				} catch (Exception e) {
					log.error("sendTextMsgByPlayerIdList error, playerId: " + playerId + ", result : " + JsonUtil.toJson(result), e);
				}
			}
		}
		return msgId;
	}
	
	public static void sendTextMsg(ChannelHandlerContext ctx, Result result){
		try {
			ctx.channel().writeAndFlush(new TextWebSocketFrame(JsonUtil.toJson(result)));
		} catch (Exception e) {
			log.error("sendTextMsgByPlayerId error, result : " + JsonUtil.toJson(result), e);
		}
	}
	
	 public static Result sendErrorMsg(ChannelHandlerContext ctx, String errorDesc, Integer msgType, GameRequest request){
		log.error(errorDesc + ", request:" + JsonUtil.toJson(request));
		Result result = new Result(1, errorDesc, msgType, GameTypeEnum.jinhua.gameType);
		try {
			sendTextMsg(ctx, result);
		} catch (Exception e) {
			log.error("sendErrorMsg error, result : " + JsonUtil.toJson(result), e);
		}
		return result;
	}
	
	public static void removeSession(ChannelHandlerContext ctx){
		if (sessionMap.isEmpty()) {
			return;
		}
		Long playerId = null;
		Set<Entry<Long, Channel>> entrySet = sessionMap.entrySet();
		for(Entry<Long, Channel> entry : entrySet){
			if (entry.getValue().equals(ctx.channel())) {
				playerId = entry.getKey();
				break;
			}
		}
		sessionMap.remove(playerId);
		System.out.println(playerId);
		
//		Collection<Channel> col =  sessionMap.values();
//		col.remove(ctx.channel());
	}
	
	public static void setUserInfoToRedis(String token, UserInfo userInfo){
		jedisTemplate.setex(token, JsonUtil.toJson(userInfo), 3600*2);
	}
	
	public static UserInfo getUserInfoFromRedis(String token){
		String temp = jedisTemplate.get(token);
		if (StringUtils.isNotBlank(temp)) {
			return JsonUtil.toObject(temp, UserInfo.class);
		}
		return null;
	}
	
	public static void expireUserInfo(String token){
		jedisTemplate.expire(token, 3600*2);
	}

	public static Map<Long, Channel> getSessionMap() {
		return sessionMap;
	}
	
	 @Autowired(required = true)
	public void setJedisTemplate(JedisTemplate jedisTemplate) {
		SessionContainer.jedisTemplate = jedisTemplate;
	}

	public static void main(String[] args) {
		Map<String, Long> map = new HashMap<String, Long>();
		map.put("1", 1L);
		map.put("2", 2L);
		map.put("3", 3L);
		Set<Entry<String, Long>> set = map.entrySet();
		for(Entry<String, Long> entry : set){
			if (entry.getValue().equals(1L)) {
				map.remove(entry.getKey());
			}
		}
		System.out.println(JsonUtil.toJson(map));
	}

}
