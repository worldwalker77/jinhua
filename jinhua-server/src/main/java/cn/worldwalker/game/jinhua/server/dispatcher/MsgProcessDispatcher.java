package cn.worldwalker.game.jinhua.server.dispatcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.worldwalker.game.jinhua.common.session.SessionContainer;
import cn.worldwalker.game.jinhua.common.utils.JsonUtil;
import cn.worldwalker.game.jinhua.domain.enums.GameTypeEnum;
import cn.worldwalker.game.jinhua.domain.enums.MsgTypeEnum;
import cn.worldwalker.game.jinhua.domain.game.GameRequest;
import cn.worldwalker.game.jinhua.domain.game.Msg;
import cn.worldwalker.game.jinhua.domain.result.Result;
import cn.worldwalker.game.jinhua.service.game.GameService;
import io.netty.channel.ChannelHandlerContext;

@Service
public class MsgProcessDispatcher {
	
	private static final Log log = LogFactory.getLog(MsgProcessDispatcher.class);
	
	@Autowired
	private GameService gameService;
	
	public void requestDispatcher(ChannelHandlerContext ctx, GameRequest request){
		
		/**参数校验*/
		if (null == request || null == request.getMsg() || request.getMsgType() == null || request.getGameType() == null) {
			sendErrorMsg(ctx, "参数不能为空!", request.getMsgType(), request);
			return;
		}
		Msg msg = request.getMsg();
		Integer msgType = request.getMsgType();
		MsgTypeEnum msgTypeEnum= MsgTypeEnum.getMsgTypeEnumByType(msgType);
		
		try {
			switch (msgTypeEnum) {
				case entryHall:
					if (msg.getPlayerId() == null) {
						sendErrorMsg(ctx, "msgType消息类型参数错误", msgType, request);
						return;
					}
					gameService.entryHall(ctx, request);
					break;
				case createRoom:
					if (msg.getPlayerId() == null || msg.getPayType() == null || msg.getTotalGames() == null) {
						sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.createRoom(ctx, request);
					break;
				case entryRoom:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.entryRoom(ctx, request);
					break;
				case ready:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.ready(ctx, request);
					break;
				case dealCards:
					break;
				case stake:
					if (msg.getPlayerId() == null || msg.getRoomId() == null || msg.getCurStakeScore() == null) {
						sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.stake(ctx, request);
					break;
				case watchCards:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.watchCards(ctx, request);
					break;
				case manualCardsCompare:
					if (msg.getPlayerId() == null || msg.getOtherPlayerId() == null || msg.getCurStakeScore() == null || msg.getRoomId() == null) {
						sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.manualCardsCompare(ctx, request);
					break;
				case discardCards:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.discardCards(ctx, request);
					break;
				case curSettlement:
					break;
				case totalSettlement:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.totalSettlement(ctx, request);
					break;
				case autoCardsCompare:
					break;

				default:
					sendErrorMsg(ctx, "msgType消息类型参数错误", msgType, request);
					break;
				}
		} catch (Exception e) {
			log.error("requestDispatcher error, request:" + JsonUtil.toJson(request), e);
			sendErrorMsg(ctx, "系统异常", msgType, request);
		}
		
	}
	
	private void sendErrorMsg(ChannelHandlerContext ctx, String errorDesc, Integer msgType, GameRequest request){
		log.error(errorDesc + ", request:" + JsonUtil.toJson(request));
		Result result = new Result(1, errorDesc, msgType, GameTypeEnum.jinhua.gameType);
		SessionContainer.sendTextMsg(ctx, result);
	}
}
