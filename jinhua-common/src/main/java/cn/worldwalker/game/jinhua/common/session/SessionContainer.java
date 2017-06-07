package cn.worldwalker.game.jinhua.common.session;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.worldwalker.game.jinhua.common.utils.JsonUtil;
import cn.worldwalker.game.jinhua.domain.result.Result;

public class SessionContainer {
	
	private static final Log log = LogFactory.getLog(SessionContainer.class);
	
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
				channel.write(new TextWebSocketFrame(JsonUtil.toJson(result)));
			} catch (Exception e) {
				log.error("sendTextMsgByPlayerId error, playerId: " + playerId + ", result : " + JsonUtil.toJson(result), e);
				return false;
			}
			return true;
		}
		return false;
	}
	
	public static void sendTextMsgByPlayerIdSet(Set<Long> playerIdSet, Result result){
		for(Long playerId : playerIdSet){
			Channel channel = getChannel(playerId);
			if (null != channel) {
				try {
					channel.write(new TextWebSocketFrame(JsonUtil.toJson(result)));
				} catch (Exception e) {
					log.error("sendTextMsgByPlayerIdList error, playerId: " + playerId + ", result : " + JsonUtil.toJson(result), e);
				}
			}
		}
	}
	
	public static void sendTextMsg(ChannelHandlerContext ctx, Result result){
		try {
			ctx.channel().write(new TextWebSocketFrame(JsonUtil.toJson(result)));
		} catch (Exception e) {
			log.error("sendTextMsgByPlayerId error, result : " + JsonUtil.toJson(result), e);
		}
	}
	
	public static void removeSession(ChannelHandlerContext ctx){
		Collection<Channel> col =  sessionMap.values();
		col.remove(ctx.channel());
	}

	public static Map<Long, Channel> getSessionMap() {
		return sessionMap;
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
