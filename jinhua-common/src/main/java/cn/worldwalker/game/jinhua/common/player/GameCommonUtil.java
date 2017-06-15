package cn.worldwalker.game.jinhua.common.player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import cn.worldwalker.game.jinhua.common.cards.CardRule;
import cn.worldwalker.game.jinhua.domain.enums.DissolveStatusEnum;
import cn.worldwalker.game.jinhua.domain.enums.OnlineStatusEnum;
import cn.worldwalker.game.jinhua.domain.enums.PlayerStatusEnum;
import cn.worldwalker.game.jinhua.domain.enums.RoomStatusEnum;
import cn.worldwalker.game.jinhua.domain.game.PlayerInfo;
import cn.worldwalker.game.jinhua.domain.game.RoomInfo;

public class GameCommonUtil {
	/**
	 * 设置玩家状态
	 * @param playerList
	 * @param playerId
	 * @param statusEnum
	 */
	public static void setPlayerStatus(List<PlayerInfo> playerList, Long playerId, PlayerStatusEnum statusEnum){
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(playerId)) {
				player.setStatus(statusEnum.status);
			}
		}
	}
	
	/**
	 * 设置玩家解散状态
	 * @param playerList
	 * @param playerId
	 * @param statusEnum
	 */
	public static void setDissolveStatus(List<PlayerInfo> playerList, Long playerId, DissolveStatusEnum statusEnum){
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(playerId)) {
				player.setDissolveStatus(statusEnum.status);
			}
		}
	}
	
	/**
	 * 设置玩家解散状态
	 * @param playerList
	 * @param playerId
	 * @param statusEnum
	 */
	public static void setOnlineStatus(List<PlayerInfo> playerList, Long playerId, OnlineStatusEnum onlineStatusEnum){
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(playerId)) {
				player.setDissolveStatus(onlineStatusEnum.status);
			}
		}
	}
	
	/**
	 * 设置玩家状态
	 * @param playerList
	 * @param playerId
	 * @param statusEnum
	 */
	public static Integer getPlayerStatus(List<PlayerInfo> playerList, Long playerId){
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(playerId)) {
				return player.getStatus();
			}
		}
		return null;
	}
	
	public static long genPlayerId(){
		int max=999999;
		int min=100000;
        Random random = new Random();
        int s = random.nextInt(max)%(max-min+1) + min;
		return s;
	}
	
	public static long genRoomId(){
		int max=999999;
        int min=100000;
        Random random = new Random();
        int s = random.nextInt(max)%(max-min+1) + min;
		return s;
	}
	
	public static Set<Long> getPlayerIdSet(List<PlayerInfo> playerList){
		Set<Long> set = new HashSet<Long>();
		for(PlayerInfo player : playerList){
			set.add(player.getPlayerId());
		}
		return set;
	}
	
	public static String[] getPlayerIds(List<PlayerInfo> playerList){
		int size = playerList.size();
		String[] players = new String[size];
		for(int i = 0; i < size; i++){
			players[i] = String.valueOf(playerList.get(i).getPlayerId());
		}
		return players;
	}
	
	public static Set<Long> getPlayerIdSetWithoutSelf(List<PlayerInfo> playerList, Long playerId){
		Set<Long> set = new HashSet<Long>();
		for(PlayerInfo player : playerList){
			if (!player.getPlayerId().equals(playerId)) {
				set.add(player.getPlayerId());
			}
		}
		return set;
	}
	
	/**
	 * 计算各玩家得分及赢家
	 * @param roomInfo
	 */
	public static void calScoresAndWinner(RoomInfo roomInfo){
		List<PlayerInfo> playerList = roomInfo.getPlayerList();
		/**在活着的玩家里面找出赢家*/
		PlayerInfo curWinnerPlayer = CardRule.comparePlayerCards(getAlivePlayerList(playerList));
		roomInfo.setCurWinnerId(curWinnerPlayer.getPlayerId());
		/**设置下一小局的庄家*/
		roomInfo.setRoomBankerId(curWinnerPlayer.getPlayerId());
		/**计算每个玩家当前局得分*/
		for(PlayerInfo player : playerList){
			if (!player.getPlayerId().equals(curWinnerPlayer.getPlayerId())) {
				player.setCurScore(player.getCurScore() - player.getCurTotalStakeScore() - 1);
				curWinnerPlayer.setCurScore(curWinnerPlayer.getCurScore() + player.getCurTotalStakeScore() + 1);
				player.setLoseTimes(player.getLoseTimes() + 1);
				if (player.getCardType() > player.getMaxCardType()) {
					player.setMaxCardType(player.getCardType());
				}
			}else{
				curWinnerPlayer.setWinTimes(curWinnerPlayer.getWinTimes() + 1);
				if (curWinnerPlayer.getCardType() > curWinnerPlayer.getMaxCardType()) {
					curWinnerPlayer.setMaxCardType(curWinnerPlayer.getCardType());
				}
			}
		}
		/**计算每个玩家总得分*/
		for(PlayerInfo player : playerList){
			player.setTotalScore(player.getTotalScore() + player.getCurScore());
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
		/**如果当前局数小于总局数，则设置为当前局结束*/
		if (roomInfo.getCurGame() < roomInfo.getTotalGames()) {
			roomInfo.setStatus(RoomStatusEnum.curGameOver.status);
		}else{/**如果当前局数等于总局数，则设置为一圈结束*/
			roomInfo.setStatus(RoomStatusEnum.totalGameOver.status);
		}
		System.out.println("当前房间状态：" + roomInfo.getStatus());
	}
	
	public static Long getNextOperatePlayerId(List<PlayerInfo> playerList, Long curPlayerId){
		
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
	
	public static List<PlayerInfo> getAlivePlayerList(List<PlayerInfo> playerList){
		List<PlayerInfo> alivePlayerList = new ArrayList<PlayerInfo>();
		for(PlayerInfo player : playerList){
			if (player.getStatus().equals(PlayerStatusEnum.notWatch.status) || player.getStatus().equals(PlayerStatusEnum.watch.status)) {
				alivePlayerList.add(player);
			}
		}
		return alivePlayerList;
	}
	
	public static int getAlivePlayerCount(List<PlayerInfo> playerList){
		int alivePlayerCount = 0;
		for(PlayerInfo player : playerList){
			if (player.getStatus().equals(PlayerStatusEnum.notWatch.status) || player.getStatus().equals(PlayerStatusEnum.watch.status)) {
				alivePlayerCount++;
			}
		}
		return alivePlayerCount;
	}
	
	public static boolean isExistPlayerInRoom(Long playerId, List<PlayerInfo> playerList){
		for(PlayerInfo player : playerList){
			if (player.getPlayerId().equals(playerId)) {
				return true;
			}
		}
		return false;
	}
}
