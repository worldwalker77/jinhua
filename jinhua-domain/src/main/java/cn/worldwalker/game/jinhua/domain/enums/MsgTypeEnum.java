package cn.worldwalker.game.jinhua.domain.enums;

public enum MsgTypeEnum {
	
	entryHall(1, "进入大厅"),
	createRoom(2, "创建房间"),
	entryRoom(3, "进入房间"),
	ready(4, "点击准备"),
	dealCards(5, "发牌消息"),
	stake(6, "跟注"),
	watchCards(7, "看牌"),
	manualCardsCompare(8, "局中发起比牌"),
	discardCards(9, "弃牌"),
	curSettlement(10, "大结算"),
	totalSettlement(11, "大结算"),
	autoCardsCompare(12, "到达跟注数量上限的自动比牌"),
	dissolveRoom(13, "解散房间");
	
	public int msgType;
	public String desc;
	
	private MsgTypeEnum(int msgType, String desc){
		this.msgType = msgType;
		this.desc = desc;
	}
	
	public static MsgTypeEnum getMsgTypeEnumByType(int msgType){
		for(MsgTypeEnum msgTypeEnum : MsgTypeEnum.values()){
			if (msgType == msgTypeEnum.msgType) {
				return msgTypeEnum;
			}
		}
		return null;
	}
	
}
