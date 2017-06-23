package cn.worldwalker.game.jinhua.service.game.impl;

import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.worldwalker.game.jinhua.common.cards.CardResource;
import cn.worldwalker.game.jinhua.common.cards.CardRule;
import cn.worldwalker.game.jinhua.common.constant.Constant;
import cn.worldwalker.game.jinhua.common.player.GameCommonUtil;
import cn.worldwalker.game.jinhua.common.roomlocks.RoomLockContainer;
import cn.worldwalker.game.jinhua.common.session.SessionContainer;
import cn.worldwalker.game.jinhua.common.utils.IPUtil;
import cn.worldwalker.game.jinhua.common.utils.JsonUtil;
import cn.worldwalker.game.jinhua.common.utils.MD5Util;
import cn.worldwalker.game.jinhua.common.utils.redis.JedisTemplate;
import cn.worldwalker.game.jinhua.dao.user.UserDao;
import cn.worldwalker.game.jinhua.domain.enums.CardTypeEnum;
import cn.worldwalker.game.jinhua.domain.enums.DissolveStatusEnum;
import cn.worldwalker.game.jinhua.domain.enums.GameTypeEnum;
import cn.worldwalker.game.jinhua.domain.enums.MsgTypeEnum;
import cn.worldwalker.game.jinhua.domain.enums.OnlineStatusEnum;
import cn.worldwalker.game.jinhua.domain.enums.PlayerStatusEnum;
import cn.worldwalker.game.jinhua.domain.enums.RoomStatusEnum;
import cn.worldwalker.game.jinhua.domain.game.Card;
import cn.worldwalker.game.jinhua.domain.game.GameRequest;
import cn.worldwalker.game.jinhua.domain.game.Msg;
import cn.worldwalker.game.jinhua.domain.game.PlayerInfo;
import cn.worldwalker.game.jinhua.domain.game.RoomInfo;
import cn.worldwalker.game.jinhua.domain.game.UserInfo;
import cn.worldwalker.game.jinhua.domain.model.UserModel;
import cn.worldwalker.game.jinhua.domain.result.Result;
import cn.worldwalker.game.jinhua.domain.result.ResultCode;
import cn.worldwalker.game.jinhua.domain.weixin.WeiXinUserInfo;
import cn.worldwalker.game.jinhua.rpc.weixin.WeiXinRpc;
import cn.worldwalker.game.jinhua.service.game.GameService;

@Service
public class GameServiceImpl implements GameService {
	
	private final static Log log = LogFactory.getLog(GameServiceImpl.class);
	
	@Autowired
	private WeiXinRpc weiXinRpc;
	@Autowired
	private JedisTemplate jedisTemplate;
	@Autowired
	private UserDao userDao;
	
	@Override
	public Result login(String code, String deviceType, HttpServletRequest request) {
		Result result = new Result();
		if (StringUtils.isBlank(code)) {
			result.setCode(ResultCode.PARAM_ERROR.code);
			result.setDesc(ResultCode.PARAM_ERROR.returnDesc);
			return result;
		}
		WeiXinUserInfo weixinUserInfo = weiXinRpc.getWeiXinUserInfo(code);
		if (null == weixinUserInfo) {
			result.setCode(ResultCode.SYSTEM_ERROR.code);
			result.setDesc(ResultCode.SYSTEM_ERROR.returnDesc);
			return result;
		}
		UserModel userModel = userDao.getUserByWxOpenId(weixinUserInfo.getOpneid());
		if (null == userModel) {
			userModel = new UserModel();
			userModel.setNickName(weixinUserInfo.getName());
			userModel.setHeadImgUrl(weixinUserInfo.getHeadImgUrl());
			userModel.setWxOpenId(weixinUserInfo.getOpneid());
			userDao.insertUser(userModel);
		}
		Long roomId = null;
		String temp = jedisTemplate.hget(Constant.jinhuaOfflinePlayerIdTimeMap, String.valueOf(userModel.getId()));
		if (StringUtils.isNotBlank(temp)) {
			roomId = Long.valueOf(temp.split("_")[0]);
		}
		UserInfo userInfo = new UserInfo();
		userInfo.setPlayerId(userModel.getId());
		userInfo.setRoomId(roomId);
		userInfo.setNickName(weixinUserInfo.getName());
		userInfo.setLevel(userModel.getUserLevel() == null ? 1 : userModel.getUserLevel());
		userInfo.setServerIp("119.23.57.236");
		userInfo.setPort("3389");
		userInfo.setRemoteIp(IPUtil.getRemoteIp(request));
		String loginToken = genToken(userModel.getId());
		SessionContainer.setUserInfoToRedis(loginToken, userInfo);
		userInfo.setHeadImgUrl(weixinUserInfo.getHeadImgUrl());
		userInfo.setToken(loginToken);
		result.setData(userInfo);
		return result;
	}
	
	@Override
	public Result login1(String code, String deviceType,HttpServletRequest request) {
		Result result = new Result();
		Long roomId = null;
		Long playerId = GameCommonUtil.genPlayerId();
		UserInfo userInfo = new UserInfo();
		userInfo.setPlayerId(playerId);
		userInfo.setRoomId(roomId);
		userInfo.setNickName("nickName_" + playerId);
		userInfo.setLevel(1);
		userInfo.setServerIp("119.23.57.236");
		userInfo.setPort("3389");
		userInfo.setRemoteIp(IPUtil.getRemoteIp(request));
		String loginToken = genToken(playerId);
		SessionContainer.setUserInfoToRedis(loginToken, userInfo);
		userInfo.setHeadImgUrl("http://wx.qlogo.cn/mmopen/wibbRT31wkCR4W9XNicL2h2pgaLepmrmEsXbWKbV0v9ugtdibibDgR1ybONiaWFtVeVtYWGWhObRiaiaicMgw8zat8Y5p6YzQbjdstE2/0");
		userInfo.setToken(loginToken);
		result.setData(userInfo);
		return result;
	}
	
	/**
	 * 登录后token生成，当前时间+
	 * @return
	 */
	private String genToken(Long playerId){
		String temp = playerId + System.currentTimeMillis() + Thread.currentThread().getName();
		return MD5Util.encryptByMD5(temp);
	}
	
