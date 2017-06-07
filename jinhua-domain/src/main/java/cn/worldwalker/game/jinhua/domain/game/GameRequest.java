package cn.worldwalker.game.jinhua.domain.game;

import java.io.Serializable;


public class GameRequest implements Serializable{
	
	private static final long serialVersionUID = 1167211738710670217L;
	/**消息类型1,2,3,4,5,6,7,8,9,10*/
	private Integer msgType;
	private String token;
	private Integer gameType;
	private Msg msg;
	public Integer getMsgType() {
		return msgType;
	}
	public void setMsgType(Integer msgType) {
		this.msgType = msgType;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public Integer getGameType() {
		return gameType;
	}
	public void setGameType(Integer gameType) {
		this.gameType = gameType;
	}
	public Msg getMsg() {
		return msg;
	}
	public void setMsg(Msg msg) {
		this.msg = msg;
	}
	
	
}
