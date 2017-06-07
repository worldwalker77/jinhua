package cn.worldwalker.game.jinhua.common.roomlocks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

/**
 * 每个房间都会分配一把锁，控制此房间请求排队
 * @author jinfeng.liu
 *
 */
public class RoomLockContainer {
	private static final Map<Long, Lock> lockMap = new ConcurrentHashMap<Long, Lock>();
	
	public static Lock getLockByRoomId(Long roomId){
		return lockMap.get(roomId);
	}
	
	public static void setLockByRoomId(Long roomId, Lock lock){
		lockMap.put(roomId, lock);
	}
}
