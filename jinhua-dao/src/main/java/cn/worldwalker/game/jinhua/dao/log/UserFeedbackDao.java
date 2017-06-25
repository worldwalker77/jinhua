package cn.worldwalker.game.jinhua.dao.log;

import cn.worldwalker.game.jinhua.domain.model.UserFeedbackModel;

public interface UserFeedbackDao {
	public long insertFeedback(UserFeedbackModel model);
}
