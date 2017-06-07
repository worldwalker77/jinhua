package cn.worldwalker.game.jinhua.domain.enums;

public enum RoomStatusEnum {
	
	inGame(1, "小局中"),
	curGameOver(2, "小局结束"),
	totalGameOver(3, "一圈结束");
	
	public Integer status;
	public String desc;
	
	private RoomStatusEnum(Integer status, String desc){
		this.status = status;
		this.desc = desc;
	}
}
