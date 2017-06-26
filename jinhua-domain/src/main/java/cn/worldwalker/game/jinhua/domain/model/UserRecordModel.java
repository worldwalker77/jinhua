package cn.worldwalker.game.jinhua.domain.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserRecordModel {
	
	private Long id;
	
	private Long playerId;
	
	private Long roomId;
	
	private Integer score;
	
	private String nickNames;
	
	private List<String> nickNameList = new ArrayList<String>();
	
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


	public Long getRoomId() {
		return roomId;
	}

	public void setRoomId(Long roomId) {
		this.roomId = roomId;
	}

	public Integer getScore() {
		return score;
	}

	public void setScore(Integer score) {
		this.score = score;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getNickNames() {
		return nickNames;
	}

	public void setNickNames(String nickNames) {
		this.nickNames = nickNames;
	}

	public List<String> getNickNameList() {
		return nickNameList;
	}

	public void setNickNameList(List<String> nickNameList) {
		this.nickNameList = nickNameList;
	}
	
}
