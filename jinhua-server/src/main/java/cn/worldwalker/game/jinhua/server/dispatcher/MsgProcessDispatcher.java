package cn.worldwalker.game.jinhua.server.dispatcher;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.worldwalker.game.jinhua.common.roomlocks.RoomLockContainer;
import cn.worldwalker.game.jinhua.common.session.SessionContainer;
import cn.worldwalker.game.jinhua.common.utils.JsonUtil;
import cn.worldwalker.game.jinhua.domain.enums.MsgTypeEnum;
import cn.worldwalker.game.jinhua.domain.game.GameRequest;
import cn.worldwalker.game.jinhua.domain.game.Msg;
import cn.worldwalker.game.jinhua.service.game.GameService;

@Service
public class MsgProcessDispatcher {
	
	private static final Log log = LogFactory.getLog(MsgProcessDispatcher.class);
	
	@Autowired
	private GameService gameService;
	
	public void requestDispatcher(ChannelHandlerContext ctx, GameRequest request){
		
		/**参数校验*/
		if (null == request || null == request.getMsg() || request.getMsgType() == null) {
			SessionContainer.sendErrorMsg(ctx, "参数不能为空", request.getMsgType(), request);
			return;
		}
		/**非进入大厅请求，则需要校验gameType*/
		if (!MsgTypeEnum.entryHall.equals(MsgTypeEnum.getMsgTypeEnumByType(request.getMsgType()))) {
			if (request.getGameType() == null) {
				SessionContainer.sendErrorMsg(ctx, "参数不能为空", request.getMsgType(), request);
				return;
			}
		}
		Msg msg = request.getMsg();
		Integer msgType = request.getMsgType();
		MsgTypeEnum msgTypeEnum= MsgTypeEnum.getMsgTypeEnumByType(msgType);
		Lock lock = null;
		try {
			/**除了进入大厅及创建房间之外，其他的请求需要按照房间号对请求进行排队，防止并发情况下数据状态错乱*/
			if (!MsgTypeEnum.entryHall.equals(msgTypeEnum) && !MsgTypeEnum.createRoom.equals(msgTypeEnum)) {
				lock = RoomLockContainer.getLockByRoomId(msg.getRoomId());
				if (lock == null) {
					SessionContainer.sendErrorMsg(ctx, "此房间已经解散或不存在", msgType, request);
					return;
				}
				lock.lock();
			}
			switch (msgTypeEnum) {
				case entryHall:
					if (msg.getPlayerId() == null) {
						SessionContainer.sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.entryHall(ctx, request);
					break;
				case createRoom:
					if (msg.getPlayerId() == null || msg.getPayType() == null || msg.getTotalGames() == null) {
						SessionContainer.sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.createRoom(ctx, request);
					break;
				case entryRoom:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.entryRoom(ctx, request);
					break;
				case ready:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.ready(ctx, request);
					break;
				case dealCards:
					break;
				case stake:
					if (msg.getPlayerId() == null || msg.getRoomId() == null || msg.getCurStakeScore() == null) {
						SessionContainer.sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.stake(ctx, request);
					break;
				case watchCards:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.watchCards(ctx, request);
					break;
				case manualCardsCompare:
					if (msg.getPlayerId() == null || msg.getOtherPlayerId() == null || msg.getCurStakeScore() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.manualCardsCompare(ctx, request);
					break;
				case discardCards:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.discardCards(ctx, request);
					break;
				case curSettlement:
					break;
				case totalSettlement:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.totalSettlement(ctx, request);
					break;
				case autoCardsCompare:
					break;
				case dissolveRoom:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.dissolveRoom(ctx, request);
					break;
				case agreeDissolveRoom:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.agreeDissolveRoom(ctx, request);
					break;
				case disagreeDissolveRoom:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, "参数不能为空", msgType, request);
						return;
					}
					gameService.disagreeDissolveRoom(ctx, request);
					break;
				case successDissolveRoom://服务端主动推送的消息
					break;
					
				default:
					SessionContainer.sendErrorMsg(ctx, "msgType消息类型参数错误", msgType, request);
					break;
				}
		} catch (Exception e) {
			log.error("requestDispatcher error, request:" + JsonUtil.toJson(request), e);
			SessionContainer.sendErrorMsg(ctx, "系统异常", msgType, request);
		} finally{
			if (lock != null) {
				lock.unlock();
			}
		}
		
	}
}
