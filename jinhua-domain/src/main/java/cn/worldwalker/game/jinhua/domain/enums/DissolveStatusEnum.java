package cn.worldwalker.game.jinhua.domain.enums;

public enum DissolveStatusEnum {
	
	agree(1, "同意解散房间"),
	disagree(2, "不同意解散房间");
	
	public int status;
	public String desc;
	
	private DissolveStatusEnum(int status, String desc){
		this.status = status;
		this.desc = desc;
	}
	
	public static DissolveStatusEnum getDissolveEnumByType(int status){
		for(DissolveStatusEnum dissolveEnum : DissolveStatusEnum.values()){
			if (status == dissolveEnum.status) {
				return dissolveEnum;
			}
		}
		return null;
	}
}
