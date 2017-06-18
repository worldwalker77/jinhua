package cn.worldwalker.game.jinhua.dao.user;

import cn.worldwalker.game.jinhua.domain.model.UserModel;

public interface UserDao {
	
	 public UserModel getUserByWxOpenId(String wxOpenId);
	 
	 public Long insertUser(UserModel userModel);
	 
	 public int deductRoomCard();
	 
}
