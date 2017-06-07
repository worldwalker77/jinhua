package cn.worldwalker.game.jinhua.service.game.impl;

import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.worldwalker.game.jinhua.common.cards.CardResource;
import cn.worldwalker.game.jinhua.common.cards.CardRule;
import cn.worldwalker.game.jinhua.common.constant.Constant;
import cn.worldwalker.game.jinhua.common.session.SessionContainer;
import cn.worldwalker.game.jinhua.common.utils.IPUtil;
import cn.worldwalker.game.jinhua.common.utils.JsonUtil;
import cn.worldwalker.game.jinhua.common.utils.redis.JedisTemplate;
import cn.worldwalker.game.jinhua.domain.enums.GameTypeEnum;
import cn.worldwalker.game.jinhua.domain.enums.MsgTypeEnum;
import cn.worldwalker.game.jinhua.domain.enums.PlayerStatusEnum;
import cn.worldwalker.game.jinhua.domain.game.Card;
import cn.worldwalker.game.jinhua.domain.game.GameRequest;
import cn.worldwalker.game.jinhua.domain.game.Msg;
import cn.worldwalker.game.jinhua.domain.game.PlayerInfo;
import cn.worldwalker.game.jinhua.domain.game.RoomInfo;
import cn.worldwalker.game.jinhua.domain.result.Result;
import cn.worldwalker.game.jinhua.service.game.GameService;

@Service
public class GameServiceImpl implements GameService {
	
	private final static Log log = LogFactory.getLog(GameServiceImpl.class);
	
