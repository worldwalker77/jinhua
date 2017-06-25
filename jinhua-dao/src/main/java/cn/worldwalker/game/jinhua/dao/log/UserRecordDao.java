package cn.worldwalker.game.jinhua.dao.log;

import java.util.List;

import cn.worldwalker.game.jinhua.domain.model.UserRecordModel;

public interface UserRecordDao {
	
	public long insertRecord(UserRecordModel model);
	
	public long batchInsertRecord(List<UserRecordModel> modelList);
	
	public List<UserRecordModel> getUserRecord(Long playerId);
	
}
