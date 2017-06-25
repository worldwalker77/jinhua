package cn.worldwalker.game.jinhua.domain.enums;

public enum RoomCardConsumeEnum {
	
	roomOwnerPayTenGames(1, 10, 4, "房主付费10局需4张房卡"),
	roomOwnerPayTwentyGames(1, 20, 7, "房主付费20局需7张房卡"),
	roomOwnerPayThirtyGames(1, 30, 10, "房主付费30局需10张房卡"),
	AAPayTenGames(2, 10, 1, "AA制付费10局每人需1张房卡"),
	AAPayTwentyGames(2, 20, 2, "AA制付费20局每人需2张房卡"),
	AAPayThirtyGames(2, 30, 3, "AA制付费30局每人需3张房卡"),
	winnerPayTenGames(3, 10, 4, "赢家付费10局需4张房卡"),
	winnerPayTwentyGames(3, 20, 7, "赢家付费20局需7张房卡"),
	winnerPayThirtyGames(3, 30, 10, "赢家付费30局需10张房卡"),;
	
	public Integer payType;
	public Integer totalGames;
	public Integer needRoomCardNum;
	public String desc;
	
	private RoomCardConsumeEnum(Integer payType, Integer totalGames, Integer needRoomCardNum, String desc){
		this.payType = payType;
		this.totalGames = totalGames;
		this.needRoomCardNum = needRoomCardNum;
		this.desc = desc;
	}
	
	public static RoomCardConsumeEnum getRoomCardConsumeEnum(Integer payType, Integer totalGames){
		for(RoomCardConsumeEnum consumeEnum : RoomCardConsumeEnum.values()){
			if (consumeEnum.payType.equals(payType) && consumeEnum.totalGames.equals(totalGames)) {
				return consumeEnum;
			}
		}
		return null;
	}
}
