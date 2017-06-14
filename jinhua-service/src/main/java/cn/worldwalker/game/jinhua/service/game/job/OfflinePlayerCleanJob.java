package cn.worldwalker.game.jinhua.service.game.job;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cn.worldwalker.game.jinhua.common.constant.Constant;
import cn.worldwalker.game.jinhua.common.session.SessionContainer;


public class OfflinePlayerCleanJob extends SingleServerJobByRedis{
	 
	/**
	 * 清除游戏房间中离线超过20分钟的玩家信息及房间信息
	 */
	@Override
	public void execute() {
		System.out.println("===================定时任务执行");
		Map<String, String> map = jedisTemplate.hgetAll(Constant.jinhuaOfflinePlayerIdTimeMap);
		if (map != null) {
			Set<Entry<String, String>> entrySet = map.entrySet();
			for(Entry<String, String> entry : entrySet){
				String playerIdStr = entry.getKey();
				String value = entry.getValue();
				String[] arr = value.split("_");
				String roomIdStr = arr[0];
				Long offlineTime = Long.valueOf(arr[1]);
				Long diffTime = System.currentTimeMillis() - offlineTime;
				/**离线时间超过20分钟，则删除玩家及房间信息*/
				if (diffTime > 20*60*1000L) {
					String[] playerIds = new String[1];
					playerIds[0] = playerIdStr;
					SessionContainer.cleanPlayerAndRoomInfo(Long.valueOf(roomIdStr), playerIds);
				}
			}
		}
	}
	
}
