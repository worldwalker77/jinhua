package cn.worldwalker.game.jinhua.domain.game;

public class RoomCardOperationFailInfo {
	private Long playerId;
	private Integer payType;
	private Integer totalGames;
	private Integer roomCardOperationType;
	
	public RoomCardOperationFailInfo(Long playerId, Integer payType, Integer totalGames, Integer roomCardOperationType){
		this.playerId = playerId;
		this.payType = payType;
		this.totalGames = totalGames;
		this.roomCardOperationType = roomCardOperationType;
	}
	public Long getPlayerId() {
		return playerId;
	}
	public void setPlayerId(Long playerId) {
		this.playerId = playerId;
	}
	public Integer getPayType() {
		return payType;
	}
	public void setPayType(Integer payType) {
		this.payType = payType;
	}
	public Integer getTotalGames() {
		return totalGames;
	}
	public void setTotalGames(Integer totalGames) {
		this.totalGames = totalGames;
	}
	public Integer getRoomCardOperationType() {
		return roomCardOperationType;
	}
	public void setRoomCardOperationType(Integer roomCardOperationType) {
		this.roomCardOperationType = roomCardOperationType;
	}
	
}
