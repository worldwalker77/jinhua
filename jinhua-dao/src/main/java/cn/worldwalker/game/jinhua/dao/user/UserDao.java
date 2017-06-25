package cn.worldwalker.game.jinhua.dao.user;

import java.util.Map;

import cn.worldwalker.game.jinhua.domain.model.UserModel;

public interface UserDao {
	
	 public UserModel getUserByWxOpenId(String wxOpenId);
	 
	 public UserModel getUserById(Long id);
	 
	 public Long insertUser(UserModel userModel);
	 
	 public int deductRoomCard(Map<String, Object> map);
	 
}
