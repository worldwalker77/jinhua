package cn.worldwalker.game.jinhua.domain.enums;

public enum GameTypeEnum {
	
	jinhua(1, "金花"),
	majiang(2, "麻将");
	
	public int gameType;
	public String desc;
	
	private GameTypeEnum(int gameType, String desc){
		this.gameType = gameType;
		this.desc = desc;
	}
	
	public static GameTypeEnum getGameTypeEnumByType(int gameType){
		for(GameTypeEnum gameTypeEnum : GameTypeEnum.values()){
			if (gameType == gameTypeEnum.gameType) {
				return gameTypeEnum;
			}
		}
		return null;
	}
	
}
