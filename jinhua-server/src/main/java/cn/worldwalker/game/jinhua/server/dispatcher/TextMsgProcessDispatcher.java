package cn.worldwalker.game.jinhua.server.dispatcher;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.worldwalker.game.jinhua.common.roomlocks.RoomLockContainer;
import cn.worldwalker.game.jinhua.common.utils.JsonUtil;
import cn.worldwalker.game.jinhua.domain.enums.MsgTypeEnum;
import cn.worldwalker.game.jinhua.domain.game.GameRequest;
import cn.worldwalker.game.jinhua.domain.game.Msg;
import cn.worldwalker.game.jinhua.domain.result.Result;
import cn.worldwalker.game.jinhua.domain.result.ResultCode;
import cn.worldwalker.game.jinhua.service.game.GameService;
import cn.worldwalker.game.jinhua.service.session.SessionContainer;

@Service
public class TextMsgProcessDispatcher extends ProcessDisPatcher{
	
	private static final Log log = LogFactory.getLog(TextMsgProcessDispatcher.class);
	
	@Autowired
	private GameService gameService;
	
	@Override
	public void requestDispatcher(ChannelHandlerContext ctx, GameRequest request){
		
		
		Msg msg = request.getMsg();
		Integer msgType = request.getMsgType();
		MsgTypeEnum msgTypeEnum= MsgTypeEnum.getMsgTypeEnumByType(msgType);
		Lock lock = null;
		try {
			/**除了进入大厅及创建房间之外，其他的请求需要按照房间号对请求进行排队，防止并发情况下数据状态错乱*/
			if (!MsgTypeEnum.entryHall.equals(msgTypeEnum) 
				&& !MsgTypeEnum.createRoom.equals(msgTypeEnum) 
				&& !MsgTypeEnum.heartBeat.equals(msgTypeEnum)
				&& !MsgTypeEnum.userFeedback.equals(msgTypeEnum)
				&& !MsgTypeEnum.userRecord.equals(msgTypeEnum)) {
				if (MsgTypeEnum.refreshRoom.equals(msgTypeEnum) && msg.getRoomId() != null) {
					lock = RoomLockContainer.getLockByRoomId(msg.getRoomId());
					if (lock == null) {
						/**如果是刷新房间，并且没有房间号，则可能是*/
						synchronized (TextMsgProcessDispatcher.class) {
							if (lock == null) {
								lock = new ReentrantLock();
								RoomLockContainer.setLockByRoomId(msg.getRoomId(), lock);
							}
						}
					}
					/**刷新接口并且roomId为空，则说明是在 大厅，直接返回进入大厅消息*/
				}else if(MsgTypeEnum.refreshRoom.equals(msgTypeEnum) && msg.getRoomId() == null){
					SessionContainer.addChannel(ctx, msg.getPlayerId());
					SessionContainer.sendTextMsgByPlayerId(msg.getPlayerId(), new Result(0, null, MsgTypeEnum.entryHall.msgType));
					return;
				}else{
					lock = RoomLockContainer.getLockByRoomId(msg.getRoomId());
					if (null == lock) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.ROOM_NOT_EXIST, msgType, request);
						return;
					}
				}
				
				lock.lock();
			}
			switch (msgTypeEnum) {
				case entryHall:
					if (msg.getPlayerId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.entryHall(ctx, request);
					break;
				case createRoom:
					if (msg.getPlayerId() == null || msg.getPayType() == null || msg.getTotalGames() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.createRoom(ctx, request);
					break;
				case entryRoom:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.entryRoom(ctx, request);
					break;
				case ready:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.ready(ctx, request);
					break;
				case dealCards:
					break;
				case stake:
					if (msg.getPlayerId() == null || msg.getRoomId() == null || msg.getCurStakeScore() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.stake(ctx, request);
					break;
				case watchCards:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.watchCards(ctx, request);
					break;
				case manualCardsCompare:
					if (msg.getPlayerId() == null || msg.getOtherPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.manualCardsCompare(ctx, request);
					break;
				case discardCards:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.discardCards(ctx, request);
					break;
				case curSettlement:
					break;
				case totalSettlement:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.totalSettlement(ctx, request);
					break;
				case autoCardsCompare:
					break;
				case dissolveRoom:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.dissolveRoom(ctx, request);
					break;
				case agreeDissolveRoom:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.agreeDissolveRoom(ctx, request);
					break;
				case disagreeDissolveRoom:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.disagreeDissolveRoom(ctx, request);
					break;
					
				case successDissolveRoom://服务端主动推送的消息
					break;
					
				case delRoomConfirmBeforeReturnHall:
					if (msg.getPlayerId() == null || msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.delRoomConfirmBeforeReturnHall(ctx, request);
					break;
					
				case refreshRoom:
					/**由于重连后，channel与playerId的映射关系会丢失，所以这里需要重新映射;如果单纯只是刷新房间信息，则不需要重新映射*/
					SessionContainer.addChannel(ctx, msg.getPlayerId());
					/**没有房间，则直接返回大厅*/
					if (msg.getRoomId() == null) {
						SessionContainer.sendTextMsgByPlayerId(msg.getPlayerId(), new Result(0, null, MsgTypeEnum.entryHall.msgType));
						return;
					}
					gameService.refreshRoom(ctx, request);
					break;
				case queryPlayerInfo:
					if (msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.refreshRoom(ctx, request);
					break;
				case chatMsg:
					if (msg.getRoomId() == null) {
						SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
						return;
					}
					gameService.chatMsg(ctx, request);
					break;
				case heartBeat:
					SessionContainer.sendTextMsg(ctx, new Result(0, null, MsgTypeEnum.heartBeat.msgType));
					break;
					
				case userRecord:
					gameService.userRecord(ctx, request);
					break;
					
				case userFeedback:
					gameService.userFeedback(ctx, request);
					break;
					
				case updatePlayerInfo:
					gameService.updatePlayerInfo(ctx, request);
					break;
					
				case sendEmoticon:
					gameService.sendEmoticon(ctx, request);
					break;
					
				default:
					SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, msgType, request);
					break;
				}
		} catch (Exception e) {
			log.error("requestDispatcher error, request:" + JsonUtil.toJson(request), e);
			SessionContainer.sendErrorMsg(ctx, ResultCode.SYSTEM_ERROR, msgType, request);
		} finally{
			if (lock != null) {
				lock.unlock();
			}
		}
		
	}
}