	@Override
	public Result getIpByRoomId(String token, Long roomId) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		
		/**根据房间号从redis获取对应的服务器ip*/
		String roomInfo = jedisTemplate.hget(Constant.jinhuaRoomIdRoomInfoMap, String.valueOf(roomId));
		if (StringUtils.isBlank(roomInfo)) {
			result.setCode(1);
			result.setDesc("房号" + roomId + "不存在");
			return result;
		}
		RoomInfo room = JsonUtil.toObject(roomInfo, RoomInfo.class);
		data.put("roomId", roomId);
		data.put("serverIp", room.getServerIp());
		data.put("port", "3389");
		result.setData(data);
		return result;
	}
	
	@Override
	public Result entryHall(ChannelHandlerContext ctx, GameRequest request) {
		
		Result result = null;
		result = new Result();
		result.setMsgType(MsgTypeEnum.entryHall.msgType);
		Long playerId = request.getMsg().getPlayerId();
		/**将channel与playerId进行映射*/
		SessionContainer.addChannel(ctx, playerId);
		SessionContainer.sendTextMsgByPlayerId(playerId, result);
		return result;
	}
	
	@Override
	public Result createRoom(ChannelHandlerContext ctx, GameRequest request) {
		Result result = null;
		Msg msg = request.getMsg();
		Long roomId = GameCommonUtil.genRoomId();
		int i = 0;
		while(i < 3){
			boolean exist = true;
			try {
				exist = jedisTemplate.hexists(Constant.jinhuaRoomIdRoomInfoMap, String.valueOf(roomId));
			} catch (Exception e) {
				log.error("校验房号在redis中是否存在异常！, request : " + JsonUtil.toJson(request), e);
				return SessionContainer.sendErrorMsg(ctx, ResultCode.SYSTEM_ERROR, MsgTypeEnum.createRoom.msgType, request);
			}
			/**如果不存在则跳出循环，此房间号可以使用*/
			if (!exist) {
				break;
			}
			/**如果此房间号存在则重新生成*/
			roomId = GameCommonUtil.genRoomId();
			i++;
			if (i >= 3) {
				log.error("三次生成房号都有重复......");
				return SessionContainer.sendErrorMsg(ctx, ResultCode.SYSTEM_ERROR, MsgTypeEnum.createRoom.msgType, request);
			}
		}
		/**组装房间对象*/
		RoomInfo roomInfo = new RoomInfo();
		roomInfo.setRoomId(roomId);
		roomInfo.setRoomOwnerId(msg.getPlayerId());
		roomInfo.setRoomBankerId(msg.getPlayerId());
		roomInfo.setPayType(msg.getPayType());
		roomInfo.setTotalGames(msg.getTotalGames());
		roomInfo.setCurGame(0);
		roomInfo.setStakeButtom(Constant.stakeButtom);
		roomInfo.setStakeLimit(Constant.stakeLimit);
		roomInfo.setStakeTimesLimit(Constant.stakeTimesLimit);
		roomInfo.setStatus(RoomStatusEnum.justBegin.status);
		roomInfo.setServerIp(IPUtil.getLocalIp());
		Date date = new Date();
		roomInfo.setCreateTime(date);
		roomInfo.setUpdateTime(date);
		List<PlayerInfo> playerList = new ArrayList<PlayerInfo>();
		PlayerInfo playerInfo = new PlayerInfo();
		playerInfo.setPlayerId(msg.getPlayerId());
		playerInfo.setNickName("NickName_" + msg.getPlayerId());
		playerInfo.setHeadImgUrl("http://img.zcool.cn/community/01e282574bc0126ac72525ae39ce5f.jpg");
		playerInfo.setLevel(1);
		playerInfo.setOrder(1);
		playerInfo.setStatus(PlayerStatusEnum.notReady.status);
		playerInfo.setRoomCardNum(10);
		playerInfo.setWinTimes(0);
		playerInfo.setLoseTimes(0);
		playerInfo.setMaxCardType(CardTypeEnum.C235.cardType);
		playerList.add(playerInfo);
		roomInfo.setPlayerList(playerList);
		
		/**将roomId设置进用户信息中，后面会使用到*/
		UserInfo userInfo = SessionContainer.getUserInfoFromRedis(request.getToken());
		/**设置当前用户ip*/
		playerInfo.setIp(userInfo.getRemoteIp());
		userInfo.setRoomId(roomId);
		SessionContainer.setUserInfoToRedis(request.getToken(), userInfo);
		/**将当前房间的信息设置到redis中*/
		SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
		/**设置playerId与roomId的映射关系*/
		jedisTemplate.hset(Constant.jinhuaPlayerIdRoomIdMap, String.valueOf(msg.getPlayerId()), String.valueOf(roomId));
		
		/**设置返回信息*/
		result = new Result();
		result.setMsgType(MsgTypeEnum.createRoom.msgType);
		result.setGameType(GameTypeEnum.jinhua.gameType);
		result.setData(roomInfo);
		if (SessionContainer.sendTextMsgByPlayerId(roomId, msg.getPlayerId(), result)) {
			RoomLockContainer.setLockByRoomId(roomId, new ReentrantLock());
		}
		return result;  
	}
	
	@Override
	public Result entryRoom(ChannelHandlerContext ctx, GameRequest request) {
		Result result = null;
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		/**参数为空*/
		if (roomId == null) {
			result = new Result(1, "房号不能为空", request.getMsgType());
			SessionContainer.sendTextMsg(ctx, result);
			return result;
		}
		/**房间如果不存在*/
		if (!jedisTemplate.hexists(Constant.jinhuaRoomIdRoomInfoMap, String.valueOf(roomId))) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.ROOM_NOT_EXIST, MsgTypeEnum.entryRoom.msgType, request);
		}
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		/**如果不是刚开始游戏准备阶段，则不允许加入房间*/
		if (!RoomStatusEnum.justBegin.status.equals(roomInfo.getStatus())) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.NOT_ALLOW_ENTRY_ROOM, MsgTypeEnum.entryRoom.msgType, request);
		}
		/**需要发送消息的玩家id 列表*/
		Set<Long> playerIdSet = new HashSet<Long>();
		playerIdSet.add(msg.getPlayerId());
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		/**校验房间中是否已经有玩家信息，如果有的话可能是异常情况下产生的（缓存设置成功，但是客户端没有收到成功加入房间消息，导致客户端再次操作），需要去掉*/
		for(int i = 0; i < playerList.size(); i++){
			Long tempPalyerId = playerList.get(i).getPlayerId();
			if (tempPalyerId.equals(msg.getPlayerId())) {
				playerList.remove(i);
			}
			playerIdSet.add(tempPalyerId);
		}
		/**将roomId设置进用户信息中，后面会使用到*/
		UserInfo userInfo = SessionContainer.getUserInfoFromRedis(request.getToken());
		userInfo.setRoomId(roomId);
		SessionContainer.setUserInfoToRedis(request.getToken(), userInfo);
		
		PlayerInfo playerInfo = new PlayerInfo();
		playerInfo.setPlayerId(msg.getPlayerId());
		playerInfo.setNickName("nickName_" + msg.getPlayerId());
		playerInfo.setHeadImgUrl("http://img.zcool.cn/community/01e282574bc0126ac72525ae39ce5f.jpg");
		playerInfo.setLevel(1);
		playerInfo.setOrder(playerList.size() + 1);
		playerInfo.setStatus(PlayerStatusEnum.notReady.status);
		playerInfo.setRoomCardNum(10);
		playerInfo.setWinTimes(0);
		playerInfo.setLoseTimes(0);
		playerInfo.setMaxCardType(CardTypeEnum.C235.cardType);
		playerInfo.setIp(userInfo.getRemoteIp());
		playerList.add(playerInfo);
		roomInfo.setUpdateTime(new Date());
		/**将当前加入的玩家信息加入房间，并设置进缓存*/
		SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
		
		/**设置playerId与roomId的映射关系*/
		jedisTemplate.hset(Constant.jinhuaPlayerIdRoomIdMap, String.valueOf(msg.getPlayerId()), String.valueOf(roomId));
		result = new Result();
		result.setMsgType(request.getMsgType());
		request.setGameType(1);
		result.setData(roomInfo);
		/**给此房间中的所有玩家发送消息*/
		SessionContainer.sendTextMsgByPlayerIdSet(roomId, playerIdSet, result);
		return result;
	}
	

	@Override
	public Result ready(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		if (!GameCommonUtil.isExistPlayerInRoom(msg.getPlayerId(), playerList)) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_NOT_IN_ROOM, MsgTypeEnum.ready.msgType, request);
		}
		/**玩家已经准备计数*/
		int readyCount = 0;
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(msg.getPlayerId())) {
				/**设置状态为已准备*/
				player.setStatus(PlayerStatusEnum.ready.status);
			}
			if (PlayerStatusEnum.ready.status.equals(player.getStatus())) {
				readyCount++;
			}
		}
		
		int size = playerList.size();
		/**如果已经准备的人数据大于1并且等于房间内所有玩家的数目，则开始发牌*/
		if (readyCount > 1 && readyCount == size) {
			/**开始发牌时将房间内当前局数+1*/
			roomInfo.setCurGame(roomInfo.getCurGame() + 1);
			/**发牌*/
			List<List<Card>> playerCards = CardResource.dealCards(size);
			/**为每个玩家设置牌及牌型*/
			for(int i = 0; i < size; i++ ){
				PlayerInfo player = playerList.get(i);
				player.setCardList(playerCards.get(i));
				player.setCardType(CardRule.calculateCardType(playerCards.get(i)));
				player.setStatus(PlayerStatusEnum.notWatch.status);
				player.setStakeTimes(0);
				player.setCurTotalStakeScore(0);
				player.setCurScore(0);
				player.setTotalScore(0);
				player.setWinTimes(0);
				player.setLoseTimes(0);
				player.setCurStakeScore(0);
				/**设置每个玩家的解散房间状态为不同意解散，后面大结算返回大厅的时候回根据此状态判断是否解散房间*/
				player.setDissolveStatus(DissolveStatusEnum.disagree.status);
			}
			roomInfo.setCurPlayerId(roomInfo.getRoomBankerId());
			roomInfo.setStatus(RoomStatusEnum.inGame.status);
			roomInfo.setUpdateTime(new Date());
			SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
			/**发牌返回信息*/
			result.setMsgType(MsgTypeEnum.dealCards.msgType);
			Map<String, Object> roomInfoMap = new HashMap<String, Object>();
			roomInfoMap.put("roomId", roomInfo.getRoomId());
			roomInfoMap.put("roomOwnerId", roomInfo.getRoomOwnerId());
			roomInfoMap.put("roomBankerId", roomInfo.getRoomBankerId());
			/**庄家第一个说话*/
			roomInfoMap.put("curPlayerId", roomInfo.getRoomBankerId());
			roomInfoMap.put("totalGames", roomInfo.getTotalGames());
			roomInfoMap.put("curGame", roomInfo.getCurGame());
			result.setData(roomInfoMap);
			SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
			return result;
		}
		roomInfo.setUpdateTime(new Date());
		SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
		result.setGameType(1);
		result.setMsgType(request.getMsgType());
		data.put("playerId", msg.getPlayerId());
		SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
		return result;
	}
	
	@Override
	public Result dealCards(ChannelHandlerContext ctx, GameRequest request) {
		return null;
	}

	@Override
	public Result stake(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		result.setGameType(1);
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		if (!GameCommonUtil.isExistPlayerInRoom(msg.getPlayerId(), playerList)) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_NOT_IN_ROOM, MsgTypeEnum.stake.msgType, request);
		}
		/**如果跟注人不是当前说话人的id，则直接返回提示*/
		if (!msg.getPlayerId().equals(roomInfo.getCurPlayerId())) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.IS_NOT_YOUR_TURN, MsgTypeEnum.stake.msgType, request);
		}
		
		Integer prePlayerStatus = roomInfo.getPrePlayerStatus();
		Integer prePlayerStakeScore = roomInfo.getPrePlayerStakeScore();
		if (prePlayerStakeScore != null) {
			/**前一个玩家未看牌*/
			if (PlayerStatusEnum.notWatch.status.equals(prePlayerStatus)) {
				/**当前玩家未看牌*/
				if (PlayerStatusEnum.notWatch.status.equals(GameCommonUtil.getPlayerStatus(playerList, msg.getPlayerId()))) {
					/**如果当前玩家的跟注分数小于前一个玩家，则提示错误信息*/
					if (msg.getCurStakeScore() < prePlayerStakeScore) {
						return SessionContainer.sendErrorMsg(ctx, ResultCode.STAKE_SCORE_ERROR_1, MsgTypeEnum.stake.msgType, request);
					}
					/**当前玩家已看牌*/
				}else if (PlayerStatusEnum.watch.status.equals(GameCommonUtil.getPlayerStatus(playerList, msg.getPlayerId()))){
					/**当前玩家的跟注分数如果小于前一个玩家的跟注分数的2倍，则提示错误信息*/
					if (msg.getCurStakeScore() < prePlayerStakeScore*2) {
						return SessionContainer.sendErrorMsg(ctx, ResultCode.STAKE_SCORE_ERROR_2, MsgTypeEnum.stake.msgType, request);
					}
				}else{
					return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_STATUS_ERROR_1, MsgTypeEnum.stake.msgType, request);
				}
				/**前一个玩家已看牌*/
			}else{
				/**当前玩家未看牌*/
				if (PlayerStatusEnum.notWatch.status.equals(GameCommonUtil.getPlayerStatus(playerList, msg.getPlayerId()))) {
					/**如果当前玩家的跟注分数的2倍小于前一个玩家，则提示错误信息*/
					if (2*msg.getCurStakeScore() < prePlayerStakeScore) {
						return SessionContainer.sendErrorMsg(ctx, ResultCode.STAKE_SCORE_ERROR_3, MsgTypeEnum.stake.msgType, request);
					}
					/**当前玩家已看牌*/
				}else if (PlayerStatusEnum.watch.status.equals(GameCommonUtil.getPlayerStatus(playerList, msg.getPlayerId()))){
					/**当前玩家的跟注分数如果小于前一个玩家的跟注分数，则提示错误信息*/
					if (msg.getCurStakeScore() < prePlayerStakeScore) {
						return SessionContainer.sendErrorMsg(ctx, ResultCode.STAKE_SCORE_ERROR_1, MsgTypeEnum.stake.msgType, request);
					}
				}else{
					return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_STATUS_ERROR_1, MsgTypeEnum.stake.msgType, request);
				}
			}
		}
		/**跟注次数到指定次数上限的玩家计数*/
		int stakeTimesReachCount = 0;
		/**当前玩家的跟注次数*/
		int curPlayerStakeTimes = 0;
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(msg.getPlayerId())) {
				player.setStakeTimes(player.getStakeTimes() + 1);
				player.setCurStakeScore(msg.getCurStakeScore());
				player.setCurTotalStakeScore(player.getCurTotalStakeScore() + msg.getCurStakeScore());
				curPlayerStakeTimes = player.getStakeTimes();
			}
			if (PlayerStatusEnum.notWatch.status.equals(player.getStatus()) || PlayerStatusEnum.watch.status.equals(player.getStatus())) {
				if (player.getStakeTimes().equals(roomInfo.getStakeTimesLimit())) {
					stakeTimesReachCount++;
				}
			}
		}
		/**如果活着的玩家的跟注次数都已经到了指定跟注次数上限，则自动明牌，注意明牌的时候主动弃牌的不用明*/
		if (stakeTimesReachCount == GameCommonUtil.getAlivePlayerCount(playerList)) {
			GameCommonUtil.calScoresAndWinner(roomInfo);
			roomInfo.setUpdateTime(new Date());
			SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
			/**此处new一个新对象，是返回给客户端需要返回的数据，不需要返回的数据则隐藏掉*/
			RoomInfo newRoomInfo = new RoomInfo();
			newRoomInfo.setCurWinnerId(roomInfo.getCurWinnerId());
			newRoomInfo.setTotalWinnerId(roomInfo.getTotalWinnerId());
			newRoomInfo.setStatus(roomInfo.getStatus());
			newRoomInfo.setRoomId(roomId);
			newRoomInfo.setRoomOwnerId(roomInfo.getRoomOwnerId());
			for(PlayerInfo player : playerList){
				PlayerInfo newPlayer = new PlayerInfo();
				newPlayer.setPlayerId(player.getPlayerId());
				newPlayer.setCurScore(player.getCurScore());
				newPlayer.setTotalScore(player.getTotalScore());
				newPlayer.setStatus(player.getStatus());
				newPlayer.setMaxCardType(player.getMaxCardType());
				newPlayer.setWinTimes(player.getWinTimes());
				newPlayer.setLoseTimes(player.getLoseTimes());
				if (!PlayerStatusEnum.autoDiscard.status.equals(player.getStatus())) {
					newPlayer.setCardType(player.getCardType());
					newPlayer.setCardList(player.getCardList());
				}
				newRoomInfo.getPlayerList().add(newPlayer);
			}
			result.setMsgType(MsgTypeEnum.autoCardsCompare.msgType);
			result.setData(newRoomInfo);
			SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
			return result;
		}
		Long curPlayerId = GameCommonUtil.getNextOperatePlayerId(playerList, msg.getPlayerId());
		roomInfo.setCurPlayerId(curPlayerId);
		roomInfo.setPrePlayerId(msg.getPlayerId());
		roomInfo.setPrePlayerStatus(GameCommonUtil.getPlayerStatus(playerList, msg.getPlayerId()));
		roomInfo.setPrePlayerStakeScore(msg.getCurStakeScore());
		roomInfo.setUpdateTime(new Date());
		SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
		result.setMsgType(MsgTypeEnum.stake.msgType);
		data.put("playerId", msg.getPlayerId());
		data.put("stakeScore", msg.getCurStakeScore());
		data.put("stakeTimes", curPlayerStakeTimes);
		data.put("curPlayerId", GameCommonUtil.getNextOperatePlayerId(playerList, msg.getPlayerId()));
		SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
		return result;
	}
	
	@Override
	public Result watchCards(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		if (!GameCommonUtil.isExistPlayerInRoom(msg.getPlayerId(), playerList)) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_NOT_IN_ROOM, MsgTypeEnum.watchCards.msgType, request);
		}
		/**如果当前房间的状态不是在游戏中，则不处理此请求*/
		if (!RoomStatusEnum.inGame.status.equals(roomInfo.getStatus())) {
			log.error("当前房间的状态不是在游戏中,看牌请求无效！");
			return result;
		}
		
		List<Card> cardList = null;
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(msg.getPlayerId())) {
				player.setStatus(PlayerStatusEnum.watch.status);
				cardList = player.getCardList();
			}
		}
		roomInfo.setUpdateTime(new Date());
		SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
		result.setMsgType(MsgTypeEnum.watchCards.msgType);
		data.put("playerId", msg.getPlayerId());
		long msgId = SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSetWithoutSelf(playerList, msg.getPlayerId()), result);
		data.put("cardList", cardList);
		result.setMsgId(msgId);
		SessionContainer.sendTextMsgByPlayerId(msg.getPlayerId(), result);
		return result;
	}
	/**
	 * 局中玩家发起的比牌
	 */
	@Override
	public Result manualCardsCompare(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		if (!GameCommonUtil.isExistPlayerInRoom(msg.getPlayerId(), playerList)) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_NOT_IN_ROOM, MsgTypeEnum.manualCardsCompare.msgType, request);
		}
		/**如果跟注人不是当前说话人的id，则直接返回提示*/
		if (!msg.getPlayerId().equals(roomInfo.getCurPlayerId())) {
			SessionContainer.sendErrorMsg(ctx, ResultCode.IS_NOT_YOUR_TURN, MsgTypeEnum.manualCardsCompare.msgType, request);
			return result;
		}
		
		Integer curStakeScore = null;
		Integer prePlayerStatus = roomInfo.getPrePlayerStatus();
		Integer prePlayerStakeScore = roomInfo.getPrePlayerStakeScore();
		if (prePlayerStakeScore != null) {
			/**前一个玩家未看牌*/
			if (PlayerStatusEnum.notWatch.status.equals(prePlayerStatus)) {
				/**当前玩家未看牌*/
				if (PlayerStatusEnum.notWatch.status.equals(GameCommonUtil.getPlayerStatus(playerList, msg.getPlayerId()))) {
					/**当前玩家自动投注分数为前一个玩家的一半*/
					if (null != prePlayerStakeScore) {
						curStakeScore = prePlayerStakeScore;
					}
					/**当前玩家已看牌*/
				}else if (PlayerStatusEnum.watch.status.equals(GameCommonUtil.getPlayerStatus(playerList, msg.getPlayerId()))){
					/**当前玩家自动投注分数为前一个玩家的一半*/
					if (null != prePlayerStakeScore) {
						curStakeScore = 2*prePlayerStakeScore;
					}
				}else{
					return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_STATUS_ERROR_1, MsgTypeEnum.stake.msgType, request);
				}
				/**前一个玩家已看牌*/
			}else{
				/**当前玩家未看牌*/
				if (PlayerStatusEnum.notWatch.status.equals(GameCommonUtil.getPlayerStatus(playerList, msg.getPlayerId()))) {
					/**当前玩家自动投注分数为前一个玩家的一半*/
					if (null != prePlayerStakeScore) {
						curStakeScore = prePlayerStakeScore/2;
					}
					/**当前玩家已看牌*/
				}else if (PlayerStatusEnum.watch.status.equals(GameCommonUtil.getPlayerStatus(playerList, msg.getPlayerId()))){
					/**当前玩家自动投注分数为前一个玩家的一半*/
					if (null != prePlayerStakeScore) {
						curStakeScore = prePlayerStakeScore;
					}
				}else{
					return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_STATUS_ERROR_1, MsgTypeEnum.stake.msgType, request);
				}
			}
		}else{/**前一个玩家没有投注分，则说明此玩家是第一个说话的，直接设置1分*/
			curStakeScore = 1;
		}
		
		PlayerInfo selfPlayer = null;
		PlayerInfo otherPlayer = null;
		int alivePlayerCount = 0;
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(msg.getPlayerId())) {
				selfPlayer = player;
				/**设置当前跟注分数*/
				player.setCurStakeScore(curStakeScore);
				/**设置当前总跟注分数*/
				player.setCurTotalStakeScore((player.getCurTotalStakeScore()==null?0:player.getCurTotalStakeScore()) + curStakeScore);
			}else if(player.getPlayerId().equals(msg.getOtherPlayerId())){
				otherPlayer = player;
			}
			if (player.getStatus().equals(PlayerStatusEnum.notWatch.status) || player.getStatus().equals(PlayerStatusEnum.watch.status)) {
				alivePlayerCount++;
			}
		}
		/**如果最后只剩下两家，则需要自动进行明牌，结束本局*/
		if (alivePlayerCount == 2) {
			GameCommonUtil.calScoresAndWinner(roomInfo);
			roomInfo.setUpdateTime(new Date());
			SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
			/**此处new一个新对象，是返回给客户端需要返回的数据，不需要返回的数据则隐藏掉*/
			RoomInfo newRoomInfo = new RoomInfo();
			newRoomInfo.setCurWinnerId(roomInfo.getCurWinnerId());
			newRoomInfo.setTotalWinnerId(roomInfo.getTotalWinnerId());
			newRoomInfo.setStatus(roomInfo.getStatus());
			newRoomInfo.setRoomId(roomId);
			newRoomInfo.setRoomOwnerId(roomInfo.getRoomOwnerId());
			for(PlayerInfo player : playerList){
				PlayerInfo newPlayer = new PlayerInfo();
				newPlayer.setPlayerId(player.getPlayerId());
				newPlayer.setCurScore(player.getCurScore());
				newPlayer.setTotalScore(player.getTotalScore());
				newPlayer.setStatus(player.getStatus());
				newPlayer.setMaxCardType(player.getMaxCardType());
				newPlayer.setWinTimes(player.getWinTimes());
				newPlayer.setLoseTimes(player.getLoseTimes());
				if (!PlayerStatusEnum.autoDiscard.status.equals(player.getStatus())) {
					newPlayer.setCardType(player.getCardType());
					newPlayer.setCardList(player.getCardList());
				}
				newRoomInfo.getPlayerList().add(newPlayer);
			}
			result.setMsgType(MsgTypeEnum.autoCardsCompare.msgType);
			result.setData(newRoomInfo);
			SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
			return result;
		}
		
		/**如果活着的玩家大于2家，则只需要将此两个玩比牌*/
		Long curPlayerId = null;
		int re = CardRule.compareTwoPlayerCards(selfPlayer, otherPlayer);
		if (re > 0) {
			otherPlayer.setStatus(PlayerStatusEnum.compareDisCard.status);
			data.put("winnerId", selfPlayer.getPlayerId());
			data.put("loserId", otherPlayer.getPlayerId());
			/**获取下一个操作者id需要在另外一个玩家设置弃牌之后*/
			curPlayerId = GameCommonUtil.getNextOperatePlayerId(playerList, msg.getPlayerId());
			data.put("curPlayerId", curPlayerId);
		}else{
			data.put("winnerId", otherPlayer.getPlayerId());
			data.put("loserId", selfPlayer.getPlayerId());
			/**获取下一个操作者id需要在本玩家设置弃牌之前*/
			curPlayerId = GameCommonUtil.getNextOperatePlayerId(playerList, msg.getPlayerId());
			data.put("curPlayerId", curPlayerId);
			selfPlayer.setStatus(PlayerStatusEnum.compareDisCard.status);
		}
		roomInfo.setCurPlayerId(curPlayerId);
		roomInfo.setUpdateTime(new Date());
		SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
		result.setMsgType(MsgTypeEnum.manualCardsCompare.msgType);
		SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
		return result;
	}
	/**
	 * 玩家弃牌
	 * 如果其他的都弃牌，只剩下最后一个玩家，则进行自动明牌
	 */
	@Override
	public Result discardCards(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		/**如果当前房间的状态不是在游戏中，则不处理此请求*/
		if (!RoomStatusEnum.inGame.status.equals(roomInfo.getStatus())) {
			log.error("当前房间的状态不是在游戏中,弃牌请求无效！");
			return result;
		}
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		if (!GameCommonUtil.isExistPlayerInRoom(msg.getPlayerId(), playerList)) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_NOT_IN_ROOM, MsgTypeEnum.discardCards.msgType, request);
		}
		Long nextOperatePlayerId = GameCommonUtil.getNextOperatePlayerId(playerList, msg.getPlayerId());
		/**设置当前玩家状态为主动弃牌*/
		GameCommonUtil.setPlayerStatus(playerList, msg.getPlayerId(), PlayerStatusEnum.autoDiscard);
		
		int alivePlayerCount = GameCommonUtil.getAlivePlayerCount(playerList);
		/**如果剩余或者的玩家数为1，自动明牌*/
		if (alivePlayerCount == 1) {
			GameCommonUtil.calScoresAndWinner(roomInfo);
			roomInfo.setUpdateTime(new Date());
			SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
			/**此处new一个新对象，是返回给客户端需要返回的数据，不需要返回的数据则隐藏掉*/
			RoomInfo newRoomInfo = new RoomInfo();
			newRoomInfo.setCurWinnerId(roomInfo.getCurWinnerId());
			newRoomInfo.setRoomBankerId(roomInfo.getRoomBankerId());
			newRoomInfo.setTotalWinnerId(roomInfo.getTotalWinnerId());
			newRoomInfo.setStatus(roomInfo.getStatus());
			newRoomInfo.setRoomId(roomId);
			newRoomInfo.setRoomOwnerId(roomInfo.getRoomOwnerId());
			for(PlayerInfo player : playerList){
				PlayerInfo newPlayer = new PlayerInfo();
				newPlayer.setPlayerId(player.getPlayerId());
				newPlayer.setCurScore(player.getCurScore());
				newPlayer.setTotalScore(player.getTotalScore());
				newPlayer.setStatus(player.getStatus());
				newPlayer.setMaxCardType(player.getMaxCardType());
				newPlayer.setWinTimes(player.getWinTimes());
				newPlayer.setLoseTimes(player.getLoseTimes());
				if (!PlayerStatusEnum.autoDiscard.status.equals(player.getStatus())) {
					newPlayer.setCardType(player.getCardType());
					newPlayer.setCardList(player.getCardList());
				}
				newRoomInfo.getPlayerList().add(newPlayer);
			}
			result.setMsgType(MsgTypeEnum.autoCardsCompare.msgType);
			result.setData(newRoomInfo);
			SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
			return result;
		}
		roomInfo.setUpdateTime(new Date());
		SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
		/**如果剩余或者的玩家数大于1,则给所有的玩家广播通知此玩家弃牌*/
		data.put("playerId", msg.getPlayerId());
		data.put("curPlayerId", nextOperatePlayerId);
		SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
		return result;
		
	}
	/**
	 * 小结算
	 * 两种情况下会自动进行小结算 1，跟注达到上限  2，主动比牌的时候只剩下两个玩家  3，弃牌后只剩下一个玩家
	 */
	@Override
	public Result curSettlement(ChannelHandlerContext ctx, GameRequest request) {
		
		return null;
	}
	/**
	 * 大结算
	 */
	@Override
	public Result totalSettlement(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		if (!GameCommonUtil.isExistPlayerInRoom(msg.getPlayerId(), playerList)) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_NOT_IN_ROOM, MsgTypeEnum.totalSettlement.msgType, request);
		}
		/**此处new一个新对象，是返回给客户端需要返回的数据，不需要返回的数据则隐藏掉*/
		RoomInfo newRoomInfo = new RoomInfo();
		newRoomInfo.setTotalWinnerId(roomInfo.getTotalWinnerId());
		for(PlayerInfo player : playerList){
			PlayerInfo newPlayer = new PlayerInfo();
			newPlayer.setPlayerId(player.getPlayerId());
			newPlayer.setTotalScore(player.getTotalScore());
			newPlayer.setWinTimes(player.getWinTimes());
			newPlayer.setLoseTimes(player.getLoseTimes());
			newRoomInfo.getPlayerList().add(newPlayer);
		}
		result.setMsgType(MsgTypeEnum.totalSettlement.msgType);
		result.setData(newRoomInfo);
		SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
		return result;
	}
	
	@Override
	public Result dissolveRoom(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		if (!GameCommonUtil.isExistPlayerInRoom(msg.getPlayerId(), playerList)) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_NOT_IN_ROOM, MsgTypeEnum.dissolveRoom.msgType, request);
		}
		GameCommonUtil.setDissolveStatus(playerList, msg.getPlayerId(), DissolveStatusEnum.agree);
		roomInfo.setUpdateTime(new Date());
		SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
		if (playerList.size() == 1) {
			/**解散房间*/
			doDissolveRoom(roomId, GameCommonUtil.getPlayerIds(playerList));
			result.setMsgType(MsgTypeEnum.successDissolveRoom.msgType);
			data.put("roomId", roomId);
			SessionContainer.sendTextMsgByPlayerId(roomId, msg.getPlayerId(), result);
			return result;
		}
		result.setMsgType(MsgTypeEnum.dissolveRoom.msgType);
		data.put("roomId", roomId);
		data.put("playerId", msg.getPlayerId());
		SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
		return result;
	}

	@Override
	public Result agreeDissolveRoom(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		if (!GameCommonUtil.isExistPlayerInRoom(msg.getPlayerId(), playerList)) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_NOT_IN_ROOM, MsgTypeEnum.agreeDissolveRoom.msgType, request);
		}
		int agreeDissolveCount = 0;
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(msg.getPlayerId())) {
				player.setDissolveStatus(DissolveStatusEnum.agree.status);
			}
			if (player.getDissolveStatus().equals(DissolveStatusEnum.agree.status)) {
				agreeDissolveCount++;
			}
		}
		roomInfo.setUpdateTime(new Date());
		SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
		/**如果大部分人同意，则推送解散消息并解散房间*/
		if (agreeDissolveCount >= (playerList.size()/2 + 1)) {
			/**解散房间*/
			doDissolveRoom(roomId, GameCommonUtil.getPlayerIds(playerList));
			result.setMsgType(MsgTypeEnum.successDissolveRoom.msgType);
			SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
			return result;
		}
		result.setMsgType(MsgTypeEnum.agreeDissolveRoom.msgType);
		data.put("roomId", roomId);
		data.put("playerId", msg.getPlayerId());
		SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
		return result;
	}
	/**
	 * 解散1 删除redis房间信息 2 删除房间消息redis消息id计数器 3 删除playerId与roomId的映射关系  4 删除离线playerId与时间的映射关系  5 删除锁
	 */
	private void doDissolveRoom(Long roomId, String[] players){
		System.out.println("doDissolveRoom,解散房间，roomId：" + roomId + ",players:" + JsonUtil.toJson(players));
		jedisTemplate.hdel(Constant.jinhuaRoomIdRoomInfoMap, String.valueOf(roomId));
		jedisTemplate.hdel(Constant.jinhuaRoomIdMsgIdMap, String.valueOf(roomId));
		jedisTemplate.hdel(Constant.jinhuaPlayerIdRoomIdMap, players);
		jedisTemplate.hdel(Constant.jinhuaOfflinePlayerIdTimeMap, players);
		RoomLockContainer.delLockByRoomId(roomId);
	}

	@Override
	public Result disagreeDissolveRoom(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		if (null == roomInfo) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.ROOM_NOT_EXIST, MsgTypeEnum.dissolveRoom.msgType, request);
		}
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		if (!GameCommonUtil.isExistPlayerInRoom(msg.getPlayerId(), playerList)) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_NOT_IN_ROOM, MsgTypeEnum.dissolveRoom.msgType, request);
		}
		GameCommonUtil.setDissolveStatus(playerList, msg.getPlayerId(), DissolveStatusEnum.disagree);
		roomInfo.setUpdateTime(new Date());
		SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
		result.setMsgType(MsgTypeEnum.disagreeDissolveRoom.msgType);
		data.put("roomId", roomId);
		data.put("playerId", msg.getPlayerId());
		SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
		return result;
	}
	
	@Override
	public Result refreshRoom(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		if (null == roomInfo) {
			SessionContainer.sendTextMsgByPlayerId(msg.getPlayerId(), new Result(0, null, MsgTypeEnum.entryHall.msgType));
			return result;
		}
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		if (!GameCommonUtil.isExistPlayerInRoom(msg.getPlayerId(), playerList)) {
			SessionContainer.sendTextMsgByPlayerId(msg.getPlayerId(), new Result(0, null, MsgTypeEnum.entryHall.msgType));
			return result;
		}
		RoomStatusEnum roomStatusEnum = RoomStatusEnum.getRoomStatusEnum(roomInfo.getStatus());
		RoomInfo newRoomInfo = new RoomInfo();
		newRoomInfo.setStatus(roomStatusEnum.status);
		newRoomInfo.setRoomId(roomId);
		newRoomInfo.setRoomOwnerId(roomInfo.getRoomOwnerId());
		newRoomInfo.setRoomBankerId(roomInfo.getRoomBankerId());
		newRoomInfo.setTotalGames(roomInfo.getTotalGames());
		newRoomInfo.setCurGame(roomInfo.getCurGame());
		newRoomInfo.setPayType(roomInfo.getPayType());
		newRoomInfo.setStakeButtom(roomInfo.getStakeButtom());
		newRoomInfo.setStakeLimit(roomInfo.getStakeLimit());
		newRoomInfo.setStakeTimesLimit(roomInfo.getStakeTimesLimit());
		switch (roomStatusEnum) {
			case justBegin:
				/**此处new一个新对象，是返回给客户端需要返回的数据，不需要返回的数据则隐藏掉*/
				for(PlayerInfo player : playerList){
					PlayerInfo newPlayer = new PlayerInfo();
					newPlayer.setPlayerId(player.getPlayerId());
					newPlayer.setNickName(player.getNickName());
					newPlayer.setHeadImgUrl(player.getHeadImgUrl());
					newPlayer.setOrder(player.getOrder());
					newPlayer.setRoomCardNum(player.getRoomCardNum());
					newPlayer.setLevel(player.getLevel());
					newPlayer.setStatus(player.getStatus());
					newRoomInfo.getPlayerList().add(newPlayer);
				}
				result.setMsgType(MsgTypeEnum.refreshRoom.msgType);
				result.setData(newRoomInfo);
				break;
			case inGame:
				/**此处new一个新对象，是返回给客户端需要返回的数据，不需要返回的数据则隐藏掉*/
				newRoomInfo.setCurPlayerId(roomInfo.getCurPlayerId());
				for(PlayerInfo player : playerList){
					PlayerInfo newPlayer = new PlayerInfo();
					newPlayer.setPlayerId(player.getPlayerId());
					newPlayer.setNickName(player.getNickName());
					newPlayer.setHeadImgUrl(player.getHeadImgUrl());
					newPlayer.setOrder(player.getOrder());
					newPlayer.setRoomCardNum(player.getRoomCardNum());
					newPlayer.setLevel(player.getLevel());
					newPlayer.setStatus(player.getStatus());
					/**如果是已看牌，并且是当前请求刷新的用户，则要给此玩家返回牌型*/
					if (PlayerStatusEnum.watch.status.equals(player.getStatus()) && player.getPlayerId().equals(msg.getPlayerId())) {
						newPlayer.setCardList(player.getCardList());
					}
					
					newPlayer.setTotalScore(player.getTotalScore());
					newPlayer.setCurStakeScore(player.getCurStakeScore());
					newPlayer.setStakeTimes(player.getStakeTimes());
					
					newRoomInfo.getPlayerList().add(newPlayer);
				}
				result.setMsgType(MsgTypeEnum.refreshRoom.msgType);
				result.setData(newRoomInfo);
				break;
			case curGameOver:
				/**此处new一个新对象，是返回给客户端需要返回的数据，不需要返回的数据则隐藏掉*/
				newRoomInfo.setCurWinnerId(roomInfo.getCurWinnerId());
				for(PlayerInfo player : playerList){
					PlayerInfo newPlayer = new PlayerInfo();
					newPlayer.setPlayerId(player.getPlayerId());
					newPlayer.setNickName(player.getNickName());
					newPlayer.setHeadImgUrl(player.getHeadImgUrl());
					newPlayer.setOrder(player.getOrder());
					newPlayer.setRoomCardNum(player.getRoomCardNum());
					newPlayer.setLevel(player.getLevel());
					newPlayer.setTotalScore(player.getTotalScore());
					newPlayer.setCurScore(player.getCurScore());
					newPlayer.setStatus(player.getStatus());
					if (!PlayerStatusEnum.autoDiscard.status.equals(player.getStatus())) {
						newPlayer.setCardList(player.getCardList());
					}
					newRoomInfo.getPlayerList().add(newPlayer);
				}
				result.setMsgType(MsgTypeEnum.refreshRoom.msgType);
				result.setData(newRoomInfo);
				break;
			case totalGameOver:
				/**此处new一个新对象，是返回给客户端需要返回的数据，不需要返回的数据则隐藏掉*/
				newRoomInfo.setTotalWinnerId(roomInfo.getTotalWinnerId());
				newRoomInfo.setCurWinnerId(roomInfo.getCurWinnerId());
				for(PlayerInfo player : playerList){
					PlayerInfo newPlayer = new PlayerInfo();
					newPlayer.setPlayerId(player.getPlayerId());
					newPlayer.setNickName(player.getNickName());
					newPlayer.setHeadImgUrl(player.getHeadImgUrl());
					newPlayer.setOrder(player.getOrder());
					newPlayer.setRoomCardNum(player.getRoomCardNum());
					newPlayer.setLevel(player.getLevel());
					newPlayer.setStatus(player.getStatus());
					
					newPlayer.setTotalScore(player.getTotalScore());
					newPlayer.setCurScore(player.getCurScore());
					newPlayer.setMaxCardType(player.getMaxCardType());
					newPlayer.setWinTimes(player.getWinTimes());
					newPlayer.setLoseTimes(player.getLoseTimes());
					if (!PlayerStatusEnum.autoDiscard.status.equals(player.getStatus())) {
						newPlayer.setCardList(player.getCardList());
					}
					newRoomInfo.getPlayerList().add(newPlayer);
				}
				result.setMsgType(MsgTypeEnum.refreshRoom.msgType);
				result.setData(newRoomInfo);
				break;
	
			default:
				break;
		}
		/**1为断线后的刷新，所以需要设置在线状态，并通知其他玩家*/
		if (msg.getRefreshType() == 1) {
			GameCommonUtil.setOnlineStatus(playerList, msg.getPlayerId(), OnlineStatusEnum.online);
			/**删除此玩家的离线标记*/
			jedisTemplate.hdel(Constant.jinhuaOfflinePlayerIdTimeMap, String.valueOf(msg.getPlayerId()));
			long msgId = SessionContainer.sendTextMsgByPlayerIdSet(roomId, 
					GameCommonUtil.getPlayerIdSetWithoutSelf(playerList, msg.getPlayerId()), 
					new Result(0, null, MsgTypeEnum.onlineNotice.msgType));
			result.setMsgId(msgId);
		}
		SessionContainer.sendTextMsgByPlayerId(msg.getPlayerId(), result);
		return result;
	}
	
	@Override
	public Result delRoomConfirmBeforeReturnHall(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		if (null == roomInfo) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.ROOM_NOT_EXIST, MsgTypeEnum.delRoomConfirmBeforeReturnHall.msgType, request);
		}
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		if (!GameCommonUtil.isExistPlayerInRoom(msg.getPlayerId(), playerList)) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_NOT_IN_ROOM, MsgTypeEnum.delRoomConfirmBeforeReturnHall.msgType, request);
		}
		
		int agreeDissolveCount = 0;
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(msg.getPlayerId())) {
				player.setDissolveStatus(DissolveStatusEnum.agree.status);
			}
			if (player.getDissolveStatus().equals(DissolveStatusEnum.agree.status)) {
				agreeDissolveCount++;
			}
		}
		roomInfo.setUpdateTime(new Date());
		SessionContainer.setRoomInfoToRedis(roomId, roomInfo);
		/**如果所有人都有确认消息，则解散房间*/
		if (agreeDissolveCount >= playerList.size()) {
			/**解散房间*/
			doDissolveRoom(roomId, GameCommonUtil.getPlayerIds(playerList));
		}
		/**通知玩家返回大厅*/
		result.setMsgType(MsgTypeEnum.delRoomConfirmBeforeReturnHall.msgType);
		if (SessionContainer.sendTextMsgByPlayerId(msg.getPlayerId(), result)) {
			/**将roomId从用户信息中去除*/
			UserInfo userInfo = SessionContainer.getUserInfoFromRedis(request.getToken());
			userInfo.setRoomId(null);
			SessionContainer.setUserInfoToRedis(request.getToken(), userInfo);
		}
		return result;
	}
	@Override
	public Result queryOtherPlayerInfo(ChannelHandlerContext ctx,
			GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		if (null == roomInfo) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.ROOM_NOT_EXIST, MsgTypeEnum.queryPlayerInfo.msgType, request);
		}
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		UserInfo userInfo = SessionContainer.getUserInfoFromRedis(request.getToken());
		if (!GameCommonUtil.isExistPlayerInRoom(userInfo.getPlayerId(), playerList)) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_NOT_IN_ROOM, MsgTypeEnum.queryPlayerInfo.msgType, request);
		}
		PlayerInfo queryPlayer = null;
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(msg.getPlayerId())) {
				queryPlayer = player;
				break;
			}
		}
		PlayerInfo newPalyer = new PlayerInfo();
		newPalyer.setPlayerId(queryPlayer.getPlayerId());
		newPalyer.setNickName(queryPlayer.getNickName());
		newPalyer.setHeadImgUrl(queryPlayer.getHeadImgUrl());
		newPalyer.setIp(queryPlayer.getIp());
		result.setData(newPalyer);
		result.setMsgType(MsgTypeEnum.queryPlayerInfo.msgType);
		SessionContainer.sendTextMsgByPlayerId(userInfo.getPlayerId(), result);
		return result;
	}
	@Override
	public Result chatMsg(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = SessionContainer.getRoomInfoFromRedis(roomId);
		if (null == roomInfo) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.ROOM_NOT_EXIST, MsgTypeEnum.chatMsg.msgType, request);
		}
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		if (!GameCommonUtil.isExistPlayerInRoom(msg.getPlayerId(), playerList)) {
			return SessionContainer.sendErrorMsg(ctx, ResultCode.PLAYER_NOT_IN_ROOM, MsgTypeEnum.chatMsg.msgType, request);
		}
		result.setMsgType(MsgTypeEnum.chatMsg.msgType);
		data.put("playerId", msg.getPlayerId());
		data.put("chatMsg", msg.getChatMsg());
		data.put("chatType", msg.getChatType());
		SessionContainer.sendTextMsgByPlayerIdSet(roomId, GameCommonUtil.getPlayerIdSet(playerList), result);
		return result;
	}
	
}
