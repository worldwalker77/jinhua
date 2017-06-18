package cn.worldwalker.game.jinhua.domain.model;

import java.util.Date;

public class RoomCardLogModel {
	
	private Long id;
	
	private Long playerId;
	
	private String nickName;
	
	private Integer preRoomCardNum;
	
	private Integer curRoomCardNum;
	
	private Integer diffRoomCardNum;
	
	private Long operatorId;
	
	private String operatorName;
	
	private Integer operatorType;
	
	private Date createTime;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(Long playerId) {
		this.playerId = playerId;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public Integer getPreRoomCardNum() {
		return preRoomCardNum;
	}

	public void setPreRoomCardNum(Integer preRoomCardNum) {
		this.preRoomCardNum = preRoomCardNum;
	}

	public Integer getCurRoomCardNum() {
		return curRoomCardNum;
	}

	public void setCurRoomCardNum(Integer curRoomCardNum) {
		this.curRoomCardNum = curRoomCardNum;
	}

	public Integer getDiffRoomCardNum() {
		return diffRoomCardNum;
	}

	public void setDiffRoomCardNum(Integer diffRoomCardNum) {
		this.diffRoomCardNum = diffRoomCardNum;
	}

	public Long getOperatorId() {
		return operatorId;
	}

	public void setOperatorId(Long operatorId) {
		this.operatorId = operatorId;
	}

	public String getOperatorName() {
		return operatorName;
	}

	public void setOperatorName(String operatorName) {
		this.operatorName = operatorName;
	}

	public Integer getOperatorType() {
		return operatorType;
	}

	public void setOperatorType(Integer operatorType) {
		this.operatorType = operatorType;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	
}
