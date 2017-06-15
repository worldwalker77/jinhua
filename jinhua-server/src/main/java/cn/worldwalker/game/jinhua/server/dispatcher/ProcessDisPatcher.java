package cn.worldwalker.game.jinhua.server.dispatcher;

import io.netty.channel.ChannelHandlerContext;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import cn.worldwalker.game.jinhua.common.session.SessionContainer;
import cn.worldwalker.game.jinhua.common.utils.JsonUtil;
import cn.worldwalker.game.jinhua.domain.enums.MsgTypeEnum;
import cn.worldwalker.game.jinhua.domain.game.GameRequest;
import cn.worldwalker.game.jinhua.domain.game.Msg;
import cn.worldwalker.game.jinhua.domain.game.UserInfo;
import cn.worldwalker.game.jinhua.domain.result.ResultCode;

public abstract class ProcessDisPatcher {
	
	private static final Logger logger = Logger.getLogger(ProcessDisPatcher.class);
	
	public void textMsgProcess(ChannelHandlerContext ctx, String textMsg){
		GameRequest request = null;
		request = JsonUtil.toObject(textMsg, GameRequest.class);
		if (null == request) {
			logger.error("参数json解析异常");
			SessionContainer.sendErrorMsg(ctx, ResultCode.SYSTEM_ERROR, 0, new GameRequest());
			return;
		}
		
		/**参数校验*/
		if (request.getMsgType() == null || StringUtils.isBlank(request.getToken())) {
			SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, request.getMsgType(), request);
			return;
		}
		
		/**
		 * token登录检验
		 */
		String token = request.getToken();
		UserInfo userInfo = SessionContainer.getUserInfoFromRedis(token);
		if (userInfo == null) {
			SessionContainer.sendErrorMsg(ctx, ResultCode.NEED_LOGIN, request.getMsgType(), request);
			return;
		}
		SessionContainer.expireUserInfo(token);
		
		/**非进入大厅请求，则需要校验gameType*/
		if (!MsgTypeEnum.entryHall.equals(MsgTypeEnum.getMsgTypeEnumByType(request.getMsgType()))) {
			if (request.getGameType() == null) {
				SessionContainer.sendErrorMsg(ctx, ResultCode.PARAM_ERROR, request.getMsgType(), request);
				return;
			}
		}
		Msg msg = request.getMsg();
		if (msg == null) {
			msg = new Msg();
			request.setMsg(msg);
		}
		msg.setPlayerId(userInfo.getPlayerId());
		if (msg.getRoomId() == null) {
			msg.setRoomId(userInfo.getRoomId());
		}
		requestDispatcher(ctx, request);
	}
	
	public abstract void requestDispatcher(ChannelHandlerContext ctx, GameRequest request);
}
