package cn.worldwalker.game.jinhua.domain.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class RoomInfo implements Serializable{
	
	private static final long serialVersionUID = -7536150063810368838L;
	
	/**房间id*/
	private Long roomId;
	/**房主id*/
	private Long roomOwnerId;
	/**当前局赢家*/
	private Long curWinnerId;
	/**总赢家*/
	private Long totalWinnerId;
	/**当前可操作人id*/
	private Long curPlayerId;
	/**庄家id*/
	private Long roomBankerId;
	/**总局数*/
	private Integer totalGames;
	/**当前局数*/
	private Integer curGame;
	/**押注上限*/
	private Integer stakeLimit = 10;
	/**押注次数上限*/
	private Integer stakeTimesLimit = 6;
	/**支付方式 1：房主付费 2：AA付费*/
	private Integer payType;
	/**此房间所在服务器ip*/
	private String serverIp;
	/**房间的创建时间*/
	private Date createTime;
	/**当前房间状态*/
	private Integer status;
	
	private Long prePlayerId;
	
	private Integer preStatus;
	
	private List<PlayerInfo> playerList = new ArrayList<PlayerInfo>();
	
	public Long getRoomId() {
		return roomId;
	}
	public void setRoomId(Long roomId) {
		this.roomId = roomId;
	}
	public Long getRoomOwnerId() {
		return roomOwnerId;
	}
	public void setRoomOwnerId(Long roomOwnerId) {
		this.roomOwnerId = roomOwnerId;
	}
	public Long getCurWinnerId() {
		return curWinnerId;
	}
	public void setCurWinnerId(Long curWinnerId) {
		this.curWinnerId = curWinnerId;
	}
	public Long getTotalWinnerId() {
		return totalWinnerId;
	}
	public void setTotalWinnerId(Long totalWinnerId) {
		this.totalWinnerId = totalWinnerId;
	}
	public Long getCurPlayerId() {
		return curPlayerId;
	}
	public void setCurPlayerId(Long curPlayerId) {
		this.curPlayerId = curPlayerId;
	}
	public Long getRoomBankerId() {
		return roomBankerId;
	}
	public void setRoomBankerId(Long roomBankerId) {
		this.roomBankerId = roomBankerId;
	}
	public Integer getTotalGames() {
		return totalGames;
	}
	public void setTotalGames(Integer totalGames) {
		this.totalGames = totalGames;
	}
	public Integer getCurGame() {
		return curGame;
	}
	public void setCurGame(Integer curGame) {
		this.curGame = curGame;
	}
	public Integer getStakeLimit() {
		return stakeLimit;
	}
	public void setStakeLimit(Integer stakeLimit) {
		this.stakeLimit = stakeLimit;
	}
	public Integer getStakeTimesLimit() {
		return stakeTimesLimit;
	}
	public void setStakeTimesLimit(Integer stakeTimesLimit) {
		this.stakeTimesLimit = stakeTimesLimit;
	}
	public List<PlayerInfo> getPlayerList() {
		return playerList;
	}
	public void setPlayerList(List<PlayerInfo> playerList) {
		this.playerList = playerList;
	}
	public Integer getPayType() {
		return payType;
	}
	public void setPayType(Integer payType) {
		this.payType = payType;
	}
	public String getServerIp() {
		return serverIp;
	}
	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}
	public Date getCreateTime() {
		return createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	
}
