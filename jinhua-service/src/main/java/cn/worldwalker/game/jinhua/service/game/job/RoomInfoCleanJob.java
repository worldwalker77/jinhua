package cn.worldwalker.game.jinhua.service.game.job;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cn.worldwalker.game.jinhua.common.constant.Constant;
import cn.worldwalker.game.jinhua.common.player.GameCommonUtil;
import cn.worldwalker.game.jinhua.common.session.SessionContainer;
import cn.worldwalker.game.jinhua.common.utils.JsonUtil;
import cn.worldwalker.game.jinhua.domain.game.RoomInfo;


public class RoomInfoCleanJob extends SingleServerJobByRedis {

	@Override
	public void execute() {
		Map<String, String> map = jedisTemplate.hgetAll(Constant.jinhuaRoomIdRoomInfoMap);
		if (map != null) {
			Set<Entry<String, String>> entrySet = map.entrySet();
			for(Entry<String, String> entry : entrySet){
				String roomIdStr = entry.getKey();
				String roomInfoStr = entry.getValue();
				RoomInfo roomInfo = JsonUtil.toObject(roomInfoStr, RoomInfo.class);
				Date nowTime = new Date();
				Date updateTime = roomInfo.getUpdateTime();
				/**如果房间超过12小时没有更新，则清除房间信息*/
				if (nowTime.getTime() - updateTime.getTime() > 12*60*60*1000) {
					SessionContainer.cleanPlayerAndRoomInfo(Long.valueOf(roomIdStr), GameCommonUtil.getPlayerIds(roomInfo.getPlayerList()));
				}
			}
		}
	}

}