	@Autowired
	private JedisTemplate jedisTemplate;
	
	
	@Override
	public Result login(String token, String deviceType) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("headImgUrl", "http://img.zcool.cn/community/01e282574bc0126ac72525ae39ce5f.jpg");
		long playerId = genPlayerId();
		data.put("nickName", "NickName_" + playerId);
		data.put("playerId", playerId);
		data.put("level", 1);
		data.put("token", "");
		data.put("serverIp", "119.23.57.236");
		data.put("port", "3389");
		result.setData(data);
		return result;
	}

	@Override
	public Result getIpByRoomId(String token, Long roomId) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		
		/**根据房间号从redis获取对应的服务器ip*/
		String roomInfo = jedisTemplate.hget(Constant.jinhuaRoomMap, String.valueOf(roomId));
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
		try {
			result = new Result();
			result.setMsgType(MsgTypeEnum.entryHall.msgType);
			Long playerId = request.getMsg().getPlayerId();
			/**将channel与playerId进行映射*/
			SessionContainer.addChannel(ctx, playerId);
			SessionContainer.sendTextMsgByPlayerId(playerId, result);
		} catch (Exception e) {
			log.error("entryHall error, request : " + JsonUtil.toJson(request), e);
		}
		return result;
	}
	
	@Override
	public Result createRoom(ChannelHandlerContext ctx, GameRequest request) {
		Result result = null;
		Msg msg = request.getMsg();
		/**TODO 房号如何生成，后续待优化*/
		Long roomId = genRoomId();
		int i = 0;
		while(i < 3){
			boolean exist = true;
			try {
				exist = jedisTemplate.hexists(Constant.jinhuaRoomMap, String.valueOf(roomId));
			} catch (Exception e) {
				log.error("校验房号在redis中是否存在异常！, request : " + JsonUtil.toJson(request), e);
				result = new Result(1, "系统异常", request.getMsgType());
				SessionContainer.sendTextMsgByPlayerId(msg.getPlayerId(), result);
				return result;
			}
			/**如果不存在则跳出循环，此房间号可以使用*/
			if (!exist) {
				break;
			}
			/**如果此房间号存在则重新生成*/
			roomId = genRoomId();
			i++;
			if (i >= 3) {
				log.error("三次生成房号都有重复......");
				result = new Result(1, "系统异常,请稍后再试！", request.getMsgType());
				SessionContainer.sendTextMsgByPlayerId(msg.getPlayerId(), result);
				return result;
			}
		}
		RoomInfo roomInfo = new RoomInfo();
		roomInfo.setRoomId(roomId);
		roomInfo.setRoomOwnerId(msg.getPlayerId());
		roomInfo.setPayType(msg.getPayType());
		roomInfo.setTotalGames(msg.getTotalGames());
		/**创建房间的时候设置当前为第0局*/
		roomInfo.setCurGame(0);
//		roomInfo.setStakeLimit(msg.getStakeLimit());
//		roomInfo.setStakeTimesLimit(msg.getStakeTimesLimit());
//		roomInfo.setServerIp(IPUtil.getLocalIp());
		roomInfo.setCreateTime(new Date());
		List<PlayerInfo> playerList = new ArrayList<PlayerInfo>();
		PlayerInfo playerInfo = new PlayerInfo();
		playerInfo.setPlayerId(msg.getPlayerId());
		playerInfo.setNickName("NickName_" + msg.getPlayerId());
		playerInfo.setHeadImgUrl("http://img.zcool.cn/community/01e282574bc0126ac72525ae39ce5f.jpg");
		playerInfo.setLevel(1);
		playerInfo.setOrder(1);
		playerInfo.setStatus(PlayerStatusEnum.notReady.status);
		playerList.add(playerInfo);
		roomInfo.setPlayerList(playerList);
		/**将当前房间的信息设置到redis中*/
		setRoomInfoToRedis(roomId, roomInfo);
		
		/**设置返回信息*/
		result = new Result();
		result.setMsgType(MsgTypeEnum.createRoom.msgType);
		result.setGameType(GameTypeEnum.jinhua.gameType);
		result.setData(roomInfo);
		SessionContainer.sendTextMsgByPlayerId(msg.getPlayerId(), result);
		
		return result;  
	}
	
	@Override
	public Result entryRoom(ChannelHandlerContext ctx, GameRequest request) {
		Result result = null;
		try {
			Msg msg = request.getMsg();
			Long roomId = msg.getRoomId();
			/**参数为空*/
			if (roomId == null) {
				result = new Result(1, "房号不能为空", request.getMsgType());
				SessionContainer.sendTextMsg(ctx, result);
				return result;
			}
			/**房间如果不存在*/
			if (!jedisTemplate.hexists(Constant.jinhuaRoomMap, String.valueOf(roomId))) {
				result = new Result(1, "房号不存在", request.getMsgType());
				SessionContainer.sendTextMsg(ctx, result);
				return result;
			}
			String roomInfoStr = jedisTemplate.hget(Constant.jinhuaRoomMap, String.valueOf(roomId));
			RoomInfo roomInfo = JsonUtil.toObject(roomInfoStr, RoomInfo.class);
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
			PlayerInfo playerInfo = new PlayerInfo();
			playerInfo.setPlayerId(msg.getPlayerId());
			playerInfo.setNickName("nickName_" + msg.getPlayerId());
			playerInfo.setHeadImgUrl("http://img.zcool.cn/community/01e282574bc0126ac72525ae39ce5f.jpg");
			playerInfo.setLevel(1);
			playerInfo.setOrder(playerList.size() + 1);
			playerInfo.setStatus(PlayerStatusEnum.notReady.status);
			playerList.add(playerInfo);
			/**将当前加入的玩家信息加入房间，并设置进缓存*/
			jedisTemplate.hset(Constant.jinhuaRoomMap, String.valueOf(roomId), JsonUtil.toJson(roomInfo));
			result = new Result();
			result.setMsgType(request.getMsgType());
			request.setGameType(1);
			result.setData(roomInfo);
			/**给此房间中的所有玩家发送消息*/
			SessionContainer.sendTextMsgByPlayerIdSet(playerIdSet, result);
		} catch (Exception e) {
			log.error("进入房间异常, request : " + JsonUtil.toJson(request), e);
			result = new Result(1, "系统异常", request.getMsgType());
			SessionContainer.sendTextMsg(ctx, result);
			return result;
		}
		return result;
	}
	

	@Override
	public Result ready(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
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
			}
			roomInfo.setCurPlayerId(roomInfo.getRoomBankerId());
			
			setRoomInfoToRedis(roomId, roomInfo);
			/**发牌返回信息*/
			result.setMsgType(MsgTypeEnum.dealCards.msgType);
			Map<String, Object> roomInfoMap = new HashMap<String, Object>();
			roomInfoMap.put("roomId", roomInfo.getRoomId());
			roomInfoMap.put("roomOwnerId", roomInfo.getRoomOwnerId());
			roomInfoMap.put("roomBankerId", roomInfo.getRoomBankerId());
			roomInfoMap.put("curPlayerId", roomInfo.getCurPlayerId());
			roomInfoMap.put("totalGames", roomInfo.getTotalGames());
			roomInfoMap.put("curGame", roomInfo.getCurGame());
			SessionContainer.sendTextMsgByPlayerIdSet(getPlayerIdSet(playerList), result);
			return result;
		}
		setRoomInfoToRedis(roomId, roomInfo);
		result.setGameType(1);
		result.setMsgType(request.getMsgType());
		data.put("playerId", msg.getPlayerId());
		SessionContainer.sendTextMsgByPlayerIdSet(getPlayerIdSet(playerList), result);
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
		RoomInfo roomInfo = getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		/**跟注次数到指定次数的玩家计数*/
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
			if (player.getStakeTimes().equals(roomInfo.getStakeTimesLimit())) {
				stakeTimesReachCount++;
			}
		}
		Long nextOperatePlayerId = getNextOperatePlayerId(playerList, msg.getPlayerId());
		roomInfo.setCurPlayerId(nextOperatePlayerId);
		/**如果玩家的跟注次数都已经到了指定跟注次数上限，则自动明牌*/
		if (stakeTimesReachCount == playerList.size()) {
			calScoresAndWinner(roomInfo);
			setRoomInfoToRedis(roomId, roomInfo);
			/**此处new一个新对象，是返回给客户端需要返回的数据，不需要返回的数据则隐藏掉*/
			RoomInfo newRoomInfo = new RoomInfo();
			newRoomInfo.setCurWinnerId(roomInfo.getCurWinnerId());
			for(PlayerInfo player : playerList){
				PlayerInfo newPlayer = new PlayerInfo();
				newPlayer.setPlayerId(player.getPlayerId());
				newPlayer.setCurScore(player.getCurScore());
				newPlayer.setStatus(player.getStatus());
				newPlayer.setCardType(player.getCardType());
				newPlayer.setCardList(player.getCardList());
				newRoomInfo.getPlayerList().add(newPlayer);
			}
			result.setMsgType(MsgTypeEnum.autoCardsCompare.msgType);
			result.setData(newRoomInfo);
			SessionContainer.sendTextMsgByPlayerIdSet(getPlayerIdSet(playerList), result);
			return result;
		}
		setRoomInfoToRedis(roomId, roomInfo);
		result.setMsgType(MsgTypeEnum.stake.msgType);
		data.put("playerId", msg.getPlayerId());
		data.put("stakeScore", msg.getCurStakeScore());
		data.put("stakeTimes", curPlayerStakeTimes);
		data.put("curPlayerId", nextOperatePlayerId);
		SessionContainer.sendTextMsgByPlayerIdSet(getPlayerIdSet(playerList), result);
		return result;
	}
	
	@Override
	public Result watchCards(ChannelHandlerContext ctx, GameRequest request) {
		Result result = new Result();
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		
		Msg msg = request.getMsg();
		Long roomId = msg.getRoomId();
		RoomInfo roomInfo = getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		List<Card> cardList = null;
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(msg.getPlayerId())) {
				player.setStatus(PlayerStatusEnum.watch.status);
				cardList = player.getCardList();
			}
		}
		setRoomInfoToRedis(roomId, roomInfo);
		data.put("playerId", msg.getPlayerId());
		SessionContainer.sendTextMsgByPlayerIdSet(getPlayerIdSetWithoutSelf(playerList, msg.getPlayerId()), result);
		data.put("cards", cardList);
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
		RoomInfo roomInfo = getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		PlayerInfo selfPlayer = null;
		PlayerInfo otherPlayer = null;
		int alivePlayerCount = 0;
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(msg.getPlayerId())) {
				selfPlayer = player;
				/**设置当前跟注分数*/
				player.setCurStakeScore(msg.getCurStakeScore());
				/**设置当前总跟注分数*/
				player.setCurTotalStakeScore((player.getCurTotalStakeScore()==null?0:player.getCurTotalStakeScore()) + msg.getCurStakeScore());
			}else if(player.getPlayerId().equals(msg.getOtherPlayerId())){
				otherPlayer = player;
			}
			if (player.getStatus().equals(PlayerStatusEnum.notWatch.status) || player.getStatus().equals(PlayerStatusEnum.watch.status)) {
				alivePlayerCount++;
			}
		}
		/**如果最后只剩下两家，则需要自动进行明牌，结束本局*/
		if (alivePlayerCount == 2) {
			calScoresAndWinner(roomInfo);
			setRoomInfoToRedis(roomId, roomInfo);
			/**此处new一个新对象，是返回给客户端需要返回的数据，不需要返回的数据则隐藏掉*/
			RoomInfo newRoomInfo = new RoomInfo();
			newRoomInfo.setCurWinnerId(roomInfo.getCurWinnerId());
			for(PlayerInfo player : playerList){
				PlayerInfo newPlayer = new PlayerInfo();
				newPlayer.setPlayerId(player.getPlayerId());
				newPlayer.setCurScore(player.getCurScore());
				newPlayer.setStatus(player.getStatus());
				newPlayer.setCardType(player.getCardType());
				newPlayer.setCardList(player.getCardList());
				newRoomInfo.getPlayerList().add(newPlayer);
			}
			result.setMsgType(MsgTypeEnum.autoCardsCompare.msgType);
			result.setData(newRoomInfo);
			SessionContainer.sendTextMsgByPlayerIdSet(getPlayerIdSet(playerList), result);
			return result;
		}
		
		/**如果活着的玩家大于2家，则只需要将此两个玩家比牌*/
		Long curPlayerId = null;
		int re = CardRule.compareTwoPlayerCards(selfPlayer, otherPlayer);
		if (re > 0) {
			otherPlayer.setStatus(PlayerStatusEnum.compareDisCard.status);
			data.put("winnerId", selfPlayer.getPlayerId());
			data.put("loserId", otherPlayer.getPlayerId());
			/**获取下一个操作者id需要在另外一个玩家设置弃牌之后*/
			curPlayerId = getNextOperatePlayerId(playerList, msg.getPlayerId());
			data.put("curPlayerId", curPlayerId);
		}else{
			data.put("winnerId", otherPlayer.getPlayerId());
			data.put("loserId", selfPlayer.getPlayerId());
			/**获取下一个操作者id需要在本玩家设置弃牌之前*/
			curPlayerId = getNextOperatePlayerId(playerList, msg.getPlayerId());
			data.put("curPlayerId", curPlayerId);
			selfPlayer.setStatus(PlayerStatusEnum.compareDisCard.status);
		}
		roomInfo.setCurPlayerId(curPlayerId);
		setRoomInfoToRedis(roomId, roomInfo);
		SessionContainer.sendTextMsgByPlayerIdSet(getPlayerIdSet(playerList), result);
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
		RoomInfo roomInfo = getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		Long nextOperatePlayerId = getNextOperatePlayerId(playerList, msg.getPlayerId());
		/**设置当前玩家状态为主动弃牌*/
		setCurPlayerStatus(playerList, msg.getPlayerId(), PlayerStatusEnum.autoDiscard);
		
		int alivePlayerCount = getAlivePlayerCount(playerList);
		/**如果剩余或者的玩家数为1*/
		if (alivePlayerCount == 1) {
			calScoresAndWinner(roomInfo);
			setRoomInfoToRedis(roomId, roomInfo);
			/**此处new一个新对象，是返回给客户端需要返回的数据，不需要返回的数据则隐藏掉*/
			RoomInfo newRoomInfo = new RoomInfo();
			newRoomInfo.setCurWinnerId(roomInfo.getCurWinnerId());
			for(PlayerInfo player : playerList){
				PlayerInfo newPlayer = new PlayerInfo();
				newPlayer.setPlayerId(player.getPlayerId());
				newPlayer.setCurScore(player.getCurScore());
				newPlayer.setStatus(player.getStatus());
				newPlayer.setCardType(player.getCardType());
				newPlayer.setCardList(player.getCardList());
				newRoomInfo.getPlayerList().add(newPlayer);
			}
			result.setMsgType(MsgTypeEnum.discardCards.msgType);
			result.setData(newRoomInfo);
			SessionContainer.sendTextMsgByPlayerIdSet(getPlayerIdSet(playerList), result);
			return result;
		}
		setRoomInfoToRedis(roomId, roomInfo);
		/**如果剩余或者的玩家数大于1,则给所有的玩家广播通知此玩家弃牌*/
		data.put("playerId", msg.getPlayerId());
		data.put("curPlayerId", nextOperatePlayerId);
		SessionContainer.sendTextMsgByPlayerIdSet(getPlayerIdSet(playerList), result);
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
		RoomInfo roomInfo = getRoomInfoFromRedis(roomId);
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		
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
		SessionContainer.sendTextMsgByPlayerIdSet(getPlayerIdSet(playerList), result);
		return result;
	}
	/**
	 * 设置玩家状态
	 * @param playerList
	 * @param playerId
	 * @param statusEnum
	 */
	private void setCurPlayerStatus(List<PlayerInfo> playerList, Long playerId, PlayerStatusEnum statusEnum){
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(playerId)) {
				player.setStatus(statusEnum.status);
			}
		}
	}
	
	private long genPlayerId(){
		int max=999999;
		int min=100000;
        Random random = new Random();
        int s = random.nextInt(max)%(max-min+1) + min;
		return s;
	}
	
	private long genRoomId(){
		int max=999999;
        int min=100000;
        Random random = new Random();
        int s = random.nextInt(max)%(max-min+1) + min;
		return s;
	}
	
	private Set<Long> getPlayerIdSet(List<PlayerInfo> playerList){
		Set<Long> set = new HashSet<Long>();
		for(PlayerInfo player : playerList){
			set.add(player.getPlayerId());
		}
		return set;
	}
	
	private Set<Long> getPlayerIdSetWithoutSelf(List<PlayerInfo> playerList, Long playerId){
		Set<Long> set = new HashSet<Long>();
		for(PlayerInfo player : playerList){
			if (!player.getPlayerId().equals(playerId)) {
				set.add(player.getPlayerId());
			}
		}
		return set;
	}
	
	private RoomInfo getRoomInfoFromRedis(Long roomId){
		String roomInfoStr = jedisTemplate.hget(Constant.jinhuaRoomMap, String.valueOf(roomId));
		return JsonUtil.toObject(roomInfoStr, RoomInfo.class);
	}
	
	private void setRoomInfoToRedis(Long roomId, RoomInfo roomInfo){
		jedisTemplate.hset(Constant.jinhuaRoomMap, String.valueOf(roomId),JsonUtil.toJson(roomInfo));
	}
	/**
	 * 计算各玩家得分及赢家
	 * @param roomInfo
	 */
	private void calScoresAndWinner(RoomInfo roomInfo){
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		/**在活着的玩家里面找出赢家*/
		PlayerInfo curWinnerPlayer = CardRule.comparePlayerCards(getAlivePlayerList(playerList));
		roomInfo.setCurWinnerId(curWinnerPlayer.getPlayerId());
		/**计算每个玩家当前局得分*/
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(curWinnerPlayer.getPlayerId())) {
				player.setCurScore((player.getCurScore()==null?0:player.getCurScore()) + (player.getCurTotalStakeScore() == null?0:player.getCurTotalStakeScore()));
			}else{
				player.setCurScore((player.getCurScore()==null?0:player.getCurScore()) - (player.getCurTotalStakeScore() == null?0:player.getCurTotalStakeScore()));
				curWinnerPlayer.setCurScore((curWinnerPlayer.getCurScore()==null?0:curWinnerPlayer.getCurScore()) + (player.getCurTotalStakeScore() == null?0:player.getCurTotalStakeScore()));
				
			}
		}
		/**计算每个玩家总得分*/
		for(PlayerInfo player : playerList){
			player.setTotalScore((player.getTotalScore()==null?0:player.getTotalScore()) + (player.getCurScore()==null?0:player.getCurScore()));
		}
		
		/**设置房间的总赢家*/
		Long totalWinnerId = playerList.get(0).getPlayerId();
		Integer maxTotalScore = playerList.get(0).getTotalScore()==null?0:playerList.get(0).getTotalScore();
		for(PlayerInfo player : playerList){
			Integer tempTotalScore = player.getTotalScore()==null?0:player.getTotalScore();
			if (tempTotalScore > maxTotalScore) {
				maxTotalScore = tempTotalScore;
				totalWinnerId = player.getPlayerId();
			}
		}
		roomInfo.setTotalWinnerId(totalWinnerId);
	}
	
	
	private Long getNextOperatePlayerId(List<PlayerInfo> playerList, Long curPlayerId){
		
		List<PlayerInfo> alivePlayerList = getAlivePlayerList(playerList);
		int size = alivePlayerList.size();
		Long nextOperatePlayerId = null;
		for(int i = 0; i < size; i++ ){
			PlayerInfo player = alivePlayerList.get(i);
			if (player.getPlayerId().equals(curPlayerId)) {
				if (i == size - 1) {
					nextOperatePlayerId = alivePlayerList.get(0).getPlayerId();
					break;
				}else{
					nextOperatePlayerId = alivePlayerList.get(i + 1).getPlayerId();
					break;
				}
			}
		}
		return nextOperatePlayerId;
	}
	
	private List<PlayerInfo> getAlivePlayerList(List<PlayerInfo> playerList){
		List<PlayerInfo> alivePlayerList = new ArrayList<PlayerInfo>();
		for(PlayerInfo player : playerList){
			if (player.getStatus().equals(PlayerStatusEnum.notWatch.status) || player.getStatus().equals(PlayerStatusEnum.watch.status)) {
				alivePlayerList.add(player);
			}
		}
		return alivePlayerList;
	}
	
	private int getAlivePlayerCount(List<PlayerInfo> playerList){
		int alivePlayerCount = 0;
		for(PlayerInfo player : playerList){
			if (player.getStatus().equals(PlayerStatusEnum.notWatch.status) || player.getStatus().equals(PlayerStatusEnum.watch.status)) {
				alivePlayerCount++;
			}
		}
		return alivePlayerCount;
	}
	
}
