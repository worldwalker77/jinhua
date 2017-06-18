package cn.worldwalker.game.jinhua.rpc.weixin;

import cn.worldwalker.game.jinhua.domain.weixin.WeiXinUserInfo;

public interface WeiXinRpc {
	public WeiXinUserInfo getWeiXinUserInfo(String code);
}
