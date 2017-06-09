package cn.worldwalker.game.jinhua.domain.game;

import java.util.List;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class PlayerInfo {
	/**玩家id*/
	private Long playerId;
	/**用户图像url*/
	private String headImgUrl;
	/**玩家昵称*/
	private String nickName;
	/**玩家顺序*/
	private Integer order;
	/**用户级别*/
	private Integer level;
	/**玩家状态 1 未准备 2 准备 3未看牌 4看牌 5 主动弃牌 6 被动弃牌 */
	private Integer status;
	/**牌型*/
	private Integer cardType;
	/**牌*/
	private List<Card> cardList;
	/**当前跟注分数*/
	private Integer curStakeScore;
	/**当前局总的跟注分数*/
	private Integer curTotalStakeScore;
	/**当前局得分*/
	private Integer curScore;
	/**一圈总得分*/
	private Integer totalScore;
	/**当前局玩家跟注次数*/
	private Integer stakeTimes;
	/**赢的局数*/
	private Integer winTimes;
	/**输的局数*/
	private Integer loseTimes;
	/**1 同意解散 2不同意解散*/
	private Integer dissolveStatus;
	
	public Long getPlayerId() {
		return playerId;
	}
	public void setPlayerId(Long playerId) {
		this.playerId = playerId;
	}
	public String getHeadImgUrl() {
		return headImgUrl;
	}
	public void setHeadImgUrl(String headImgUrl) {
		this.headImgUrl = headImgUrl;
	}
	public String getNickName() {
		return nickName;
	}
	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
	public Integer getOrder() {
		return order;
	}
	public void setOrder(Integer order) {
		this.order = order;
	}
	public Integer getLevel() {
		return level;
	}
	public void setLevel(Integer level) {
		this.level = level;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public Integer getCardType() {
		return cardType;
	}
	public void setCardType(Integer cardType) {
		this.cardType = cardType;
	}
	public List<Card> getCardList() {
		return cardList;
	}
	public void setCardList(List<Card> cardList) {
		this.cardList = cardList;
	}
	public Integer getCurStakeScore() {
		return curStakeScore;
	}
	public void setCurStakeScore(Integer curStakeScore) {
		this.curStakeScore = curStakeScore;
	}
	public Integer getCurTotalStakeScore() {
		return curTotalStakeScore;
	}
	public void setCurTotalStakeScore(Integer curTotalStakeScore) {
		this.curTotalStakeScore = curTotalStakeScore;
	}
	public Integer getCurScore() {
		return curScore;
	}
	public void setCurScore(Integer curScore) {
		this.curScore = curScore;
	}
	public Integer getStakeTimes() {
		return stakeTimes;
	}
	public void setStakeTimes(Integer stakeTimes) {
		this.stakeTimes = stakeTimes;
	}
	public Integer getTotalScore() {
		return totalScore;
	}
	public void setTotalScore(Integer totalScore) {
		this.totalScore = totalScore;
	}
	public Integer getWinTimes() {
		return winTimes;
	}
	public void setWinTimes(Integer winTimes) {
		this.winTimes = winTimes;
	}
	public Integer getLoseTimes() {
		return loseTimes;
	}
	public void setLoseTimes(Integer loseTimes) {
		this.loseTimes = loseTimes;
	}
	public Integer getDissolveStatus() {
		return dissolveStatus;
	}
	public void setDissolveStatus(Integer dissolveStatus) {
		this.dissolveStatus = dissolveStatus;
	}
	
}
