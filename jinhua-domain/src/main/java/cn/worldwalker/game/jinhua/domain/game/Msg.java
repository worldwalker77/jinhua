package cn.worldwalker.game.jinhua.domain.game;

import java.util.List;

public class Msg {
	private Long roomId;
	private Long playerId;
	private Long otherPlayerId;//比牌的时候另外一方的id
	private Integer curStakeScore;//当前押注分数
	private Integer payType;//1:房主付费 2:AA付费
	private Integer totalGames;//10局，20局，30局
	private Integer stakeLimit;//押注封顶
	private Integer stakeTimesLimit;//押注次数封顶
	private Integer refreshType;//刷新类型 1，断线重连刷新  2 消息id不连续刷新
	private Integer chatType;//聊天消息类型 1 文本 2 表情 3语音 4图片
	private String chatMsg;//消息
	private String feedBack;
	private String mobilePhone;
	private Integer feedBackType;
	
	private Integer noticeType;
	private String noticeContent;
	private List<Long> noticePlayerList;
	public Long getRoomId() {
		return roomId;
	}
	public void setRoomId(Long roomId) {
		this.roomId = roomId;
	}
	public Long getPlayerId() {
		return playerId;
	}
	public void setPlayerId(Long playerId) {
		this.playerId = playerId;
	}
	public Long getOtherPlayerId() {
		return otherPlayerId;
	}
	public void setOtherPlayerId(Long otherPlayerId) {
		this.otherPlayerId = otherPlayerId;
	}
	public Integer getPayType() {
		return payType;
	}
	public void setPayType(Integer payType) {
		this.payType = payType;
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
	public Integer getTotalGames() {
		return totalGames;
	}
	public void setTotalGames(Integer totalGames) {
		this.totalGames = totalGames;
	}
	public Integer getCurStakeScore() {
		return curStakeScore;
	}
	public void setCurStakeScore(Integer curStakeScore) {
		this.curStakeScore = curStakeScore;
	}
	public Integer getRefreshType() {
		return refreshType;
	}
	public void setRefreshType(Integer refreshType) {
		this.refreshType = refreshType;
	}
	public Integer getChatType() {
		return chatType;
	}
	public void setChatType(Integer chatType) {
		this.chatType = chatType;
	}
	public String getChatMsg() {
		return chatMsg;
	}
	public void setChatMsg(String chatMsg) {
		this.chatMsg = chatMsg;
	}
	public String getMobilePhone() {
		return mobilePhone;
	}
	public void setMobilePhone(String mobilePhone) {
		this.mobilePhone = mobilePhone;
	}
	public Integer getFeedBackType() {
		return feedBackType;
	}
	public void setFeedBackType(Integer feedBackType) {
		this.feedBackType = feedBackType;
	}
	public String getFeedBack() {
		return feedBack;
	}
	public void setFeedBack(String feedBack) {
		this.feedBack = feedBack;
	}
	public Integer getNoticeType() {
		return noticeType;
	}
	public void setNoticeType(Integer noticeType) {
		this.noticeType = noticeType;
	}
	public String getNoticeContent() {
		return noticeContent;
	}
	public void setNoticeContent(String noticeContent) {
		this.noticeContent = noticeContent;
	}
	public List<Long> getNoticePlayerList() {
		return noticePlayerList;
	}
	public void setNoticePlayerList(List<Long> noticePlayerList) {
		this.noticePlayerList = noticePlayerList;
	}
	
}
